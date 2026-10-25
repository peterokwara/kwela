import { readChatMessages } from "./chatRequestValidationService.ts";

type RunState = "QUEUED" | "RUNNING" | "COMPLETE" | "FAILED";

interface ChatRunRow {
    id: string;
    owner_id: string;
    idempotency_key: string;
    workflow_instance_id: string;
    attempt_count: number;
    status: RunState;
    input_key: string;
    reply_json: string | null;
    error_message: string | null;
    created_at: number;
    updated_at: number;
    input_expires_at: number | null;
    reply_expires_at: number | null;
    acknowledged_at: number | null;
    retry_key: string | null;
}

interface ChatRunDatabase {
    prepare(query: string): {
        bind(...values: unknown[]): {
            first<T>(): Promise<T | null>;
            all<T>(): Promise<{ results: T[] }>;
            run(): Promise<unknown>;
        };
    };
}

interface ChatRunInputBucket {
    put(key: string, value: string, options?: { httpMetadata?: { contentType?: string } }): Promise<unknown>;
    get(key: string): Promise<{ text(): Promise<string> } | null>;
    delete(key: string): Promise<void>;
}

interface ChatRunWorkflowBinding {
    create(options: { id: string; params: { runId: string } }): Promise<unknown>;
    get(id: string): Promise<{
        status(): Promise<{ status: string }>;
        terminate(): Promise<void>;
        delete(): Promise<void>;
    }>;
}

/** Starts, deduplicates, and retrieves a user's durable chat runs. */
export class D1ChatRunService {
    private readonly database: ChatRunDatabase;
    private readonly inputs: ChatRunInputBucket;
    private readonly workflow: ChatRunWorkflowBinding;
    private readonly now: () => number;

    constructor(database: ChatRunDatabase, inputs: ChatRunInputBucket, workflow: ChatRunWorkflowBinding, now: () => number = Date.now) {
        this.database = database;
        this.inputs = inputs;
        this.workflow = workflow;
        this.now = now;
    }

    async start(request: Request, ownerId: string, idempotencyKey: string): Promise<Response> {
        if (!/^[0-9a-f-]{36}$/i.test(idempotencyKey)) {
            return this.json({ message: "Kwela could not safely queue that message. Please try again." }, 400);
        }

        const retryKey = request.headers.get("retry-key");
        if (retryKey && !/^[0-9a-f-]{36}$/i.test(retryKey)) return this.json({ message: "The retry request is invalid." }, 400);
        const existing = await this.findByKey(ownerId, idempotencyKey);
        if (existing) return this.resumeOrRead(existing, retryKey);

        const parsed = await readChatMessages(request);
        if ("response" in parsed) return parsed.response;
        const runId = idempotencyKey.toLowerCase();
        // Each upload gets a private candidate key. A losing duplicate removes only its own object.
        const inputKey = `runs/${ownerId}/${runId}/${crypto.randomUUID()}.json`;
        const now = this.now();
        const storedInput = JSON.stringify({ messages: parsed.messages, conversationContext: parsed.conversationContext });
        let inserted = false;
        try {
            await this.inputs.put(inputKey, storedInput, { httpMetadata: { contentType: "application/json" } });
            const created = await this.database
                .prepare("INSERT OR IGNORE INTO chat_runs (id, owner_id, idempotency_key, workflow_instance_id, attempt_count, status, input_key, created_at, updated_at) VALUES (?, ?, ?, ?, 1, 'QUEUED', ?, ?, ?) RETURNING id")
                .bind(runId, ownerId, idempotencyKey.toLowerCase(), `${runId}-attempt-1`, inputKey, now, now)
                .first<{ id: string }>();
            inserted = created !== null;
            if (!inserted) await this.inputs.delete(inputKey);
            const row = await this.findByKey(ownerId, idempotencyKey);
            if (!row) return this.json({ message: "Kwela could not safely queue that message." }, 409);
            return inserted ? await this.startOrGetWorkflow(row) : await this.resumeOrRead(row, null);
        } catch (error) {
            if (!inserted) await this.inputs.delete(inputKey).catch(() => undefined);
            console.error("Kwela could not start a chat run:", error);
            return this.json({ message: "Kwela couldn’t start this task. Please try again." }, 503);
        }
    }

    async get(runId: string, ownerId: string): Promise<Response> {
        const row = await this.database
            .prepare("SELECT * FROM chat_runs WHERE id = ? AND owner_id = ?")
            .bind(runId.toLowerCase(), ownerId)
            .first<ChatRunRow>();
        if (!row) return this.json({ message: "That task could not be found." }, 404);
        if (row.status === "QUEUED") return this.startOrGetWorkflow(row);
        return this.response(row);
    }

    async acknowledge(runId: string, ownerId: string): Promise<Response> {
        await this.database
            .prepare("UPDATE chat_runs SET reply_json = NULL, reply_expires_at = NULL, acknowledged_at = ?, updated_at = ? WHERE id = ? AND owner_id = ? AND status = 'COMPLETE'")
            .bind(this.now(), this.now(), runId.toLowerCase(), ownerId)
            .run();
        return this.json({ ok: true });
    }

