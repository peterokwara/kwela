import assert from "node:assert/strict";
import test from "node:test";
import { createRouter } from "../src/router.ts";
import { createTesterSignupService, readSignup } from "../src/lib/server/services/testerSignupService.ts";

const siteToken = "t".repeat(32);

function store() {
    const saved = [];
    const hits = new Map();
    return {
        saved,
        hits,
        async save(signup) {
            if (!saved.some((row) => row.email === signup.email && row.phone === signup.phone)) saved.push(signup);
        },
        async hitRateLimit(key, _now, _window, max) {
            const count = (hits.get(key) ?? 0) + 1;
            hits.set(key, count);
            return count <= max;
        }
    };
}

function service(overrides = {}) {
    const s = store();
    return { s, svc: createTesterSignupService({ store: s, secret: "server-secret", siteToken, now: () => 1_000_000, ...overrides }) };
}

const good = { email: "Amina@Example.test ", phone: "0712 345 678", consent: true, website: "" };
const ctx = { sourceIp: "203.0.113.9", token: siteToken };

test("an email is tidied up and a phone number becomes international format", () => {
    assert.deepEqual(readSignup(good), { ok: true, value: { email: "amina@example.test", phone: "+254712345678" } });
});

test("Kenyan numbers are accepted in the ways people type them", () => {
    for (const phone of ["0712345678", "0712 345 678", "0712-345-678", "+254712345678", "+254 712 345 678", "254712345678", "712345678", "0112345678", "00254712345678"]) {
        const result = readSignup({ ...good, phone });
        assert.equal(result.ok, true, phone);
        assert.equal(result.value.phone, phone.replace(/\D/g, "").endsWith("112345678") ? "+254112345678" : "+254712345678", phone);
    }
});

test("other countries are accepted when they are written with a plus", () => {
    assert.equal(readSignup({ ...good, phone: "+1 415 555 2671" }).value.phone, "+14155552671");
});

test("numbers that cannot be reached are refused", () => {
    for (const phone of ["", "abc", "12345", "0812345678", "+254712", "+2547123456789", "07123456789", "+0123456789", "tel:0712345678", "0712345678; DROP TABLE", "07+12 345 678", "0712 345 678 +", "++254712345678", "+(0)712345678"]) {
        const result = readSignup({ ...good, phone });
        assert.equal(result.ok, false, phone);
        assert.equal(result.message, "Enter a phone number we can reach you on, like 0712 345 678. Start with + for other countries.");
    }
});

test("an email that is not an email is refused", () => {
    for (const email of ["", "nope", "a@b", "a b@c.com", `${"a".repeat(250)}@x.co`]) {
        const result = readSignup({ ...good, email });
        assert.equal(result.ok, false, email);
        assert.equal(result.message, "Enter a valid email address.");
    }
});

test("values that are not text are refused instead of crashing", () => {
    for (const bad of [null, 5, {}, [], true]) {
        assert.equal(readSignup({ ...good, email: bad }).ok, false);
        assert.equal(readSignup({ ...good, phone: bad }).ok, false);
    }
    assert.equal(readSignup(null).ok, false);
});

test("a sign-up is saved with the time consent was given", async () => {
    const { s, svc } = service();
    assert.deepEqual(await svc.submit(good, ctx), { ok: true });
    assert.deepEqual(s.saved, [{ email: "amina@example.test", phone: "+254712345678", consentAt: 1_000_000, createdAt: 1_000_000 }]);
});

test("nothing is saved without consent", async () => {
    const { s, svc } = service();
    for (const consent of [false, undefined, "yes", 1, "true"]) {
        const result = await svc.submit({ ...good, consent }, ctx);
        assert.equal(result.ok, false);
        assert.equal(result.status, 400);
        assert.equal(result.message, "Tick the box so we know it is okay to contact you.");
    }
    assert.equal(s.saved.length, 0);
});

test("signing up twice gives the same answer and keeps one row", async () => {
    const { s, svc } = service();
    assert.deepEqual(await svc.submit(good, ctx), { ok: true });
    assert.deepEqual(await svc.submit(good, ctx), { ok: true });
    assert.equal(s.saved.length, 1);
});

test("a stranger using someone's email cannot replace their phone number", async () => {
    const { s, svc } = service();
    await svc.submit(good, ctx);
    assert.deepEqual(await svc.submit({ ...good, phone: "0722 000 111" }, { ...ctx, sourceIp: "198.51.100.7" }), { ok: true });
    assert.deepEqual(s.saved.map((row) => row.phone), ["+254712345678", "+254722000111"]);
});

test("a failed save is an error message, not a crash", async () => {
    const s = store();
    s.save = async () => { throw new Error("D1 is down"); };
    const svc = createTesterSignupService({ store: s, secret: "server-secret", siteToken, now: () => 1 });
    assert.deepEqual(await svc.submit(good, ctx), { ok: false, status: 503, message: "Something went wrong on our side. Please try again in a moment." });
});

test("sending many different addresses cannot get around the limits", async () => {
    const { s, svc } = service();
    let accepted = 0;
    for (let i = 0; i < 260; i += 1) {
        const result = await svc.submit({ ...good, email: `p${i}@example.test` }, { ...ctx, sourceIp: `10.0.${Math.floor(i / 250)}.${i % 250}` });
        if (result.ok) accepted += 1;
        else assert.equal(result.status, 429);
    }
    assert.equal(accepted, 200);
    assert.equal(s.saved.length, 200);
});

test("a filled hidden field is a bot: it is told it worked and nothing is saved", async () => {
    const { s, svc } = service();
    assert.deepEqual(await svc.submit({ ...good, website: "https://spam.test" }, ctx), { ok: true });
    assert.equal(s.saved.length, 0);
});

