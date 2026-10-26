import type { IAuthStore, IResolvedSession } from "../../models/services/IAuthStore.ts";
import type { IOtpMailer } from "../../models/services/IOtpMailer.ts";
import type { IGoogleIdentity } from "./googleIdToken.ts";

/**
 * Outcome of an auth action: success data, or a status code and message safe to show a person.
 */
export type AuthFailure = { ok: false; status: number; message: string };

const CODE_TTL_MS = 10 * 60 * 1000;
const MAX_ATTEMPTS = 5;
const REQUEST_WINDOW_MS = 15 * 60 * 1000;
const MAX_REQUESTS_PER_WINDOW = 3;
const MAX_REQUESTS_PER_IP_WINDOW = 10;
const MAX_GOOGLE_SIGN_INS_PER_IP_WINDOW = 30;
const SESSION_TTL_MS = 180 * 24 * 60 * 60 * 1000;
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/**
 * Dependencies of the sign-in service.
 */
export interface IAuthServiceOptions {
    /**
     * Storage for codes, accounts and sessions.
     */
    store: IAuthStore;
    /**
     * Delivers codes by email.
     */
    mailer: IOtpMailer;
    /**
     * Server secret that keys the code hashes.
     */
    secret: string;
    /**
     * Fixed six-digit code for an allow-listed private test. Omit when email delivery is enabled.
     */
    testCode?: string;
    /**
     * Checks a Google ID token. Omit when Google sign-in is not set up.
     */
    googleVerifier?: (idToken: string) => Promise<IGoogleIdentity | null>;
    /**
     * Clock in milliseconds since the epoch.
     */
    now?: () => number;
}

/** Converts bytes to lowercase hex.
 * @param bytes Bytes to encode.
 * @returns Hex string.
 */
function toHex(bytes: ArrayBuffer): string {
    return [...new Uint8Array(bytes)].map((byte) => byte.toString(16).padStart(2, "0")).join("");
}

/** Hashes text with SHA-256.
 * @param value Text to hash.
 * @returns Hex digest.
 */
export async function sha256Hex(value: string): Promise<string> {
    return toHex(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value)));
}

/** Computes a keyed hash of a sign-in code.
 * @param secret Server secret.
 * @param email Normalised email address.
 * @param code Six-digit code.
 * @returns Hex HMAC-SHA-256 digest.
 */
async function hashCode(secret: string, email: string, code: string): Promise<string> {
    const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
    return toHex(await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(`${email}:${code}`)));
}

/** Computes a stable opaque account id so deleting and recreating an account cannot reset usage caps. */
async function accountIdForEmail(secret: string, email: string): Promise<string> {
    const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
    return toHex(await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(`account:${email}`)));
}

/** Computes an opaque key for a limiter so email addresses and IPs are not stored in the rate-limit table. */
async function hashLimiterKey(secret: string, kind: "email" | "ip", value: string): Promise<string> {
    const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
    return toHex(await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(`${kind}:${value}`)));
}

/** Creates a uniformly random six-digit code.
 * @returns Code padded with leading zeros.
 */
function randomCode(): string {
    const value = new Uint32Array(1);
    do crypto.getRandomValues(value);
    while (value[0] >= 4_294_000_000);
    return String(value[0] % 1_000_000).padStart(6, "0");
}

/** Creates a random URL-safe bearer token.
 * @returns A 43-character token.
 */