    async deleteOwner(ownerId: string): Promise<void> {
        const rows = await this.database
            .prepare("SELECT id, input_key, attempt_count FROM chat_runs WHERE owner_id = ?")
            .bind(ownerId)
            .all<{ id: string; input_key: string; attempt_count: number }>();
        for (const row of rows.results) {
            if (row.input_key) await this.inputs.delete(row.input_key);
            for (let attempt = 1; attempt <= row.attempt_count; attempt++) {
                try {
                    const instance = await this.workflow.get(`${row.id}-attempt-${attempt}`);
                    await instance.terminate().catch(() => undefined);
                    await instance.delete();
                } catch {
                    // The D1 row may have been saved before its Workflow instance was created.
                }
            }
        }
        await this.database.prepare("DELETE FROM chat_runs WHERE owner_id = ?").bind(ownerId).run();
    }

    async cleanupExpiredInputs(): Promise<void> {
        const now = this.now();
        const expired = await this.database
            .prepare("SELECT id, input_key FROM chat_runs WHERE input_expires_at IS NOT NULL AND input_expires_at <= ?")
            .bind(now)
            .all<{ id: string; input_key: string }>();
        for (const row of expired.results) {
            try {
                if (row.input_key) await this.inputs.delete(row.input_key);
            } catch (error) {
                console.error("Kwela could not delete expired task input:", error);
                continue;
            }
            await this.database
                .prepare("UPDATE chat_runs SET input_key = '', input_expires_at = NULL, updated_at = ? WHERE id = ? AND input_expires_at <= ?")
                .bind(now, row.id, now)
                .run();
        }

    }

    private async resumeOrRead(row: ChatRunRow, retryKey: string | null): Promise<Response> {
        if (row.status === "FAILED" && retryKey && retryKey !== row.retry_key) {
            if (!row.input_expires_at || row.input_expires_at <= this.now() || !row.input_key) {
                return this.response(row);
            }
            const nextAttempt = await this.database
                .prepare("UPDATE chat_runs SET status = 'QUEUED', error_message = NULL, input_expires_at = NULL, retry_key = ?, attempt_count = attempt_count + 1, workflow_instance_id = id || '-attempt-' || (attempt_count + 1), updated_at = ? WHERE id = ? AND owner_id = ? AND status = 'FAILED' AND input_expires_at > ? AND (retry_key IS NULL OR retry_key <> ?) RETURNING *")
                .bind(retryKey, this.now(), row.id, row.owner_id, this.now(), retryKey)
                .first<ChatRunRow>();
            if (nextAttempt) {
                try {
                    return await this.startOrGetWorkflow(nextAttempt);
                } catch (error) {
                    console.error("Kwela could not retry a failed chat run:", error);
                    return this.json({ message: "Kwela couldn’t restart this task. Please try again." }, 503);
                }
            }
        }
        if (row.status === "QUEUED") return this.startOrGetWorkflow(row);
        return this.response(row);
    }

    private async startOrGetWorkflow(row: ChatRunRow): Promise<Response> {
        try {
            await this.workflow.create({ id: row.workflow_instance_id, params: { runId: row.id } });
        } catch (error) {
            // A repeated submission can arrive after the workflow was created but before its HTTP response reached Android.
            try {
                await (await this.workflow.get(row.workflow_instance_id)).status();
            } catch {
                throw error;
            }
        }
        return this.json({ runId: row.id, status: "QUEUED" }, 202);
    }

    private async findByKey(ownerId: string, key: string): Promise<ChatRunRow | null> {
        return this.database
            .prepare("SELECT * FROM chat_runs WHERE owner_id = ? AND idempotency_key = ?")
            .bind(ownerId, key.toLowerCase())
            .first<ChatRunRow>();
    }

    private response(row: ChatRunRow): Response {
        const payload: Record<string, unknown> = { runId: row.id, status: row.status, acknowledged: row.acknowledged_at !== null };
        if (row.status === "COMPLETE" && row.reply_json) {
            try {
                payload.reply = JSON.parse(row.reply_json);
            } catch {
                payload.status = "FAILED";
                payload.error = "Kwela sent a reply the app couldn’t read. Please try again.";
            }
        }
        if (row.status === "COMPLETE" && !row.reply_json && row.acknowledged_at === null) {
            payload.status = "FAILED";
            payload.error = "Kwela could not recover this reply. Please send your message again.";
        }
        if (row.status === "FAILED") payload.error = row.error_message ?? "Kwela couldn’t finish that one. Tap Try again.";
        return this.json(payload, row.status === "QUEUED" || row.status === "RUNNING" ? 202 : 200);
    }

    private json(payload: unknown, status = 200): Response {
        return Response.json(payload, { status, headers: { "cache-control": "no-store" } });
    }
}

export type ChatRunWorkflowEnv = {
    DB: ChatRunDatabase;
    RUN_INPUTS: ChatRunInputBucket;
    CHAT_RUNS: ChatRunWorkflowBinding;
    OPENAI_API_KEY?: string;
    USAGE_CAP_USD?: string;
    GLOBAL_USAGE_CAP_USD?: string;
    MAX_REQUESTS_PER_MINUTE?: string;
    INPUT_PRICE_PER_MILLION_USD?: string;
    OUTPUT_PRICE_PER_MILLION_USD?: string;
};

