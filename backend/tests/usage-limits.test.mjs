import assert from "node:assert/strict";
import test from "node:test";
import { createChatPostHandler } from "../src/lib/server/services/chatPostHandlerService.ts";
import { D1UsageLimitService } from "../src/lib/server/services/d1UsageLimitService.ts";
import {
    DEFAULT_USAGE_LIMITS,
    MAX_RENDERS_PER_MINUTE,
    MINIMUM_CHARGE_MICROS,
    RENDER_CHARGE_MICROS,
    estimateCostMicros,
    evaluateUsage
} from "../src/lib/server/services/usagePolicy.ts";

const prices = { inputPerMillionUsd: 5, outputPerMillionUsd: 20 };

test("cost is estimated from tokens in millionths of a dollar", () => {
    assert.equal(estimateCostMicros({ inputTokens: 100_000, outputTokens: 10_000 }, prices), 700_000);
});

test("a visitor under every limit may continue", () => {
    assert.deepEqual(
        evaluateUsage({ requestsLastMinute: 0, spentMicros: 0, globalSpentMicros: 0 }, DEFAULT_USAGE_LIMITS),
        {
            allowed: true
        }
    );
});

test("a visitor who has spent the cap is stopped with a plain message", () => {
    const decision = evaluateUsage(
        { requestsLastMinute: 0, spentMicros: 1_000_000, globalSpentMicros: 0 },
        DEFAULT_USAGE_LIMITS
    );
    assert.equal(decision.allowed, false);
    assert.equal(decision.status, 429);
    assert.match(decision.message, /free Kwela credit/);
});

test("too many requests in a minute are slowed down", () => {
    const decision = evaluateUsage(
        { requestsLastMinute: 4, spentMicros: 0, globalSpentMicros: 0 },
        DEFAULT_USAGE_LIMITS
    );
    assert.equal(decision.allowed, false);
    assert.match(decision.message, /Slow down/);
});

test("the overall budget stops everyone", () => {
    const decision = evaluateUsage(
        { requestsLastMinute: 0, spentMicros: 0, globalSpentMicros: 25_000_000 },
        DEFAULT_USAGE_LIMITS
    );
    assert.equal(decision.allowed, false);
    assert.equal(decision.status, 503);
});

/** Minimal in-memory D1 stand-in for the usage table. */
function fakeDatabase() {
    const rows = [];
    let nextId = 1;
    return {
        rows,
        prepare(query) {
            return {
                bind(...values) {
                    return {
                        async first() {
                            if (query.startsWith("INSERT")) {
                                const [owner, at, cost, kind] = values;
                                const id = nextId++;
                                rows.push({ id, owner, at, cost, kind });
                                return { id };
                            }
                            if (query.includes("WHERE owner_id")) {
                                const [since, , owner] = values;
                                const mine = rows.filter((row) => row.owner === owner);
                                return {
                                    spent: mine.reduce((sum, row) => sum + row.cost, 0),
                                    recent: mine.filter((row) => row.kind === "request" && row.at > since).length,
                                    renders: mine.filter((row) => row.kind === "render" && row.at > since).length
                                };
                            }
                            return { spent: rows.reduce((sum, row) => sum + row.cost, 0) };
                        },
                        async run() {
                            if (query.startsWith("DELETE")) {
                                const index = rows.findIndex((row) => row.id === values[0]);
                                if (index >= 0) rows.splice(index, 1);
                            }
                        }
                    };
                }
            };
        }
    };
}

function chatRequest() {
    return new Request("http://kwela.test/api/chat", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ messages: [{ id: "1", role: "user", content: "Update my CV." }] })
    });
}

