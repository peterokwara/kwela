import assert from "node:assert/strict";
import test from "node:test";
import { createRouter } from "../src/router.ts";

const token = "A".repeat(43);
const otherToken = "B".repeat(43);

function fakes() {
    const calls = { chat: [], deleted: [] };
    const profiles = new Map();
    const deps = {
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
                calls.deleted.push(ownerId);
                profiles.delete(ownerId);
            }
        },
        async renderPdf() {
            return new Response("%PDF-1.7", { headers: { "content-type": "application/pdf" } });
        }
    };
    return { calls, profiles, router: createRouter(deps) };
}

function request(path, { method = "GET", auth = token, body } = {}) {
    const headers = new Headers();
    if (auth) headers.set("authorization", `Bearer ${auth}`);
    if (body !== undefined) headers.set("content-type", "application/json");
    return new Request(`https://api.kwela.test${path}`, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body)
    });
}

test("the health check needs no sign-in", async () => {
    const { router } = fakes();
    const response = await router(request("/health", { auth: null }));
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { ok: true });
});

test("api routes refuse requests without a valid install token", async () => {
    const { router, calls } = fakes();
    for (const auth of [null, "short"]) {
        const response = await router(request("/api/chat", { method: "POST", auth, body: { messages: [] } }));
        assert.equal(response.status, 401);
    }
    assert.equal(calls.chat.length, 0);
});

test("chat receives a stable owner id that is not the raw token", async () => {
    const { router, calls } = fakes();
    await router(request("/api/chat", { method: "POST", body: { messages: [] } }));
    await router(request("/api/chat", { method: "POST", body: { messages: [] } }));
    await router(request("/api/chat", { method: "POST", auth: otherToken, body: { messages: [] } }));

    const [first, second, third] = calls.chat.map((call) => call.ownerId);
    assert.match(first, /^[0-9a-f]{64}$/);
    assert.notEqual(first, token);
    assert.equal(first, second);
    assert.notEqual(first, third);
});

test("unknown paths are 404 and wrong methods are 405", async () => {
    const { router } = fakes();
    assert.equal((await router(request("/api/nothing"))).status, 404);
    assert.equal((await router(request("/api/chat"))).status, 405);
});

test("a visitor only sees their own saved CV profile", async () => {
    const { router, profiles } = fakes();
    const empty = await router(request("/api/candidate-profile"));
    assert.deepEqual(await empty.json(), { profile: null });

    const ownerId = await ownerIdFor(token);
    profiles.set(ownerId, { professionalSummary: "Mine" });
    const mine = await router(request("/api/candidate-profile"));
    assert.deepEqual(await mine.json(), { profile: { professionalSummary: "Mine" } });
    const theirs = await router(request("/api/candidate-profile", { auth: otherToken }));
    assert.deepEqual(await theirs.json(), { profile: null });
});

test("saving an incomplete CV profile is rejected", async () => {
    const { router } = fakes();
    const response = await router(request("/api/candidate-profile", { method: "PUT", body: { profile: { nope: true } } }));
    assert.equal(response.status, 400);
});

test("deleting the account removes the visitor's saved data", async () => {
    const { router, calls } = fakes();
    const response = await router(request("/api/account", { method: "DELETE" }));
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { ok: true });
    assert.deepEqual(calls.deleted, [await ownerIdFor(token)]);
});

test("pdf export rejects an unknown document type", async () => {
    const { router } = fakes();
    const response = await router(request("/api/documents/pdf", { method: "POST", body: { type: "poem", document: {} } }));
    assert.equal(response.status, 400);
});

async function ownerIdFor(value) {
    const bytes = new TextEncoder().encode(value);
    const digest = await crypto.subtle.digest("SHA-256", bytes);
    return [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, "0")).join("");
}
