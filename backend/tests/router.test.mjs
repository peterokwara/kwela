import assert from "node:assert/strict";
import test from "node:test";
import { createRouter } from "../src/router.ts";

const tokenA = "A".repeat(43);
const tokenB = "B".repeat(43);

function fakes() {
    const calls = { chat: [], deletedProfiles: [], deletedAccounts: [], signedOut: [], requested: [], verified: [], renders: [], renderDecision: { allowed: true } };
    const sessions = new Map([
        [tokenA, { accountId: "acct-a", email: "a@example.test" }],
        [tokenB, { accountId: "acct-b", email: "b@example.test" }]
    ]);
    const profiles = new Map();
    const auth = {
        async requestCode(email, sourceIp) {
            calls.requested.push({ email, sourceIp });
            return email === "limited@example.test"
                ? { ok: false, status: 429, message: "Too many codes requested." }
                : email === "reviewer@example.test"
                  ? { ok: true, testCode: "123456" }
                  : { ok: true };
        },
        async verify(email, code) {
            calls.verified.push({ email, code });
            return code === "123456"
                ? { ok: true, token: tokenA, email }
                : { ok: false, status: 401, message: "That code isn’t right." };
        },
        async signInWithGoogle(idToken, sourceIp) {
            calls.google = calls.google ?? [];
            calls.google.push({ idToken, sourceIp });
            return idToken === "good-token"
                ? { ok: true, token: tokenA, email: "amina@example.test", name: "Amina" }
                : { ok: false, status: 401, message: "Google sign-in didn’t work. Please try again." };
        },
        async authenticate(token) {
            return sessions.get(token) ?? null;
        },
        async signOut(token) {
            calls.signedOut.push(token);
            sessions.delete(token);
        },
        async deleteAccount(accountId, email) {
            calls.deletedAccounts.push({ accountId, email });
        }
    };
    const deps = {
        auth,
        async chat(request, ownerId) {
            calls.chat.push({ ownerId, body: await request.json() });
            return Response.json({ type: "message", message: "Hello" });
        },
        profiles: {
            async get(ownerId) {
                return profiles.get(ownerId) ?? null;
            },
            async save(ownerId, profile) {
                profiles.set(ownerId, profile);
            },
            async delete(ownerId) {
                calls.deletedProfiles.push(ownerId);
                profiles.delete(ownerId);
            }
        },
        async reserveRender(ownerId) {
            calls.renders.push(ownerId);
            return calls.renderDecision;
        },
        async renderPdf() {
            return new Response("%PDF-1.7", { headers: { "content-type": "application/pdf" } });
        }
    };
    return { calls, profiles, router: createRouter(deps) };
}

function request(path, { method = "GET", auth = tokenA, body, ip } = {}) {
    const headers = new Headers();
    if (auth) headers.set("authorization", `Bearer ${auth}`);
    if (ip) headers.set("cf-connecting-ip", ip);
    if (body !== undefined) headers.set("content-type", "application/json");
    return new Request(`https://api.kwela.test${path}`, {
        method,
        headers,
        body: body === undefined ? undefined : typeof body === "string" ? body : JSON.stringify(body)
    });
}

test("the health check needs no sign-in", async () => {
    const { router } = fakes();
    const response = await router(request("/health", { auth: null }));
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { ok: true });
});

test("the request-code route returns the private test code only when the auth service allows the address", async () => {
    const { router } = fakes();
    const response = await router(request("/auth/request-code", { method: "POST", auth: null, body: { email: "reviewer@example.test" } }));
    assert.deepEqual(await response.json(), { ok: true, testCode: "123456" });
});

test("api routes refuse a missing, malformed or unknown session", async () => {
    const { router, calls } = fakes();
    for (const auth of [null, "short", "C".repeat(43)]) {
        const response = await router(request("/api/chat", { method: "POST", auth, body: { messages: [] } }));
        assert.equal(response.status, 401);
    }
    assert.equal(calls.chat.length, 0);
});

test("chat is run for the signed-in account, not the raw token", async () => {
    const { router, calls } = fakes();
    await router(request("/api/chat", { method: "POST", body: { messages: [] } }));
    await router(request("/api/chat", { method: "POST", auth: tokenB, body: { messages: [] } }));
    assert.deepEqual(
        calls.chat.map((call) => call.ownerId),
        ["acct-a", "acct-b"]
    );
});

test("unknown paths are 404 and wrong methods are 405", async () => {
    const { router } = fakes();
    assert.equal((await router(request("/api/nothing"))).status, 404);
    assert.equal((await router(request("/api/chat"))).status, 405);
    assert.equal((await router(request("/auth/verify", { auth: null }))).status, 405);
});

test("a person only sees their own saved CV profile", async () => {
    const { router, profiles } = fakes();
    assert.deepEqual(await (await router(request("/api/candidate-profile"))).json(), { profile: null });
    profiles.set("acct-a", { professionalSummary: "Mine" });
    assert.deepEqual(await (await router(request("/api/candidate-profile"))).json(), { profile: { professionalSummary: "Mine" } });
    assert.deepEqual(await (await router(request("/api/candidate-profile", { auth: tokenB }))).json(), { profile: null });
});

test("saving an incomplete CV profile is rejected", async () => {
    const { router } = fakes();
    const response = await router(request("/api/candidate-profile", { method: "PUT", body: { profile: { nope: true } } }));
    assert.equal(response.status, 400);
});

