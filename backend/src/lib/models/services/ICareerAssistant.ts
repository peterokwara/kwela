import type { IChatMessage } from "../chat/IChatMessage.ts";
import type { IConversationContext } from "../chat/IConversationContext.ts";

/**
 * Contract for producing a streamed career-assistant reply.
 */
export interface ICareerAssistant {
    /** Responds to recent chat messages with a stream of reply text.
     * @param messages Recent conversation messages.
     * @param onUsage Called once per model attempt, before the returned promise resolves, with its token counts, or null when the attempt failed.
     * @returns A stream containing the assistant's response.
     */
    respond(
        messages: readonly IChatMessage[],
        onUsage?: (usage: { inputTokens: number; outputTokens: number } | null) => void,
        context?: IConversationContext | null
    ): Promise<ReadableStream<Uint8Array>>;
}
