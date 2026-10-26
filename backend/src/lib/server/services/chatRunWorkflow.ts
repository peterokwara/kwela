import { NonRetryableError } from "cloudflare:workflows";
import { WorkflowEntrypoint, type WorkflowEvent, type WorkflowStep } from "cloudflare:workers";
import { D1UsageLimitService } from "./d1UsageLimitService.ts";
import { createChatPostHandler } from "./chatPostHandlerService.ts";
import { createOpenAICareerAssistantService } from "./openAICareerAssistantService.ts";
import { DEFAULT_MODEL_PRICES, DEFAULT_USAGE_LIMITS, positiveNumber } from "./usagePolicy.ts";
import type { ChatRunWorkflowEnv } from "./d1ChatRunService.ts";
import { analyticsFromEnv } from "./analyticsFactory.ts";
import { describeReplyForAnalytics } from "./analytics.ts";
import { ANALYTICS_EVENTS } from "./analyticsEvents.ts";

interface RunRow {
    owner_id: string;
    input_key: string;
}

/** Executes a chat request as a Cloudflare Workflow, independent of the Android request lifetime. */
export class ChatRunWorkflow extends WorkflowEntrypoint<ChatRunWorkflowEnv, { runId: string }> {
    override async run(event: WorkflowEvent<{ runId: string }>, step: WorkflowStep): Promise<void> {
        const { runId } = event.payload;
        const startedAt = Date.now();
        await step.do("mark-run-running", async () => {
            await this.env.DB.prepare("UPDATE chat_runs SET status = 'RUNNING', updated_at = ? WHERE id = ? AND status IN ('QUEUED', 'FAILED')")
                .bind(startedAt, runId)
                .run();
        });

        try {
            await step.do(
                "prepare-application",
                { retries: { limit: 1, delay: "10 seconds", backoff: "linear" }, timeout: "5 minutes" },
                async () => this.executeAndSave(runId)
            );
        } catch (error) {
            const message = error instanceof NonRetryableError && error.message
                ? error.message.slice(0, 300)
                : "Kwela couldn’t finish that one. Tap Try again.";
            await step.do("save-failed-run", async () => {
                await this.env.DB.prepare("UPDATE chat_runs SET status = 'FAILED', error_message = ?, input_expires_at = ?, updated_at = ? WHERE id = ?")
                    .bind(message, Date.now() + 24 * 60 * 60 * 1000, Date.now(), runId)
                    .run();
                await this.track(runId, ANALYTICS_EVENTS.runFail, { failure_kind: error instanceof NonRetryableError ? "rejected" : "error" });
            });
        }
    }

    /** Records a product event for the run's owner. It carries timing and counts only, never the person's words, and can never fail the run. */
    private async track(runId: string, event: string, properties: Record<string, unknown>): Promise<void> {
        try {
            const row = await this.env.DB.prepare("SELECT owner_id, created_at, attempt_count FROM chat_runs WHERE id = ?")
                .bind(runId)
                .first<{ owner_id: string; created_at: number; attempt_count: number }>();
            if (!row) return;
            await analyticsFromEnv(this.env).capture(row.owner_id, event, { ...properties, duration_ms: Date.now() - row.created_at, attempt_count: row.attempt_count });
        } catch (cause) {
            console.warn("Kwela could not record an analytics event:", cause);
        }
    }

    private async executeAndSave(runId: string): Promise<boolean> {
        const current = await this.env.DB.prepare("SELECT status FROM chat_runs WHERE id = ?").bind(runId).first<{ status: string }>();
        if (current?.status === "COMPLETE") return true;
        const replyJson = await this.prepareReply(runId);
        await this.env.DB.prepare("UPDATE chat_runs SET status = 'COMPLETE', reply_json = ?, reply_expires_at = NULL, acknowledged_at = NULL, error_message = NULL, input_expires_at = NULL, updated_at = ? WHERE id = ?")
            .bind(replyJson, Date.now(), runId)
            .run();
        const summary = describeReplyForAnalytics(replyJson);
        if (summary) await this.track(runId, ANALYTICS_EVENTS.applicationGenerate, summary);
        const row = await this.env.DB.prepare("SELECT input_key FROM chat_runs WHERE id = ?").bind(runId).first<{ input_key: string }>();
        if (row?.input_key) {
            try {
                await this.env.RUN_INPUTS.delete(row.input_key);
                await this.env.DB.prepare("UPDATE chat_runs SET input_key = '' WHERE id = ?").bind(runId).run();
            } catch (error) {
                console.error("Kwela could not delete completed task input:", error);
                await this.env.DB.prepare("UPDATE chat_runs SET input_expires_at = ? WHERE id = ?")
                    .bind(Date.now(), runId)
                    .run();
            }
        }
        return true;
    }

    private async prepareReply(runId: string): Promise<string> {
        if (!this.env.OPENAI_API_KEY) throw new NonRetryableError("Kwela’s assistant is not set up yet.");
        const row = await this.env.DB.prepare("SELECT owner_id, input_key FROM chat_runs WHERE id = ?").bind(runId).first<RunRow>();
        if (!row?.input_key) throw new NonRetryableError("The saved task input has expired. Send your message again.");
        const object = await this.env.RUN_INPUTS.get(row.input_key);
        if (!object) throw new NonRetryableError("The saved task input has expired. Send your message again.");
        const requestBody = await object.text();
        const limits = {
            capMicros: positiveNumber(this.env.USAGE_CAP_USD, DEFAULT_USAGE_LIMITS.capMicros / 1_000_000) * 1_000_000,
            globalCapMicros: positiveNumber(this.env.GLOBAL_USAGE_CAP_USD, DEFAULT_USAGE_LIMITS.globalCapMicros / 1_000_000) * 1_000_000,
            maxRequestsPerMinute: positiveNumber(this.env.MAX_REQUESTS_PER_MINUTE, DEFAULT_USAGE_LIMITS.maxRequestsPerMinute)
        };
        const prices = {
            inputPerMillionUsd: positiveNumber(this.env.INPUT_PRICE_PER_MILLION_USD, DEFAULT_MODEL_PRICES.inputPerMillionUsd),
            outputPerMillionUsd: positiveNumber(this.env.OUTPUT_PRICE_PER_MILLION_USD, DEFAULT_MODEL_PRICES.outputPerMillionUsd)
        };
        const handler = createChatPostHandler(createOpenAICareerAssistantService(this.env.OPENAI_API_KEY), {
            limiter: new D1UsageLimitService(this.env.DB, limits),
            prices
        });
        const response = await handler({
            request: new Request("https://kwela.internal/api/chat", {
                method: "POST",
                headers: { "content-type": "application/json" },
                body: requestBody
            }),
            ownerId: row.owner_id
        });
        const text = await response.text();
        if (!response.ok) {
            let message = "Kwela couldn’t finish that one. Tap Try again.";
            try {
                const body = JSON.parse(text) as { message?: unknown };
                if (typeof body.message === "string") message = body.message;
            } catch {
                // Keep the safe fallback when the assistant returns an unexpected error body.
            }
            if (response.status < 500) throw new NonRetryableError(message);
            throw new Error(message);
        }
        try {
            JSON.parse(text);
        } catch {
            throw new Error("Kwela sent a reply the app couldn’t read. Please try again.");
        }
        return text;
    }
}