test("chat handler charges real usage and blocks the visitor once the cap is reached", async () => {
    const database = fakeDatabase();
    const limiter = new D1UsageLimitService(database, { ...DEFAULT_USAGE_LIMITS, maxRequestsPerMinute: 100 });
    const assistant = {
        async respond(_messages, onUsage) {
            onUsage({ inputTokens: 100_000, outputTokens: 20_000 });
            return new ReadableStream({
                start(c) {
                    c.enqueue(new TextEncoder().encode("{}"));
                    c.close();
                }
            });
        }
    };
    const handler = createChatPostHandler(assistant, { limiter, prices });

    const statuses = [];
    for (let i = 0; i < 3; i++) statuses.push((await handler({ request: chatRequest(), ownerId: "friend" })).status);
    assert.deepEqual(statuses, [200, 200, 429]);

    const spent = database.rows.filter((row) => row.owner === "friend").reduce((sum, row) => sum + row.cost, 0);
    assert.equal(spent, 1_800_000);
});

test("a failed model attempt still costs the minimum charge and another visitor is unaffected", async () => {
    const database = fakeDatabase();
    const limiter = new D1UsageLimitService(database, DEFAULT_USAGE_LIMITS);
    const assistant = {
        async respond(_messages, onUsage) {
            onUsage(null);
            throw new Error("model failed");
        }
    };
    const handler = createChatPostHandler(assistant, { limiter, prices });
    const response = await handler({ request: chatRequest(), ownerId: "one" });
    assert.equal(response.status, 500);
    assert.equal(
        database.rows.filter((row) => row.owner === "one").reduce((sum, row) => sum + row.cost, 0),
        MINIMUM_CHARGE_MICROS
    );
    assert.equal(database.rows.filter((row) => row.owner === "two").length, 0);
});

test("simultaneous requests cannot slip past the per-minute limit", async () => {
    const database = fakeDatabase();
    const limiter = new D1UsageLimitService(database, { ...DEFAULT_USAGE_LIMITS, maxRequestsPerMinute: 2 });
    const decisions = await Promise.all(Array.from({ length: 6 }, () => limiter.reserve("burst")));
    assert.ok(decisions.filter((decision) => decision.allowed).length <= 2);
    assert.ok(decisions.some((decision) => !decision.allowed));
});

test("chat handler refuses to run when the visitor can't be identified", async () => {
    const database = fakeDatabase();
    const limiter = new D1UsageLimitService(database, DEFAULT_USAGE_LIMITS);
    let called = false;
    const assistant = {
        async respond() {
            called = true;
            return new ReadableStream();
        }
    };
    const response = await createChatPostHandler(assistant, { limiter, prices })({ request: chatRequest() });
    assert.equal(response.status, 503);
    assert.equal(called, false);
});

test("PDF renders are limited per minute and count towards the spending cap", async () => {
    const database = fakeDatabase();
    const service = new D1UsageLimitService(database, DEFAULT_USAGE_LIMITS, () => 1_000_000);
    for (let index = 0; index < MAX_RENDERS_PER_MINUTE; index += 1) {
        assert.deepEqual(await service.reserveRender("owner-1"), { allowed: true });
    }
    const refused = await service.reserveRender("owner-1");
    assert.equal(refused.allowed, false);
    assert.equal(refused.status, 429);
    assert.equal(database.rows.length, MAX_RENDERS_PER_MINUTE, "a refused render leaves no charge behind");
    assert.equal(database.rows[0].cost, RENDER_CHARGE_MICROS);
    assert.deepEqual(await service.reserveRender("owner-2"), { allowed: true });
});

test("renders do not use up the chat allowance", async () => {
    const database = fakeDatabase();
    const service = new D1UsageLimitService(database, DEFAULT_USAGE_LIMITS, () => 1_000_000);
    for (let index = 0; index < MAX_RENDERS_PER_MINUTE; index += 1) await service.reserveRender("owner-1");
    assert.deepEqual(await service.reserve("owner-1"), { allowed: true });
});

test("a visitor who has spent the cap cannot render either", async () => {
    const database = fakeDatabase();
    database.rows.push({ id: 99, owner: "owner-1", at: 1, cost: DEFAULT_USAGE_LIMITS.capMicros, kind: "request" });
    const service = new D1UsageLimitService(database, DEFAULT_USAGE_LIMITS, () => 1_000_000);
    assert.equal((await service.reserveRender("owner-1")).allowed, false);
});
