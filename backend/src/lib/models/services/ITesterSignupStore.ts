/**
 * One person who asked to test Kwela.
 */
export interface ITesterSignup {
    /** Lowercase email address. */
    email: string;
    /** Phone number in international format, like +254712345678. */
    phone: string;
    /** When they agreed to be contacted, in milliseconds since the epoch. */
    consentAt: number;
    /** When they first signed up, in milliseconds since the epoch. */
    createdAt: number;
}

/**
 * Storage for tester sign-ups.
 */
export interface ITesterSignupStore {
    /**
     * Saves a sign-up. The same email and phone again changes nothing. The same email with another phone is kept as its own row, so a repeat sign-up can never overwrite someone else's details.
     */
    save(signup: ITesterSignup): Promise<void>;
    /**
     * Atomically records a rate-limit hit and reports whether it remains within the limit.
     */
    hitRateLimit(key: string, now: number, windowMs: number, maxRequests: number): Promise<boolean>;
}