test("deleting the account removes the saved profile and the account itself", async () => {
    const { router, calls } = fakes();
    const response = await router(request("/api/account", { method: "DELETE" }));
    assert.equal(response.status, 200);
    assert.deepEqual(calls.deletedProfiles, ["acct-a"]);
    assert.deepEqual(calls.deletedAccounts, [{ accountId: "acct-a", email: "a@example.test" }]);
});

test("pdf export rejects an unknown document type", async () => {
    const { router } = fakes();
    const response = await router(request("/api/documents/pdf", { method: "POST", body: { type: "poem", document: {} } }));
    assert.equal(response.status, 400);
});

test("asking for a sign-in code passes the email and Cloudflare source IP and reports limits", async () => {
    const { router, calls } = fakes();
    const ok = await router(request("/auth/request-code", { method: "POST", auth: null, body: { email: "new@example.test" } }));
    assert.equal(ok.status, 200);
    assert.deepEqual(await ok.json(), { ok: true });
    assert.deepEqual(calls.requested, [{ email: "new@example.test", sourceIp: undefined }]);

    await router(request("/auth/request-code", { method: "POST", auth: null, ip: "203.0.113.8", body: { email: "other@example.test" } }));
    assert.deepEqual(calls.requested[1], { email: "other@example.test", sourceIp: "203.0.113.8" });

    const limited = await router(request("/auth/request-code", { method: "POST", auth: null, body: { email: "limited@example.test" } }));
    assert.equal(limited.status, 429);
    assert.deepEqual(await limited.json(), { message: "Too many codes requested." });
});

test("sign-in routes refuse a body that is not the expected JSON", async () => {
    const { router, calls } = fakes();
    for (const body of ["not json", { email: 5 }, {}]) {
        const response = await router(request("/auth/request-code", { method: "POST", auth: null, body }));
        assert.equal(response.status, 400);
    }
    assert.equal(calls.requested.length, 0);
});

test("verifying a code returns a token, and a wrong code is a 401", async () => {
    const { router } = fakes();
    const good = await router(request("/auth/verify", { method: "POST", auth: null, body: { email: "a@example.test", code: "123456" } }));
    assert.equal(good.status, 200);
    assert.deepEqual(await good.json(), { token: tokenA, email: "a@example.test" });

    const bad = await router(request("/auth/verify", { method: "POST", auth: null, body: { email: "a@example.test", code: "000000" } }));
    assert.equal(bad.status, 401);
    assert.deepEqual(await bad.json(), { message: "That code isn’t right." });
});

test("signing out revokes the session", async () => {
    const { router, calls } = fakes();
    const response = await router(request("/auth/sign-out", { method: "POST" }));
    assert.equal(response.status, 200);
    assert.deepEqual(calls.signedOut, [tokenA]);
    assert.equal((await router(request("/api/candidate-profile"))).status, 401);
});

const goodLetter = { coverLetterTitle: "Letter", fullName: "Amina Wanjiru", jobTitle: "Analyst", email: "a@example.test", phoneNumber: "0700", address: null, companyName: "Example Bank", hiringManagerName: null, linkedin: null, letterContent: "Dear team," };

test("pdf export is stopped before rendering when the account is over its limit", async () => {
    const { router, calls } = fakes();
    calls.renderDecision = { allowed: false, status: 429, message: "Slow down a little. Try again in a minute." };
    const response = await router(request("/api/documents/pdf", { method: "POST", body: { type: "cover_letter", document: goodLetter } }));
    assert.equal(response.status, 429);
    assert.match((await response.json()).message, /Slow down/);
    assert.deepEqual(calls.renders, ["acct-a"]);
});

test("pdf export refuses an oversized body without asking the renderer", async () => {
    const { router, calls } = fakes();
    const huge = { type: "cover_letter", document: { ...goodLetter, letterContent: "x".repeat(300_000) } };
    const response = await router(request("/api/documents/pdf", { method: "POST", body: huge }));
    assert.equal(response.status, 413);
    assert.deepEqual(calls.renders, []);
});

test("an incomplete draft costs nothing", async () => {
    const { router, calls } = fakes();
    const response = await router(request("/api/documents/pdf", { method: "POST", body: { type: "resume", document: {} } }));
    assert.equal(response.status, 400);
    assert.deepEqual(calls.renders, []);
});

test("Google sign-in returns a session token, the email and the name", async () => {
    const { router, calls } = fakes();
    const response = await router(request("/auth/google", { method: "POST", auth: null, body: { idToken: "good-token" }, ip: "1.2.3.4" }));
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { token: tokenA, email: "amina@example.test", name: "Amina" });
    assert.deepEqual(calls.google, [{ idToken: "good-token", sourceIp: "1.2.3.4" }]);
});

test("Google sign-in refuses a token that does not check out", async () => {
    const { router } = fakes();
    const response = await router(request("/auth/google", { method: "POST", auth: null, body: { idToken: "forged" } }));
    assert.equal(response.status, 401);
});

test("Google sign-in refuses a missing or oversized token before asking Google", async () => {
    const { router, calls } = fakes();
    assert.equal((await router(request("/auth/google", { method: "POST", auth: null, body: {} }))).status, 400);
    assert.equal((await router(request("/auth/google", { method: "POST", auth: null, body: { idToken: "x".repeat(9000) } }))).status, 400);
    assert.equal(calls.google, undefined);
});

test("Google sign-in only accepts POST", async () => {
    const { router } = fakes();
    assert.equal((await router(request("/auth/google", { auth: null }))).status, 405);
});
