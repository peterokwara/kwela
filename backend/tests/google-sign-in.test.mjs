import assert from "node:assert/strict";
import test from "node:test";
import { createGoogleTokenVerifier } from "../src/lib/server/services/googleIdToken.ts";
import { createAuthService } from "../src/lib/server/services/authService.ts";

const CLIENT_ID = "123-web.apps.googleusercontent.com";
const NOW = 1_800_000_000_000;

function b64url(bytes) {
    return Buffer.from(bytes).toString("base64url");
}

/** Makes a key pair like Google's, and signs ID tokens with it. */
async function makeSigner(kid = "key-1") {
    const pair = await crypto.subtle.generateKey({ name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256" }, true, ["sign", "verify"]);
    const jwk = { ...(await crypto.subtle.exportKey("jwk", pair.publicKey)), kid, alg: "RS256", use: "sig" };
    async function sign(claims, header = {}) {
        const head = b64url(new TextEncoder().encode(JSON.stringify({ alg: "RS256", typ: "JWT", kid, ...header })));
        const body = b64url(new TextEncoder().encode(JSON.stringify(claims)));
        const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", pair.privateKey, new TextEncoder().encode(`${head}.${body}`));
        return `${head}.${body}.${b64url(new Uint8Array(signature))}`;
    }
    return { jwk, sign };
}

const goodClaims = () => ({
    iss: "https://accounts.google.com", aud: CLIENT_ID, sub: "1234567890", email: "Amina@Example.com", email_verified: true,
    name: "Amina Wanjiru", picture: "https://example.test/a.png", iat: NOW / 1000 - 10, exp: NOW / 1000 + 3000
});

async function verifierFor(signer, extra = {}) {
    let fetches = 0;
    const verifier = createGoogleTokenVerifier({ clientId: CLIENT_ID, now: () => NOW, fetchKeys: async () => { fetches += 1; return { keys: [signer.jwk] }; }, ...extra });
    return { verify: verifier, fetchCount: () => fetches };
}

test("a real Google token gives the verified email and name", async () => {
    const signer = await makeSigner();
    const { verify } = await verifierFor(signer);
    assert.deepEqual(await verify(await signer.sign(goodClaims())), { email: "amina@example.com", name: "Amina Wanjiru" });
});

test("a token made for another app is refused", async () => {
    const signer = await makeSigner();
    const { verify } = await verifierFor(signer);
    assert.equal(await verify(await signer.sign({ ...goodClaims(), aud: "someone-else.apps.googleusercontent.com" })), null);
});

test("an expired token is refused", async () => {
    const signer = await makeSigner();
    const { verify } = await verifierFor(signer);
    assert.equal(await verify(await signer.sign({ ...goodClaims(), exp: NOW / 1000 - 120 })), null);
});

test("a token from the wrong issuer is refused", async () => {
    const signer = await makeSigner();
    const { verify } = await verifierFor(signer);
    assert.equal(await verify(await signer.sign({ ...goodClaims(), iss: "https://evil.example" })), null);
});

test("an email Google has not verified is refused", async () => {
    const signer = await makeSigner();
    const { verify } = await verifierFor(signer);
    assert.equal(await verify(await signer.sign({ ...goodClaims(), email_verified: false })), null);
});

test("a token signed with another key is refused", async () => {
    const real = await makeSigner();
    const forger = await makeSigner();
    const { verify } = await verifierFor(real);
    assert.equal(await verify(await forger.sign(goodClaims())), null);
});

test("a token that is not a token is refused, never thrown", async () => {
    const signer = await makeSigner();
    const { verify } = await verifierFor(signer);
    assert.equal(await verify("not.a.jwt"), null);
    assert.equal(await verify(""), null);
    assert.equal(await verify("a.b"), null);
});

test("an unsigned token with alg none is refused", async () => {
    const signer = await makeSigner();
    const { verify } = await verifierFor(signer);
    const head = b64url(new TextEncoder().encode(JSON.stringify({ alg: "none", typ: "JWT" })));
    const body = b64url(new TextEncoder().encode(JSON.stringify(goodClaims())));
    assert.equal(await verify(`${head}.${body}.`), null);
});

test("Google's keys are fetched once, then reused, and fetched again for a key it has not seen", async () => {
    const first = await makeSigner("key-1");
    const second = await makeSigner("key-2");
    let keys = [first.jwk];
    let fetches = 0;
    let clock = NOW;
    const verify = createGoogleTokenVerifier({ clientId: CLIENT_ID, now: () => clock, fetchKeys: async () => { fetches += 1; return { keys }; } });
    assert.ok(await verify(await first.sign(goodClaims())));
    assert.ok(await verify(await first.sign(goodClaims())));
    assert.equal(fetches, 1);
    keys = [first.jwk, second.jwk];
    clock = NOW + 61_000;
    assert.ok(await verify(await second.sign({ ...goodClaims(), iat: clock / 1000 - 10, exp: clock / 1000 + 3000 })));
    assert.equal(fetches, 2);
});

test("tokens with made-up key ids cannot make the server fetch Google's keys over and over", async () => {
    const real = await makeSigner("key-1");
    const forger = await makeSigner("made-up");
    let fetches = 0;
    const verify = createGoogleTokenVerifier({ clientId: CLIENT_ID, now: () => NOW, fetchKeys: async () => { fetches += 1; return { keys: [real.jwk] }; } });
    assert.ok(await verify(await real.sign(goodClaims())));
    for (let index = 0; index < 5; index += 1) assert.equal(await verify(await forger.sign(goodClaims())), null);
    assert.equal(fetches, 1);
});

test("sign-in with Google starts a session for the verified email", async () => {
    const calls = { accounts: [], sessions: [] };
    const store = {
        async hitRateLimit() { return true; },
        async findOrCreateAccount(email, id) { calls.accounts.push(email); return { id, email, createdAt: NOW }; },
        async createSession(session) { calls.sessions.push(session); }
    };
    const auth = createAuthService({ store, mailer: { async send() {} }, secret: "s".repeat(32), now: () => NOW, googleVerifier: async (token) => (token === "good" ? { email: "amina@example.com", name: "Amina" } : null) });

    const ok = await auth.signInWithGoogle("good", "1.2.3.4");
    assert.equal(ok.ok, true);
    assert.equal(ok.email, "amina@example.com");
    assert.equal(ok.name, "Amina");
    assert.match(ok.token, /^[A-Za-z0-9_-]{32,}$/);
    assert.deepEqual(calls.accounts, ["amina@example.com"]);
    assert.equal(calls.sessions.length, 1);

    const bad = await auth.signInWithGoogle("forged");
    assert.equal(bad.ok, false);
    assert.equal(bad.status, 401);
    assert.equal(calls.sessions.length, 1);
});

test("sign-in with Google is limited per connection", async () => {
    let hits = 0;
    const store = { async hitRateLimit() { hits += 1; return hits <= 2; }, async findOrCreateAccount(email, id) { return { id, email, createdAt: NOW }; }, async createSession() {} };
    const auth = createAuthService({ store, mailer: { async send() {} }, secret: "s".repeat(32), now: () => NOW, googleVerifier: async () => ({ email: "a@example.com", name: "" }) });
    assert.equal((await auth.signInWithGoogle("t", "9.9.9.9")).ok, true);
    assert.equal((await auth.signInWithGoogle("t", "9.9.9.9")).ok, true);
    const third = await auth.signInWithGoogle("t", "9.9.9.9");
    assert.equal(third.ok, false);
    assert.equal(third.status, 429);
});

test("without a Google client set up, Google sign-in says so instead of crashing", async () => {
    const store = { async hitRateLimit() { return true; } };
    const auth = createAuthService({ store, mailer: { async send() {} }, secret: "s".repeat(32), now: () => NOW });
    const result = await auth.signInWithGoogle("anything");
    assert.equal(result.ok, false);
    assert.equal(result.status, 503);
});
