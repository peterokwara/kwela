import assert from "node:assert/strict";
import test from "node:test";
import { accountIdForEmail, createAuthService } from "../src/lib/server/services/authService.ts";

const NOW = 1_800_000_000_000;
const accountIdFor = (email) => accountIdForEmail("s".repeat(32), email);

function setup({ existing = false, internal = false } = {}) {
    const events = [];
    const sessions = [];
    const store = {
        codes: new Map(),
        async hitRateLimit() { return true; },
        async saveCode(code) { this.codes.set(code.email, code); },
        async deleteCode() {},
        async checkAndConsumeCode() { return "consumed"; },
        async findOrCreateAccount(email, id) { return { id, email, createdAt: existing ? NOW - 86_400_000 : NOW }; },
        async createSession(session) { sessions.push(session); },
        async deleteAccountData() {}
    };
    const analytics = { async capture(id, event, properties) { events.push({ id, event, properties }); } };
    const auth = createAuthService({
        store, mailer: { async send() {} }, secret: "s".repeat(32), now: () => NOW, analytics,
        internalIds: async () => (internal ? new Set([await accountIdFor("amina@example.com")]) : new Set()),
        googleVerifier: async () => ({ email: "amina@example.com", name: "Amina" })
    });
    return { auth, events, sessions };
}

test("a first email sign-in counts as a new user and a sign-in, under the opaque account id", async () => {
    const { auth, events } = setup();
    const result = await auth.verify("Amina@Example.com", "123456");
    assert.equal(result.ok, true);
    assert.match(result.analyticsId, /^[0-9a-f]{64}$/);
    assert.deepEqual(events.map((e) => e.event), ["account:user_create", "account:user_sign_in"]);
    assert.equal(events[0].id, result.analyticsId);
    assert.equal(events[1].properties.sign_in_method, "email_code");
    assert.equal(JSON.stringify(events).includes("amina"), false, "no email in any event");
});

test("a returning person is only a sign-in, not a new user", async () => {
    const { auth, events } = setup({ existing: true });
    await auth.verify("amina@example.com", "123456");
    assert.deepEqual(events.map((e) => e.event), ["account:user_sign_in"]);
});

test("Google sign-in says how the person came in and returns the same kind of id", async () => {
    const { auth, events } = setup();
    const result = await auth.signInWithGoogle("good-token");
    assert.equal(result.ok, true);
    assert.match(result.analyticsId, /^[0-9a-f]{64}$/);
    assert.equal(events.at(-1).properties.sign_in_method, "google");
    assert.equal(JSON.stringify(events).includes("Amina"), false, "no name in any event");
});

test("the same person gets the same id whichever way they sign in", async () => {
    const { auth } = setup();
    const byCode = await auth.verify("amina@example.com", "123456");
    const byGoogle = await auth.signInWithGoogle("good-token");
    assert.equal(byCode.analyticsId, byGoogle.analyticsId);
});

test("deleting an account is recorded once, then the person is gone from analytics too", async () => {
    const { auth, events } = setup();
    await auth.deleteAccount("acct-1", "amina@example.com");
    assert.deepEqual(events.map((e) => e.event), ["account:user_delete"]);
    assert.equal(events[0].id, "acct-1");
});

test("a failing analytics sender never breaks signing in", async () => {
    const store = {
        async hitRateLimit() { return true; }, async checkAndConsumeCode() { return "consumed"; },
        async findOrCreateAccount(email, id) { return { id, email, createdAt: NOW }; }, async createSession() {}
    };
    const analytics = { async capture() { throw new Error("posthog down"); } };
    const auth = createAuthService({ store, mailer: { async send() {} }, secret: "s".repeat(32), now: () => NOW, analytics });
    const result = await auth.verify("a@example.com", "123456");
    assert.equal(result.ok, true);
});

test("a tester's sign-in says the account is internal, so the phone can tag its events too", async () => {
    const tester = setup({ internal: true });
    assert.equal((await tester.auth.verify("amina@example.com", "123456")).analyticsInternal, true);
    assert.equal((await tester.auth.signInWithGoogle("good-token")).analyticsInternal, true);
    const person = setup();
    assert.equal((await person.auth.verify("amina@example.com", "123456")).analyticsInternal, false);
});
