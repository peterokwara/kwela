import type { ISupportMessageStore } from "../../models/services/ISupportMessageStore.ts";
import { hashLimiterKey, readEmail, sameSecret, type SignupResult } from "./testerSignupService.ts";

const WINDOW_MS = 60 * 60 * 1000;
const MAX_MESSAGES_PER_IP_WINDOW = 5;
const MAX_MESSAGES_FROM_UNKNOWN_IP_WINDOW = 30;
const MAX_MESSAGES_IN_TOTAL_WINDOW = 100;
const MIN_MESSAGE_CHARS = 10;
const MAX_MESSAGE_CHARS = 2000;

/** Reads and tidies a Support page message.
 * @param body Parsed request body.
 * @returns The cleaned email and message, or a message safe to show a person.
 */
export function readSupportMessage(body: unknown): { ok: true; value: { email: string; message: string } } | { ok: false; message: string } {
    const input = body && typeof body === "object" ? (body as Record<string, unknown>) : {};
    const email = readEmail(input.email);
    if (!email) return { ok: false, message: "Enter a valid email address so we can reply." };
    // Control characters are dropped, line breaks stay.
    const message = typeof input.message === "string" ? input.message.replace(/[\u0000-\u0009\u000b\u000c\u000e-\u001f\u007f]/g, "").trim() : "";
    if (message.length < MIN_MESSAGE_CHARS || message.length > MAX_MESSAGE_CHARS) return { ok: false, message: `Write a message of ${MIN_MESSAGE_CHARS} to ${MAX_MESSAGE_CHARS} characters.` };
    return { ok: true, value: { email, message } };
}

/**
 * Dependencies of the Support message service.
 */
export interface ISupportMessageServiceOptions {
    /** Storage for messages and rate limits. */
    store: ISupportMessageStore;
    /** Server secret that keys the rate-limit hashes. */
    secret: string;
    /** Secret the Kwela website sends with every message. Without it the form stays closed. */
    siteToken?: string;
    /** Clock in milliseconds since the epoch. */
    now?: () => number;
    /** Makes a message id. */
    newId?: () => string;
}

/** Creates the service that records Support page messages.
 * @param options Store, secrets and clock.
 * @returns Functions to check the site token and to submit one message.
 */
export function createSupportMessageService(options: ISupportMessageServiceOptions) {
    const { store, secret, siteToken } = options;
    const now = options.now ?? Date.now;
    const newId = options.newId ?? (() => crypto.randomUUID());

    /** Checks the site token. Run before reading the request body, so strangers cost nothing. */
    async function check(token: string): Promise<SignupResult> {
        if (!siteToken) return { ok: false, status: 503, message: "The contact form is not open yet." };
        if (!(await sameSecret(token, siteToken))) return { ok: false, status: 403, message: "Not allowed." };
        return { ok: true };
    }

    return {
        check,
        /** Records one message.
         * @param body Parsed request body: email, message and the hidden website field.
         * @param context Caller address and the site token from the request.
         * @returns Done, or a status and message safe to show a person.
         */
        async submit(body: unknown, context: { sourceIp?: string; token: string }): Promise<SignupResult> {
            const allowedToSubmit = await check(context.token);
            if (!allowedToSubmit.ok) return allowedToSubmit;
            const input = body && typeof body === "object" ? (body as Record<string, unknown>) : {};
            // Real people never see this field. Bots fill it in, so they are told it worked and nothing is kept.
            if (typeof input.website === "string" && input.website.length > 0) return { ok: true };
            const read = readSupportMessage(input);
            if (!read.ok) return { ok: false, status: 400, message: read.message };

            const time = now();
            const ip = context.sourceIp?.trim();
            const allowed = await store.hitRateLimit(
                await hashLimiterKey(secret, "contact", ip || "unknown"),
                time,
                WINDOW_MS,
                ip ? MAX_MESSAGES_PER_IP_WINDOW : MAX_MESSAGES_FROM_UNKNOWN_IP_WINDOW
            );
            // A backstop in case the per-address limit is dodged by sending many different addresses.
            const underTotal = await store.hitRateLimit(await hashLimiterKey(secret, "contact", "all"), time, WINDOW_MS, MAX_MESSAGES_IN_TOTAL_WINDOW);
            if (!allowed || !underTotal) return { ok: false, status: 429, message: "Too many tries. Please wait a while and try again." };

            try {
                await store.save({ id: newId(), ...read.value, createdAt: time });
            } catch {
                return { ok: false, status: 503, message: "Something went wrong on our side. Please try again in a moment." };
            }
            return { ok: true };
        }
    };
}
