/**
 * One message sent from the Support page.
 */
export interface ISupportMessage {
    /** Random id. */
    id: string;
    /** Lowercase email address to reply to. */
    email: string;
    /** What the person wrote. */
    message: string;
    /** When it was sent, in milliseconds since the epoch. */
    createdAt: number;
}

/**
 * Storage for Support page messages.
 */
export interface ISupportMessageStore {
    /** Saves a message. */
    save(message: ISupportMessage): Promise<void>;
    /** Atomically records a rate-limit hit and reports whether it remains within the limit. */
    hitRateLimit(key: string, now: number, windowMs: number, maxRequests: number): Promise<boolean>;
}
