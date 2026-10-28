import type { ISupportMessage, ISupportMessageStore } from "../../models/services/ISupportMessageStore.ts";
import type { TesterSignupDatabase } from "./d1TesterSignupStoreService.ts";

/**
 * Keeps Support page messages in D1. Rate limits share the sign-in limiter table, which stores only opaque keys.
 */
export class D1SupportMessageStoreService implements ISupportMessageStore {
    private readonly database: TesterSignupDatabase;

    /**
     * Creates the D1 adapter for Support page messages.
     * @param database Cloudflare D1 binding.
     */
    constructor(database: TesterSignupDatabase) {
        this.database = database;
    }

    /** Saves a message. */
    async save(message: ISupportMessage): Promise<void> {
        await this.database
            .prepare("INSERT INTO support_messages (id, email, message, created_at) VALUES (?, ?, ?, ?)")
            .bind(message.id, message.email, message.message, message.createdAt)
            .run();
    }

    /** Atomically records a rate-limit hit without storing the source IP. Same statement and table as the sign-in limiter. */
    async hitRateLimit(key: string, now: number, windowMs: number, maxRequests: number): Promise<boolean> {
        const row = await this.database
            .prepare(
                "INSERT INTO login_rate_limits (limiter_key, window_start, request_count) VALUES (?, ?, 1) ON CONFLICT(limiter_key) DO UPDATE SET window_start = CASE WHEN login_rate_limits.window_start <= ? THEN excluded.window_start ELSE login_rate_limits.window_start END, request_count = CASE WHEN login_rate_limits.window_start <= ? THEN 1 ELSE login_rate_limits.request_count + 1 END RETURNING request_count"
            )
            .bind(key, now, now - windowMs, now - windowMs)
            .first<{ request_count: number }>();
        return Boolean(row && row.request_count <= maxRequests);
    }
}
