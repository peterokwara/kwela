import type { ITesterSignupStore } from "../../models/services/ITesterSignupStore.ts";

/**
 * Outcome of a sign-up: done, or a status code and message safe to show a person.
 */
export type SignupResult = { ok: true } | { ok: false; status: number; message: string };

const WINDOW_MS = 60 * 60 * 1000;
const MAX_SIGNUPS_PER_IP_WINDOW = 5;
const MAX_SIGNUPS_FROM_UNKNOWN_IP_WINDOW = 30;
const MAX_SIGNUPS_IN_TOTAL_WINDOW = 200;
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
const phoneMessage = "Enter a phone number we can reach you on, like 0712 345 678. Start with + for other countries.";

/** Tidies a phone number into international format.
 * @param raw Number as typed.
 * @returns The number like +254712345678, or null when it cannot be reached.
 */
function normalizePhone(raw: string): string | null {
    const text = raw.trim();
    // A plus is allowed only at the very start, and only digits and common separators after it.
    if (text.length === 0 || text.length > 32 || !/^\+?[\d\s().-]+$/.test(text)) return null;
    const digits = text.replace(/\D/g, "");
    let international: string;
    if (text.startsWith("+")) international = digits;
    else if (digits.startsWith("00")) international = digits.slice(2);
    else if (digits.startsWith("254")) international = digits;
    else if (digits.startsWith("0") && digits.length === 10) international = `254${digits.slice(1)}`;
    else if (digits.length === 9 && /^[17]/.test(digits)) international = `254${digits}`;
    else return null;
    // Kenyan mobile numbers are nine digits after 254 and start with 7 or 1.
    if (international.startsWith("254") && !/^254[17]\d{8}$/.test(international)) return null;
    return /^[1-9]\d{7,14}$/.test(international) ? `+${international}` : null;
}

/** Reads and tidies the email and phone number of a sign-up.
 * @param body Parsed request body.
 * @returns The cleaned values, or a message safe to show a person.
 */
export function readSignup(body: unknown): { ok: true; value: { email: string; phone: string } } | { ok: false; message: string } {
    const input = body && typeof body === "object" ? (body as Record<string, unknown>) : {};
    const email = typeof input.email === "string" ? input.email.trim().toLowerCase() : "";
    if (email.length > 254 || !emailPattern.test(email)) return { ok: false, message: "Enter a valid email address." };
    const phone = typeof input.phone === "string" ? normalizePhone(input.phone) : null;
    if (!phone) return { ok: false, message: phoneMessage };
    return { ok: true, value: { email, phone } };
}

/** Converts bytes to lowercase hex. */
function toHex(bytes: ArrayBuffer): string {
    return [...new Uint8Array(bytes)].map((byte) => byte.toString(16).padStart(2, "0")).join("");
}

/** Computes an opaque key so IP addresses are not stored in the rate-limit table. `all` is the shared backstop key. */
async function hashLimiterKey(secret: string, value: string): Promise<string> {
    const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
    return toHex(await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(`signup:ip:${value}`)));
}

/** Compares two secrets without leaking where they differ. */
async function sameSecret(a: string, b: string): Promise<boolean> {
    const [hashA, hashB] = await Promise.all([a, b].map(async (value) => new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value)))));
    let difference = 0;
    for (let index = 0; index < hashA.length; index += 1) difference |= hashA[index] ^ hashB[index];
    return difference === 0;
}

/**
 * Dependencies of the tester sign-up service.
 */
export interface ITesterSignupServiceOptions {
    /** Storage for sign-ups and rate limits. */
    store: ITesterSignupStore;
    /** Server secret that keys the rate-limit hashes. */
    secret: string;
    /** Secret the Kwela website sends with every sign-up. Without it the form stays closed. */
    siteToken?: string;
    /** Clock in milliseconds since the epoch. */
    now?: () => number;
}

/** Creates the service that records people who want to test Kwela.
 * @param options Store, secrets and clock.
 * @returns A function to submit one sign-up.
 */
export function createTesterSignupService(options: ITesterSignupServiceOptions) {
    const { store, secret, siteToken } = options;
    const now = options.now ?? Date.now;

    /** Checks the site token. Run before reading the request body, so strangers cost nothing. */
    async function check(token: string): Promise<SignupResult> {
        if (!siteToken) return { ok: false, status: 503, message: "Sign-ups are not open yet." };
        if (!(await sameSecret(token, siteToken))) return { ok: false, status: 403, message: "Not allowed." };
        return { ok: true };
    }

    return {
        check,
        /** Records one sign-up.
         * @param body Parsed request body: email, phone, consent and the hidden website field.
         * @param context Caller address and the site token from the request.
         * @returns Done, or a status and message safe to show a person.
         */
        async submit(body: unknown, context: { sourceIp?: string; token: string }): Promise<SignupResult> {
            const allowedToSubmit = await check(context.token);
            if (!allowedToSubmit.ok) return allowedToSubmit;
            const input = body && typeof body === "object" ? (body as Record<string, unknown>) : {};
            // Real people never see this field. Bots fill it in, so they are told it worked and nothing is kept.
            if (typeof input.website === "string" && input.website.length > 0) return { ok: true };
            const read = readSignup(input);
            if (!read.ok) return { ok: false, status: 400, message: read.message };
            if (input.consent !== true) return { ok: false, status: 400, message: "Tick the box so we know it is okay to contact you." };

            const time = now();
            const ip = context.sourceIp?.trim();
            const allowed = await store.hitRateLimit(
                await hashLimiterKey(secret, ip || "unknown"),
                time,
                WINDOW_MS,
                ip ? MAX_SIGNUPS_PER_IP_WINDOW : MAX_SIGNUPS_FROM_UNKNOWN_IP_WINDOW
            );
            // A backstop in case the per-address limit is dodged by sending many different addresses.
            const underTotal = await store.hitRateLimit(await hashLimiterKey(secret, "all"), time, WINDOW_MS, MAX_SIGNUPS_IN_TOTAL_WINDOW);
            if (!allowed || !underTotal) return { ok: false, status: 429, message: "Too many tries. Please wait a while and try again." };

            try {
                await store.save({ ...read.value, consentAt: time, createdAt: time });
            } catch {
                return { ok: false, status: 503, message: "Something went wrong on our side. Please try again in a moment." };
            }
            return { ok: true };
        }
    };
}
