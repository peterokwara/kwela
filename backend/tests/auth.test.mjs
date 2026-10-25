import assert from "node:assert/strict";
import test from "node:test";
import { createAuthService, sha256Hex } from "../src/lib/server/services/authService.ts";
import { AllowlistedTestOtpMailer } from "../src/lib/server/services/otpMailers.ts";

/** In-memory stand-in for the D1 store. Each method is one atomic step, like the SQL it replaces. */
function memoryStore() {
    const codes = new Map();
    const accounts = new Map();
    const sessions = new Map();
    const limits = new Map();
    return {
        codes,
        accounts,
        sessions,
        async getCode(email) {
            const row = codes.get(email);
            return row ? { ...row } : null;
        },
        async saveCode(code) {
            codes.set(code.email, { ...code });
        },
        async deleteCode(email) {
            codes.delete(email);
        },
        async checkAndConsumeCode(email, candidateHash, now, maxAttempts) {
            const row = codes.get(email);
            if (!row || row.expiresAt <= now || row.attempts >= maxAttempts || row.codeHash === "") return "unavailable";
            row.attempts += 1;
            if (row.codeHash !== candidateHash) return "invalid";
            row.codeHash = "";
            return "consumed";
        },
        async hitRateLimit(key, now, windowMs, maxRequests) {
            const row = limits.get(key);
            if (!row || row.windowStart <= now - windowMs) limits.set(key, { windowStart: now, count: 1 });
            else row.count += 1;
            return limits.get(key).count <= maxRequests;
        },
        async findOrCreateAccount(email, proposedId, createdAt) {
            const existing = [...accounts.values()].find((account) => account.email === email);
            if (existing) return { ...existing };
            const account = { id: proposedId, email, createdAt };
            accounts.set(account.id, account);
            return { ...account };
        },
        async createSession(session) {
            sessions.set(session.tokenHash, { ...session });
        },
        async getSession(tokenHash) {
            const session = sessions.get(tokenHash);
            const account = session && accounts.get(session.accountId);
            return session && account ? { accountId: account.id, email: account.email, createdAt: session.createdAt } : null;
        },
        async deleteSession(tokenHash) {
            sessions.delete(tokenHash);
        },
        async deleteAccountData(accountId, email) {
            accounts.delete(accountId);
            codes.delete(email);
            for (const [hash, session] of sessions) if (session.accountId === accountId) sessions.delete(hash);
        }
    };
}

function setup(options = {}) {
    const store = memoryStore();
    const sent = [];
    let clock = 1_000_000;
    const state = { failMail: false };
    const auth = createAuthService({
        store,
        mailer: options.mailer ?? {
            async send(email, code) {
                if (state.failMail) throw new Error("mail down");
                sent.push({ email, code });
            }
        },
        secret: "test-secret",
        now: () => clock,
        testCode: options.testCode
    });
    return { auth, store, sent, state, advance: (ms) => (clock += ms) };
}

const minute = 60_000;

test("asking for a code emails a six-digit code and never stores it in plain text", async () => {
    const { auth, store, sent } = setup();
    const result = await auth.requestCode("  Amina@Example.test ");

    assert.deepEqual(result, { ok: true });
    assert.equal(sent.length, 1);
    assert.equal(sent[0].email, "amina@example.test");
    assert.match(sent[0].code, /^\d{6}$/);
    const stored = store.codes.get("amina@example.test");
    assert.ok(stored);
    assert.ok(!JSON.stringify(stored).includes(sent[0].code));
});

test("the private test login uses its configured code and returns it only after delivery is accepted", async () => {
    const { auth, store, advance } = setup({ testCode: "123456", mailer: new AllowlistedTestOtpMailer(["reviewer@example.test"]) });
    const requested = await auth.requestCode("reviewer@example.test");

    assert.deepEqual(requested, { ok: true, testCode: "123456" });
    assert.ok(!JSON.stringify(store.codes.get("reviewer@example.test")).includes("123456"));

    advance(11 * minute);
    const signedIn = await auth.verify("reviewer@example.test", "123456");
    assert.equal(signedIn.ok, true);
    assert.equal((await auth.authenticate(signedIn.token)).email, "reviewer@example.test");
});

