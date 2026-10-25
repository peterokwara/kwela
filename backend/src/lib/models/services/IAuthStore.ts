/**
 * One pending sign-in code. The code itself is never stored, only a keyed hash of it.
 */
export interface IStoredLoginCode {
    /**
     * Normalised email address the code was sent to.
     */
    email: string;
    /**
     * Keyed hash of the code.
     */
    codeHash: string;
    /**
     * Time after which the code stops working, in milliseconds since the epoch.
     */
    expiresAt: number;
    /**
     * Number of wrong guesses made against this code.
     */
    attempts: number;
    /**
     * Start of the current rate-limit window, in milliseconds since the epoch.
     */
    windowStart: number;
    /**
     * Number of codes requested in the current rate-limit window.
     */
    requestCount: number;
}

/**
 * Account created the first time an email address signs in. Its ID is an opaque random UUID.
 */
export interface IStoredAccount {
    /**
     * Stable, opaque account id.
     */
    id: string;
    /**
     * Normalised email address.
     */
    email: string;
    /**
     * Creation time in milliseconds since the epoch.
     */
    createdAt: number;
}

/**
 * Sign-in session. Only a hash of the bearer token is stored.
 */
export interface IStoredSession {
    /**
     * SHA-256 hash of the bearer token.
     */
    tokenHash: string;
    /**
     * Account the session belongs to.
     */
    accountId: string;
    /**
     * Creation time in milliseconds since the epoch.
     */
    createdAt: number;
}

/**
 * Session joined with its account.
 */
export interface IResolvedSession {
    /**
     * Account id.
     */
    accountId: string;
    /**
     * Account email address.
     */
    email: string;
    /**
     * Session creation time in milliseconds since the epoch.
     */
    createdAt: number;
}

/**
 * Port for the storage that email sign-in needs.
 */
export interface IAuthStore {
    /**
     * Reads the pending code for an email address.
     */
    getCode(email: string): Promise<IStoredLoginCode | null>;
    /**
     * Creates or replaces the pending code for an email address.
     */
    saveCode(code: IStoredLoginCode): Promise<void>;
    /**
     * Removes the pending code for an email address.
     */
    deleteCode(email: string): Promise<void>;
    /**
     * Atomically checks and consumes one code attempt. A valid code can be consumed only once.
     */
    checkAndConsumeCode(
        email: string,
        candidateHash: string,
        now: number,
        maxAttempts: number
    ): Promise<"consumed" | "invalid" | "unavailable">;
    /**
     * Atomically records a rate-limit hit and reports whether it remains within the limit.
     */
    hitRateLimit(key: string, now: number, windowMs: number, maxRequests: number): Promise<boolean>;
    /**
     * Creates the account when it does not exist yet.
     */
    findOrCreateAccount(email: string, proposedId: string, createdAt: number): Promise<IStoredAccount>;
    /**
     * Stores a new session.
     */
    createSession(session: IStoredSession): Promise<void>;
    /**
     * Finds a session and its account by token hash.
     */
    getSession(tokenHash: string): Promise<IResolvedSession | null>;
    /**
     * Removes one session.
     */
    deleteSession(tokenHash: string): Promise<void>;
    /**
     * Removes an account with all its sessions and pending codes.
     */
    deleteAccountData(accountId: string, email: string): Promise<void>;
}
