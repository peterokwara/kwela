import assert from "node:assert/strict";
import test from "node:test";
import { createAnalytics, describeReplyForAnalytics } from "../src/lib/server/services/analytics.ts";
import { ANALYTICS_EVENTS } from "../src/lib/server/services/analyticsEvents.ts";

const HOST = "https://us.i.posthog.com";

function recorder(response = { ok: true, status: 200 }) {
    const calls = [];
    const fetcher = async (url, init) => {
        calls.push({ url, body: JSON.parse(init.body), method: init.method });
        if (response instanceof Error) throw response;
        return response;
    };
    return { calls, fetcher };
}

test("an event goes to PostHog's capture endpoint with the account id, never an email", async () => {
    const { calls, fetcher } = recorder();
    const analytics = createAnalytics({ apiKey: "phc_test", host: HOST, fetcher, now: () => 1_800_000_000_000 });
    await analytics.capture("acct-123", "document:pdf_render", { document_type: "resume" });
    assert.equal(calls.length, 1);
    assert.equal(calls[0].url, `${HOST}/i/v0/e/`);
    assert.equal(calls[0].method, "POST");
    assert.equal(calls[0].body.api_key, "phc_test");
    assert.equal(calls[0].body.event, "document:pdf_render");
    assert.equal(calls[0].body.distinct_id, "acct-123");
    assert.equal(calls[0].body.properties.document_type, "resume");
    assert.equal(calls[0].body.properties.$lib, "kwela-api");
    assert.equal(calls[0].body.timestamp, new Date(1_800_000_000_000).toISOString());
});

test("internal and test accounts are tagged so PostHog can filter them out", async () => {
    const { calls, fetcher } = recorder();
    const analytics = createAnalytics({ apiKey: "k", host: HOST, fetcher, internalIds: async () => new Set(["tester-1"]) });
    await analytics.capture("tester-1", "chat:application_generate");
    await analytics.capture("someone", "chat:application_generate");
    assert.equal(calls[0].body.properties.is_internal_user, true);
    assert.equal(calls[1].body.properties.is_internal_user, false);
});

test("without a PostHog key nothing is sent and nothing breaks", async () => {
    const { calls, fetcher } = recorder();
    const analytics = createAnalytics({ apiKey: "", host: HOST, fetcher });
    await analytics.capture("a", "chat:run_fail");
    assert.equal(calls.length, 0);
});

test("a PostHog outage never reaches the person: errors are swallowed", async () => {
    const analytics = createAnalytics({ apiKey: "k", host: HOST, fetcher: recorder(new Error("network down")).fetcher });
    await assert.doesNotReject(analytics.capture("a", "chat:run_fail"));
    const rejecting = createAnalytics({ apiKey: "k", host: HOST, fetcher: recorder({ ok: false, status: 503 }).fetcher });
    await assert.doesNotReject(rejecting.capture("a", "chat:run_fail"));
});

test("event names follow the category:object_action convention, in lowercase snake case", () => {
    const verbs = ["click", "view", "create", "update", "delete", "send", "resend", "fail", "generate", "start", "end", "open", "download", "share", "render", "sign_in"];
    const pattern = new RegExp(`^[a-z]+(_[a-z]+)*:[a-z]+(_[a-z]+)*_(${verbs.join("|")})$`);
    for (const name of Object.values(ANALYTICS_EVENTS)) assert.match(name, pattern, name);
    assert.equal(new Set(Object.values(ANALYTICS_EVENTS)).size, Object.values(ANALYTICS_EVENTS).length, "no duplicate names");
});

test("a reply is summarised by what it contains, never by its words", () => {
    const reply = JSON.stringify({ type: "application_pack", message: "Hi Wanjiku, your CV is ready", applicationPack: { resume: { skills: "x" }, coverLetter: { letterContent: "y" }, applicationAnswers: [{}, {}], job: { title: "Cook" } } });
    const summary = describeReplyForAnalytics(reply);
    assert.deepEqual(summary, { has_resume: true, has_cover_letter: true, answer_count: 2 });
    assert.equal(JSON.stringify(summary).includes("Wanjiku"), false);
    assert.equal(describeReplyForAnalytics(JSON.stringify({ type: "message", message: "ok", applicationPack: null })), null);
    assert.equal(describeReplyForAnalytics("not json"), null);
});

test("internal accounts are found by their real stored ids, including older random ones", async () => {
    const { analyticsFromEnv, internalAccountIds } = await import("../src/lib/server/services/analyticsFactory.ts");
    const queries = [];
    const DB = {
        prepare(query) {
            return {
                bind(...emails) {
                    return { async all() { queries.push({ query, emails }); return { results: emails.includes("tester@example.test") ? [{ id: "b2fabff2-7e5a-4a03-830b-b7132bf88319" }] : [] }; } };
                }
            };
        }
    };
    const env = { AUTH_SECRET: "s".repeat(32), AUTH_TEST_EMAILS: "Tester@Example.test, other@example.test", DB, POSTHOG_API_KEY: "k" };
    const ids = await internalAccountIds(env);
    assert.equal(ids.has("b2fabff2-7e5a-4a03-830b-b7132bf88319"), true, "the older random id");
    assert.equal(ids.size >= 3, true, "plus the keyed ids of both testers");
    assert.match(queries[0].query, /FROM accounts WHERE email IN/);
    assert.deepEqual(queries[0].emails, ["tester@example.test", "other@example.test"]);
    assert.equal(typeof analyticsFromEnv(env).capture, "function");
});
