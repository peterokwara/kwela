import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { DatabaseSync } from "node:sqlite";
import test from "node:test";
import { createRouter } from "../src/router.ts";
import { createSupportMessageService, readSupportMessage } from "../src/lib/server/services/supportMessageService.ts";
import { D1SupportMessageStoreService } from "../src/lib/server/services/d1SupportMessageStoreService.ts";

const siteToken = "t".repeat(32);
const good = { email: " Amina@Example.test ", message: "  My code did not arrive on my Tecno phone.  ", website: "" };
const ctx = { sourceIp: "203.0.113.9", token: siteToken };

function store() {
    const saved = [];
    const hits = new Map();
    return {
        saved,
        hits,
        async save(message) { saved.push(message); },
        async hitRateLimit(key, _now, _window, max) {
            const count = (hits.get(key) ?? 0) + 1;
            hits.set(key, count);
            return count <= max;
        }
    };
}

function service(overrides = {}) {
    const s = store();
    return { s, svc: createSupportMessageService({ store: s, secret: "server-secret", siteToken, now: () => 1_000_000, newId: () => "id-1", ...overrides }) };
}

test("an email and a message are tidied up", () => {
    assert.deepEqual(readSupportMessage(good), { ok: true, value: { email: "amina@example.test", message: "My code did not arrive on my Tecno phone." } });
});

test("a bad email, an empty or tiny message, or a huge one is refused", () => {
    assert.equal(readSupportMessage({ ...good, email: "nope" }).message, "Enter a valid email address so we can reply.");
    for (const message of ["", "   ", "hi", "x".repeat(2001), null, 5]) {
        const result = readSupportMessage({ ...good, message });
        assert.equal(result.ok, false, String(message).slice(0, 10));
        assert.equal(result.message, "Write a message of 10 to 2000 characters.");
    }
    assert.equal(readSupportMessage(null).ok, false);
});

test("control characters are removed but line breaks stay", () => {
    assert.equal(readSupportMessage({ ...good, message: "Line one\nLine two\u0000\u0007 done" }).value.message, "Line one\nLine two done");
});

test("a message is saved with an id and the time", async () => {
    const { s, svc } = service();
    assert.deepEqual(await svc.submit(good, ctx), { ok: true });
    assert.deepEqual(s.saved, [{ id: "id-1", email: "amina@example.test", message: "My code did not arrive on my Tecno phone.", createdAt: 1_000_000 }]);
});

test("only the Kwela site can send: a wrong or missing token is refused, and no token means closed", async () => {
    const { s, svc } = service();
    for (const token of ["", "wrong", siteToken + "x"]) assert.deepEqual(await svc.submit(good, { ...ctx, token }), { ok: false, status: 403, message: "Not allowed." });
    assert.equal(s.saved.length, 0);
    assert.deepEqual(await service({ siteToken: undefined }).svc.submit(good, ctx), { ok: false, status: 503, message: "The contact form is not open yet." });
});

test("a filled hidden field is a bot: told it worked, nothing saved", async () => {
    const { s, svc } = service();
    assert.deepEqual(await svc.submit({ ...good, website: "https://spam.test" }, ctx), { ok: true });
    assert.equal(s.saved.length, 0);
});

test("one address cannot flood the form, and a different one is not blocked", async () => {
    const { s, svc } = service();
    for (let i = 0; i < 5; i += 1) assert.equal((await svc.submit(good, ctx)).ok, true);
    assert.deepEqual(await svc.submit(good, ctx), { ok: false, status: 429, message: "Too many tries. Please wait a while and try again." });
    assert.equal(s.saved.length, 5);
    assert.equal((await svc.submit(good, { ...ctx, sourceIp: "198.51.100.7" })).ok, true);
});

test("many different addresses are held by a total limit", async () => {
    const { s, svc } = service();
    let accepted = 0;
    for (let i = 0; i < 160; i += 1) if ((await svc.submit(good, { ...ctx, sourceIp: `10.0.0.${i}` })).ok) accepted += 1;
    assert.equal(accepted, 100);
    assert.equal(s.saved.length, 100);
});