test("only the Kwela site can submit: a missing or wrong token is refused before anything is read", async () => {
    const { s, svc } = service();
    for (const token of ["", "wrong", siteToken.slice(1), siteToken + "x"]) {
        const result = await svc.submit(good, { ...ctx, token });
        assert.deepEqual(result, { ok: false, status: 403, message: "Not allowed." });
    }
    assert.equal(s.saved.length, 0);
});

test("without a configured token the form is closed, not open", async () => {
    const { svc } = service({ siteToken: undefined });
    assert.deepEqual(await svc.submit(good, ctx), { ok: false, status: 503, message: "Sign-ups are not open yet." });
});

test("one address cannot flood the form", async () => {
    const { s, svc } = service();
    for (let i = 0; i < 5; i += 1) assert.equal((await svc.submit({ ...good, email: `p${i}@example.test` }, ctx)).ok, true);
    const blocked = await svc.submit({ ...good, email: "p9@example.test" }, ctx);
    assert.deepEqual(blocked, { ok: false, status: 429, message: "Too many tries. Please wait a while and try again." });
    assert.equal(s.saved.length, 5);
    assert.equal((await svc.submit({ ...good, email: "p9@example.test" }, { ...ctx, sourceIp: "198.51.100.7" })).ok, true);
});

test("rate-limit keys never contain the address or the raw IP", async () => {
    const { s, svc } = service();
    await svc.submit(good, ctx);
    for (const key of s.hits.keys()) {
        assert.doesNotMatch(key, /203\.0\.113\.9|amina|example/);
        assert.match(key, /^[0-9a-f]{64}$/);
    }
});

function routerWith(deps) {
    return createRouter({
        auth: { authenticate: async () => null, requestCode: async () => ({ ok: true }), verify: async () => ({ ok: false, status: 401, message: "x" }), signInWithGoogle: async () => ({ ok: false, status: 401, message: "x" }), signOut: async () => {}, deleteAccount: async () => {} },
        chat: async () => new Response("x"),
        profiles: { get: async () => null, save: async () => {}, delete: async () => {} },
        renderPdf: async () => new Response("x"),
        reserveRender: async () => ({ allowed: true }),
        ...deps
    });
}

function post(body, headers = {}) {
    return new Request("https://api.test/signup", { method: "POST", headers: { "content-type": "application/json", ...headers }, body: typeof body === "string" ? body : JSON.stringify(body) });
}

test("the route passes the caller address and token to the service", async () => {
    const seen = [];
    const handle = routerWith({ signups: { check: async () => ({ ok: true }), submit: async (body, context) => (seen.push({ body, context }), { ok: true }) } });
    const response = await handle(post(good, { "x-client-ip": "203.0.113.9", "x-signup-token": "abc" }));
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { ok: true });
    assert.deepEqual(seen, [{ body: good, context: { sourceIp: "203.0.113.9", token: "abc" } }]);
});

test("the route turns a service refusal into its status and message", async () => {
    const handle = routerWith({ signups: { check: async () => ({ ok: true }), submit: async () => ({ ok: false, status: 429, message: "Too many tries. Please wait a while and try again." }) } });
    const response = await handle(post(good));
    assert.equal(response.status, 429);
    assert.deepEqual(await response.json(), { message: "Too many tries. Please wait a while and try again." });
});

test("the route only takes POST, small JSON objects, and is closed when not wired", async () => {
    const open = routerWith({ signups: { check: async () => ({ ok: true }), submit: async () => ({ ok: true }) } });
    assert.equal((await open(new Request("https://api.test/signup", { method: "GET" }))).status, 405);
    assert.equal((await open(post("not json"))).status, 400);
    assert.equal((await open(post("[]"))).status, 400);
    assert.equal((await open(post({ email: "a".repeat(5000) }))).status, 413);
    assert.equal((await routerWith({})(post(good))).status, 503);
});

test("the route needs no sign-in, and nothing else about sign-ups is readable", async () => {
    const handle = routerWith({ signups: { check: async () => ({ ok: true }), submit: async () => ({ ok: true }) } });
    assert.equal((await handle(post(good))).status, 200);
    for (const path of ["/signups", "/signup/list", "/api/signup"]) {
        const response = await handle(new Request(`https://api.test${path}`, { method: "GET" }));
        assert.ok([401, 404].includes(response.status), path);
    }
});

test("a stranger is turned away before the body is read, however large it is", async () => {
    let read = false;
    const handle = routerWith({ signups: { check: async (token) => (token === "ok" ? { ok: true } : { ok: false, status: 403, message: "Not allowed." }), submit: async () => ({ ok: true }) } });
    const big = new Request("https://api.test/signup", { method: "POST", headers: { "content-type": "application/json" }, body: new ReadableStream({ pull(controller) { read = true; controller.enqueue(new TextEncoder().encode("x".repeat(100_000))); controller.close(); } }, { highWaterMark: 0 }), duplex: "half" });
    const response = await handle(big);
    assert.equal(response.status, 403);
    assert.equal(read, false);
});

test("a body that is too large is cut off while it streams", async () => {
    const handle = routerWith({ signups: { check: async () => ({ ok: true }), submit: async () => ({ ok: true }) } });
    let chunks = 0;
    const body = new ReadableStream({ pull(controller) { chunks += 1; controller.enqueue(new TextEncoder().encode("x".repeat(1000))); if (chunks > 1000) controller.close(); } }, { highWaterMark: 0 });
    const response = await handle(new Request("https://api.test/signup", { method: "POST", headers: { "content-type": "application/json" }, body, duplex: "half" }));
    assert.equal(response.status, 413);
    assert.ok(chunks < 20, `read ${chunks} chunks`);
});
