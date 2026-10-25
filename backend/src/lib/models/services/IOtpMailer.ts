/**
 * Port that delivers a sign-in code to a person's inbox.
 */
export interface IOtpMailer {
    /**
     * Sends a sign-in code. Rejects when the message could not be sent.
     * @param email Normalised recipient address.
     * @param code Six-digit code to deliver.
     */
    send(email: string, code: string): Promise<void>;
}