function randomToken(): string {
    const bytes = crypto.getRandomValues(new Uint8Array(32));
    return btoa(String.fromCharCode(...bytes)).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

/** Normalises an email address for lookups.
 * @param email Address as typed.
 * @returns Trimmed lowercase address.
 */
export function normalizeEmail(email: string): string {
    return email.trim().toLowerCase();
}

/** Compares two strings without leaking where they differ.
 * @param a First string.
 * @param b Second string.
 * @returns True when equal.
 */
/** Creates the email sign-in service.
 * @param options Store, mailer, secret and clock.
 * @returns Functions to request a code, verify it, read a session, sign out and delete an account.
 */
export function createAuthService(options: IAuthServiceOptions) {
    const { store, mailer, secret } = options;
    const testCode = options.testCode?.trim() || undefined;
    if (testCode && !/^\d{6}$/.test(testCode)) throw new Error("The private test sign-in code must contain six digits.");
    const now = options.now ?? Date.now;

    return {
        /** Emails a six-digit code to the address.
         * @param rawEmail Address as typed.
         * @returns Success, or a failure with a status and message.
         */
        async requestCode(rawEmail: string, sourceIp?: string): Promise<{ ok: true; testCode?: string } | AuthFailure> {
            const email = normalizeEmail(rawEmail);
            if (email.length > 254 || !emailPattern.test(email)) {
                return { ok: false, status: 400, message: "Enter a valid email address." };
            }
            const time = now();
            const emailAllowed = await store.hitRateLimit(
                await hashLimiterKey(secret, "email", email),
                time,
                REQUEST_WINDOW_MS,
                MAX_REQUESTS_PER_WINDOW
            );
            if (!emailAllowed) {
                return { ok: false, status: 429, message: "Too many codes requested. Wait a few minutes and try again." };
            }
            const normalizedIp = sourceIp?.trim();
            if (normalizedIp) {
                const ipAllowed = await store.hitRateLimit(
                    await hashLimiterKey(secret, "ip", normalizedIp),
                    time,
                    REQUEST_WINDOW_MS,
                    MAX_REQUESTS_PER_IP_WINDOW
                );
                if (!ipAllowed) {
                    return { ok: false, status: 429, message: "Too many codes requested from this connection. Wait a few minutes and try again." };
                }
            }
            const code = testCode ?? randomCode();
            await store.saveCode({
                email,
                codeHash: await hashCode(secret, email, code),
                expiresAt: testCode ? Number.MAX_SAFE_INTEGER : time + CODE_TTL_MS,
                attempts: 0,
                windowStart: time,
                requestCount: 1
            });
            try {
                await mailer.send(email, code);
            } catch (cause) {
                console.error("Kwela sign-in email failed:", cause);
                await store.deleteCode(email);
                return { ok: false, status: 503, message: "Kwela couldn’t send the code right now. Please try again in a moment." };
            }
            return { ok: true, ...(testCode ? { testCode } : {}) };
        },

        /** Checks a code and, when right, starts a session.
         * @param rawEmail Address the code was sent to.
         * @param rawCode Code as typed.
         * @returns A bearer token and the address, or a failure.
         */
        async verify(rawEmail: string, rawCode: string): Promise<{ ok: true; token: string; email: string } | AuthFailure> {
            const email = normalizeEmail(rawEmail);
            const code = rawCode.trim();
            const time = now();
            const candidateHash = await hashCode(secret, email, code);
            const result = await store.checkAndConsumeCode(email, candidateHash, time, MAX_ATTEMPTS);
            if (result !== "consumed") {
                if (result === "unavailable") {
                    return { ok: false, status: 401, message: "That code has expired or is no longer valid. Request a new one." };
                }
                return { ok: false, status: 401, message: "That code isn’t right. Check it and try again." };
            }
            const account = await store.findOrCreateAccount(email, await accountIdForEmail(secret, email), time);
            const token = randomToken();
            await store.createSession({ tokenHash: await sha256Hex(token), accountId: account.id, createdAt: time });
            return { ok: true, token, email };
        },

        /** Signs a person in with a Google ID token from the phone. Google has already checked who they are, so no code is needed.
         * @param idToken Token the Google sign-in sheet gave the app.
         * @param sourceIp Address of the caller, for rate limiting.
         * @returns A bearer token, the verified address and name, or a failure.
         */
        async signInWithGoogle(idToken: string, sourceIp?: string): Promise<{ ok: true; token: string; email: string; name: string } | AuthFailure> {
            if (!options.googleVerifier) return { ok: false, status: 503, message: "Google sign-in isn’t available right now. Use your email instead." };
            const time = now();
            const ip = sourceIp?.trim();
            if (ip && !(await store.hitRateLimit(await hashLimiterKey(secret, "ip", `google:${ip}`), time, REQUEST_WINDOW_MS, MAX_GOOGLE_SIGN_INS_PER_IP_WINDOW))) {
                return { ok: false, status: 429, message: "Too many sign-in attempts from this connection. Wait a few minutes and try again." };
            }
            const identity = await options.googleVerifier(idToken);
            if (!identity) return { ok: false, status: 401, message: "Google sign-in didn’t work. Please try again." };
            const account = await store.findOrCreateAccount(identity.email, await accountIdForEmail(secret, identity.email), time);
            const token = randomToken();
            await store.createSession({ tokenHash: await sha256Hex(token), accountId: account.id, createdAt: time });
            return { ok: true, token, email: identity.email, name: identity.name };
        },

        /** Finds the signed-in person for a bearer token.
         * @param token Bearer token from the app.
         * @returns The account, or null when the token is unknown or too old.
         */
        async authenticate(token: string): Promise<IResolvedSession | null> {
            const session = await store.getSession(await sha256Hex(token));
            if (!session || now() - session.createdAt > SESSION_TTL_MS) return null;
            return session;
        },

        /** Ends one session.
         * @param token Bearer token to revoke.
         */
        async signOut(token: string): Promise<void> {
            await store.deleteSession(await sha256Hex(token));
        },

        /** Removes an account, its sessions and its pending codes.
         * @param accountId Account to remove.
         * @param email Account email address.
         */
        async deleteAccount(accountId: string, email: string): Promise<void> {
            await store.deleteAccountData(accountId, email);
        }
    };
}

export type AuthService = ReturnType<typeof createAuthService>;
