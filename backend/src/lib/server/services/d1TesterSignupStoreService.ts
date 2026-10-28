import type { ITesterSignup, ITesterSignupStore } from "../../models/services/ITesterSignupStore.ts";

/**
 * D1 methods used by the tester sign-up store.
 */
export interface TesterSignupDatabase {
    /**
     * Prepares a parameterized D1 statement.
     */
    prepare(query: string): {
        /**
         * Binds values and runs or reads the statement.
         */
        bind(...values: unknown[]): {
            first<T>(): Promise<T | null>;
            run(): Promise<unknown>;
        };
    };
}

/**
 * Keeps tester sign-ups in D1. Rate limits share the sign-in limiter table, which stores only opaque keys.
 */
export class D1TesterSignupStoreService implements ITesterSignupStore {
    private readonly database: TesterSignupDatabase;

    /**
     * Creates the D1 adapter for tester sign-ups.
     * @param database Cloudflare D1 binding.
     */
    constructor(database: TesterSignupDatabase) {
        this.database = database;
    }

    /** Saves a sign-up. The same email and phone again changes nothing, and the same email with another phone is kept as its own row, so nobody can overwrite someone else's details. */
    async save(signup: ITesterSignup): Promise<void> {
        await this.database
            .prepare(
                "INSERT INTO tester_signups (email, phone, consent_at, created_at) VALUES (?, ?, ?, ?) ON CONFLICT(email, phone) DO NOTHING"
            )
            .bind(signup.email, signup.phone, signup.consentAt, signup.createdAt)
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