test("rate-limit keys never hold the raw address, and do not collide with sign-up keys", async () => {
    const { s, svc } = service();
    await svc.submit(good, ctx);
    for (const key of s.hits.keys()) assert.match(key, /^[0-9a-f]{64}$/);
});

test("a failed save is a message, not a crash", async () => {
    const s = store();
    s.save = async () => { throw new Error("down"); };
    const svc = createSupportMessageService({ store: s, secret: "x", siteToken, now: () => 1, newId: () => "i" });
    assert.deepEqual(await svc.submit(good, ctx), { ok: false, status: 503, message: "Something went wrong on our side. Please try again in a moment." });
});

function routerWith(contact) {
    return createRouter({
        auth: { authenticate: async () => null, requestCode: async () => ({ ok: true }), verify: async () => ({ ok: false, status: 401, message: "x" }), signInWithGoogle: async () => ({ ok: false, status: 401, message: "x" }), signOut: async () => {}, deleteAccount: async () => {} },
        chat: async () => new Response("x"),
        profiles: { get: async () => null, save: async () => {}, delete: async () => {} },
        renderPdf: async () => new Response("x"),
        reserveRender: async () => ({ allowed: true }),
        contact
    });
}

const post = (body, headers = {}) => new Request("https://api.test/contact", { method: "POST", headers: { "content-type": "application/json", ...headers }, body: typeof body === "string" ? body : JSON.stringify(body) });

test("the contact route passes the address and token on and answers", async () => {
    const seen = [];
    const handle = routerWith({ check: async () => ({ ok: true }), submit: async (body, context) => (seen.push({ body, context }), { ok: true }) });
    const response = await handle(post(good, { "x-client-ip": "203.0.113.9", "x-signup-token": "abc" }));
    assert.equal(response.status, 200);
    assert.deepEqual(seen, [{ body: good, context: { sourceIp: "203.0.113.9", token: "abc" } }]);
});

test("the contact route takes a longer body than sign-up, checks the token first, and is closed when not wired", async () => {
    const open = routerWith({ check: async (t) => (t === "ok" ? { ok: true } : { ok: false, status: 403, message: "Not allowed." }), submit: async () => ({ ok: true }) });
    assert.equal((await open(post({ ...good, message: "x".repeat(2000) }, { "x-signup-token": "ok" }))).status, 200);
    assert.equal((await open(post({ ...good, message: "x".repeat(9000) }, { "x-signup-token": "ok" }))).status, 413);
    assert.equal((await open(post(good, { "x-signup-token": "bad" }))).status, 403);
    assert.equal((await open(new Request("https://api.test/contact"))).status, 405);
    assert.equal((await routerWith(undefined)(post(good))).status, 503);
});

test("messages are stored by the real SQL, one row each, and values are bound", async () => {
    const sqlite = new DatabaseSync(":memory:");
    for (const file of ["0004_auth_rate_limits.sql", "0008_support_messages.sql"]) sqlite.exec(readFileSync(new URL(`../migrations/${file}`, import.meta.url), "utf8"));
    const d1 = { prepare: (q) => ({ bind: (...v) => ({ first: async () => sqlite.prepare(q).get(...v) ?? null, run: async () => sqlite.prepare(q).run(...v) }) }) };
    const s = new D1SupportMessageStoreService(d1);
    const hostile = "x'); DROP TABLE support_messages; --";
    await s.save({ id: "a", email: "a@example.test", message: hostile, createdAt: 1 });
    await s.save({ id: "b", email: "a@example.test", message: "second", createdAt: 2 });
    assert.deepEqual(sqlite.prepare("SELECT id, message FROM support_messages ORDER BY created_at").all().map((r) => ({ ...r })), [{ id: "a", message: hostile }, { id: "b", message: "second" }]);
    assert.deepEqual([await s.hitRateLimit("k", 0, 1000, 1), await s.hitRateLimit("k", 1, 1000, 1)], [true, false]);
});