test("the private test code is refused for addresses outside the allow-list", async () => {
    const { auth, store } = setup({ testCode: "123456", mailer: new AllowlistedTestOtpMailer(["reviewer@example.test"]) });

    const result = await auth.requestCode("other@example.test");

    assert.deepEqual(result, { ok: false, status: 503, message: "Kwela couldn’t send the code right now. Please try again in a moment." });
    assert.equal(store.codes.has("other@example.test"), false);
});

test("an invalid email address is refused before any mail is sent", async () => {
    const { auth, sent } = setup();
    for (const email of ["", "nope", "a@b", "a b@c.com"]) {
        const result = await auth.requestCode(email);
        assert.equal(result.ok, false);
        assert.equal(result.status, 400);
    }
    assert.equal(sent.length, 0);
});

test("only three codes can be requested in fifteen minutes", async () => {
    const { auth, sent, advance } = setup();
    for (let i = 0; i < 3; i++) assert.equal((await auth.requestCode("a@example.test")).ok, true);
    const blocked = await auth.requestCode("a@example.test");
    assert.equal(blocked.ok, false);
    assert.equal(blocked.status, 429);
    assert.equal(sent.length, 3);

    advance(16 * minute);
    assert.equal((await auth.requestCode("a@example.test")).ok, true);
});

test("the right code signs in, creates the account once and works as a session", async () => {
    const { auth, sent, store } = setup();
    await auth.requestCode("Amina@example.test");
    const signedIn = await auth.verify("amina@example.test", sent[0].code);

    assert.equal(signedIn.ok, true);
    assert.equal(signedIn.email, "amina@example.test");
    assert.match(signedIn.token, /^[A-Za-z0-9_-]{43}$/);
    assert.equal(store.accounts.size, 1);
    const session = await auth.authenticate(signedIn.token);
    assert.equal(session.email, "amina@example.test");
    assert.match(session.accountId, /^[0-9a-f]{64}$/);
    assert.equal(await auth.authenticate("not-a-real-token-" + "x".repeat(30)), null);
    assert.ok(![...store.sessions.keys()].includes(signedIn.token));
});

test("signing in twice gives the same account id", async () => {
    const { auth, sent } = setup();
    await auth.requestCode("a@example.test");
    const first = await auth.verify("a@example.test", sent[0].code);
    await auth.requestCode("A@example.test");
    const second = await auth.verify("a@example.test", sent[1].code);
    assert.equal((await auth.authenticate(first.token)).accountId, (await auth.authenticate(second.token)).accountId);
});

test("guesses sent at the same moment still count towards the five-try limit", async () => {
    const { auth, sent } = setup();
    await auth.requestCode("a@example.test");
    const wrong = sent[0].code === "000000" ? "111111" : "000000";
    await Promise.all(Array.from({ length: 20 }, () => auth.verify("a@example.test", wrong)));
    assert.equal((await auth.verify("a@example.test", sent[0].code)).ok, false);
});

test("the same right code cannot start two sessions at once", async () => {
    const { auth, sent, store } = setup();
    await auth.requestCode("a@example.test");
    const results = await Promise.all([auth.verify("a@example.test", sent[0].code), auth.verify("a@example.test", sent[0].code)]);
    assert.equal(results.filter((result) => result.ok).length, 1);
    assert.equal(store.sessions.size, 1);
});

test("signing in or failing to send mail does not reset the request limit", async () => {
    const { auth, sent, state } = setup();
    await auth.requestCode("a@example.test");
    await auth.verify("a@example.test", sent[0].code);
    assert.equal((await auth.requestCode("a@example.test")).ok, true);
    assert.equal((await auth.requestCode("a@example.test")).ok, true);
    assert.equal((await auth.requestCode("a@example.test")).status, 429);

    state.failMail = true;
    for (let i = 0; i < 3; i++) assert.equal((await auth.requestCode("b@example.test")).status, 503);
    assert.equal((await auth.requestCode("b@example.test")).status, 429);
});

