import type { IApplicationPack } from "../careergo/IApplicationPack.ts";
import type { IChatAttachment } from "./IChatAttachment.ts";
import type { ICareergoResume } from "../careergo/ICareergoResume.ts";

/**
 * Message exchanged with the chat assistant and displayed in a thread.
 */
export interface IChatMessage {
    /**
     * Stable identifier used to render and store the message.
     */
    id: string;
    /**
     * Author of the message.
     */
    role: "user" | "assistant";
    /**
     * Text shown in the conversation.
     */
    content: string;
    /**
     * Files attached to a user's message.
     */
    files?: IChatAttachment[];
    /**
     * Structured application materials attached to an assistant reply.
     */
    application?: IApplicationPack;
    /**
     * Candidate profile extracted from a CV and saved for future chats.
     */
    candidateProfile?: ICareergoResume;
}
