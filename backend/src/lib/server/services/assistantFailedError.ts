/**
 * Raised when the agent could not produce a reply after its attempts. It is never turned into a pretend chat message.
 */
export class AssistantFailedError extends Error {
    /**
     * Whether the agent ran out of time, as opposed to failing outright.
     */
    readonly timedOut: boolean;

    /**
     * Creates the error with a message safe to log.
     * @param timedOut True when the agent ran out of time.
     */
    constructor(timedOut = false) {
        super("Kwela could not finish this reply.");
        this.name = "AssistantFailedError";
        this.timedOut = timedOut;
    }
}