test("one network address cannot ask for codes for many people", async () => {
    const { auth, sent } = setup();
    for (let i = 0; i < 10; i++) assert.equal((await auth.requestCode(`person${i}@example.test`, "203.0.113.9")).ok, true);
    const blocked = await auth.requestCode("person10@example.test", "203.0.113.9");
    assert.equal(blocked.status, 429);
    assert.equal(sent.length, 10);
    assert.equal((await auth.requestCode("someone@example.test", "198.51.100.4")).ok, true);
});

test("the account id cannot be worked out from the email alone", async () => {
    const { auth, sent } = setup();
    await auth.requestCode("a@example.test");
    const { token } = await auth.verify("a@example.test", sent[0].code);
    const { accountId } = await auth.authenticate(token);
    assert.notEqual(accountId, await sha256Hex("a@example.test"));
    assert.match(accountId, /^[0-9a-f]{64}$/);
});

test("a wrong code is refused and five wrong guesses burn the code", async () => {
    const { auth, sent } = setup();
    await auth.requestCode("a@example.test");
    for (let i = 0; i < 5; i++) {
        const wrong = await auth.verify("a@example.test", "000000" === sent[0].code ? "111111" : "000000");
        assert.equal(wrong.ok, false);
        assert.equal(wrong.status, 401);
    }
    const afterBurn = await auth.verify("a@example.test", sent[0].code);
    assert.equal(afterBurn.ok, false);
});

test("a code expires after ten minutes and can only be used once", async () => {
    const { auth, sent, advance } = setup();
    await auth.requestCode("a@example.test");
    advance(11 * minute);
    assert.equal((await auth.verify("a@example.test", sent[0].code)).ok, false);

    await auth.requestCode("a@example.test");
    assert.equal((await auth.verify("a@example.test", sent[1].code)).ok, true);
    assert.equal((await auth.verify("a@example.test", sent[1].code)).ok, false);
});

test("signing out ends the session", async () => {
    const { auth, sent } = setup();
    await auth.requestCode("a@example.test");
    const { token } = await auth.verify("a@example.test", sent[0].code);
    await auth.signOut(token);
    assert.equal(await auth.authenticate(token), null);
});

test("deleting the account removes the account and every session", async () => {
    const { auth, sent, store } = setup();
    await auth.requestCode("a@example.test");
    const { token } = await auth.verify("a@example.test", sent[0].code);
    const session = await auth.authenticate(token);

    await auth.deleteAccount(session.accountId, session.email);

    assert.equal(await auth.authenticate(token), null);
    assert.equal(store.accounts.size, 0);
    assert.equal(store.sessions.size, 0);

    await auth.requestCode("a@example.test");
    const recreated = await auth.verify("a@example.test", sent[1].code);
    assert.equal((await auth.authenticate(recreated.token)).accountId, session.accountId);
});

test("when delivery fails the person can retry while the rate limit remains in force", async () => {
    const { auth, state, store } = setup();
    state.failMail = true;
    const result = await auth.requestCode("a@example.test");
    assert.equal(result.ok, false);
    assert.equal(result.status, 503);
    assert.equal(store.codes.has("a@example.test"), false);

    state.failMail = false;
    assert.equal((await auth.requestCode("a@example.test")).ok, true);
    assert.equal((await auth.requestCode("a@example.test")).ok, true);
    assert.equal((await auth.requestCode("a@example.test")).status, 429);
});

test("a session older than 180 days is no longer valid", async () => {
    const { auth, sent, advance } = setup();
    await auth.requestCode("a@example.test");
    const { token } = await auth.verify("a@example.test", sent[0].code);
    advance(181 * 24 * 60 * minute);
    assert.equal(await auth.authenticate(token), null);
});
