import type {
    IAuthStore,
    IResolvedSession,
    IStoredAccount,
    IStoredLoginCode,
    IStoredSession
} from "../../models/services/IAuthStore.ts";

/**
 * D1 methods used by the sign-in store.
 */
export interface AuthDatabase {
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
 * Keeps sign-in codes, accounts and sessions in D1.
 */
export class D1AuthStoreService implements IAuthStore {
    /**
     * Creates the D1 adapter for sign-in storage.
     * @param database Cloudflare D1 binding.
     */
    constructor(private readonly database: AuthDatabase) {}

    /**
     * Reads the pending code for an email address.
     * @param email Normalised email address.
     * @returns The stored code, or null when none is pending.
     */
    async getCode(email: string): Promise<IStoredLoginCode | null> {
        const row = await this.database
            .prepare(
                "SELECT email, code_hash, expires_at, attempts, window_start, request_count FROM login_codes WHERE email = ?"
            )
            .bind(email)
            .first<{
                email: string;
                code_hash: string;
                expires_at: number;
                attempts: number;
                window_start: number;
                request_count: number;
            }>();
        return row
            ? {
                  email: row.email,
                  codeHash: row.code_hash,
                  expiresAt: row.expires_at,
                  attempts: row.attempts,
                  windowStart: row.window_start,
                  requestCount: row.request_count
              }
            : null;
    }

    /**
     * Creates or replaces the pending code for an email address.
     * @param code Code record to store.
     */
    async saveCode(code: IStoredLoginCode): Promise<void> {
        await this.database
            .prepare(
                "INSERT INTO login_codes (email, code_hash, expires_at, attempts, window_start, request_count) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT(email) DO UPDATE SET code_hash = excluded.code_hash, expires_at = excluded.expires_at, attempts = excluded.attempts, window_start = excluded.window_start, request_count = excluded.request_count"
            )
            .bind(code.email, code.codeHash, code.expiresAt, code.attempts, code.windowStart, code.requestCount)
            .run();
    }

    /**
     * Removes the pending code for an email address.
     * @param email Normalised email address.
     */
    async deleteCode(email: string): Promise<void> {
        await this.database.prepare("DELETE FROM login_codes WHERE email = ?").bind(email).run();
    }

    /** Atomically consumes a valid code or increments the attempt count for a wrong guess. */
    async checkAndConsumeCode(
        email: string,
        candidateHash: string,
        now: number,
        maxAttempts: number
    ): Promise<"consumed" | "invalid" | "unavailable"> {
        const row = await this.database
            .prepare(
                "UPDATE login_codes SET code_hash = CASE WHEN code_hash = ? THEN '' ELSE code_hash END, attempts = attempts + 1 WHERE email = ? AND expires_at > ? AND attempts < ? AND code_hash <> '' RETURNING code_hash"
            )
            .bind(candidateHash, email, now, maxAttempts)
            .first<{ code_hash: string }>();
        if (!row) return "unavailable";
        return row.code_hash === "" ? "consumed" : "invalid";
    }

    /** Atomically records a rate-limit hit without storing the source email or IP. */
    async hitRateLimit(key: string, now: number, windowMs: number, maxRequests: number): Promise<boolean> {
        const row = await this.database
            .prepare(
                "INSERT INTO login_rate_limits (limiter_key, window_start, request_count) VALUES (?, ?, 1) ON CONFLICT(limiter_key) DO UPDATE SET window_start = CASE WHEN login_rate_limits.window_start <= ? THEN excluded.window_start ELSE login_rate_limits.window_start END, request_count = CASE WHEN login_rate_limits.window_start <= ? THEN 1 ELSE login_rate_limits.request_count + 1 END RETURNING request_count"
            )
            .bind(key, now, now - windowMs, now - windowMs)
            .first<{ request_count: number }>();
        return Boolean(row && row.request_count <= maxRequests);
    }

    /**
     * Creates the account when it does not exist yet.
     * @param account Account to create.
     */
    async findOrCreateAccount(email: string, proposedId: string, createdAt: number): Promise<IStoredAccount> {
        await this.database
            .prepare("INSERT OR IGNORE INTO accounts (id, email, created_at) VALUES (?, ?, ?)")
            .bind(proposedId, email, createdAt)
            .run();
        const row = await this.database
            .prepare("SELECT id, email, created_at FROM accounts WHERE email = ?")
            .bind(email)
            .first<{ id: string; email: string; created_at: number }>();
        if (!row) throw new Error("Account could not be found after insert.");
        return { id: row.id, email: row.email, createdAt: row.created_at };
    }

    /**
     * Stores a new session.
     * @param session Session to store.
     */
    async createSession(session: IStoredSession): Promise<void> {
        await this.database
            .prepare("INSERT INTO sessions (token_hash, account_id, created_at) VALUES (?, ?, ?)")
            .bind(session.tokenHash, session.accountId, session.createdAt)
            .run();
    }

    /**
     * Finds a session and its account.
     * @param tokenHash SHA-256 hash of the bearer token.
     * @returns The session with its account, or null.
     */
    async getSession(tokenHash: string): Promise<IResolvedSession | null> {
        const row = await this.database
            .prepare(
                "SELECT accounts.id AS account_id, accounts.email AS email, sessions.created_at AS created_at FROM sessions JOIN accounts ON accounts.id = sessions.account_id WHERE sessions.token_hash = ?"
            )
            .bind(tokenHash)
            .first<{ account_id: string; email: string; created_at: number }>();
        return row ? { accountId: row.account_id, email: row.email, createdAt: row.created_at } : null;
    }

    /**
     * Removes one session.
     * @param tokenHash SHA-256 hash of the bearer token.
     */
    async deleteSession(tokenHash: string): Promise<void> {
        await this.database.prepare("DELETE FROM sessions WHERE token_hash = ?").bind(tokenHash).run();
    }

    /**
     * Removes an account with its sessions and pending codes.
     * @param accountId Account id.
     * @param email Account email address.
     */
    async deleteAccountData(accountId: string, email: string): Promise<void> {
        await this.database.prepare("DELETE FROM sessions WHERE account_id = ?").bind(accountId).run();
        await this.database.prepare("DELETE FROM login_codes WHERE email = ?").bind(email).run();
        await this.database.prepare("DELETE FROM accounts WHERE id = ?").bind(accountId).run();
    }
}
