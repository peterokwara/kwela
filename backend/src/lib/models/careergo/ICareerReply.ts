import { z } from "zod";
import { shapeBubbles } from "./bubbles.ts";
import { reviewResume, type IReviewFinding } from "./review.ts";
import { applicationPackSchema } from "./IApplicationPack.ts";
import type { IApplicationPack } from "./IApplicationPack.ts";
import { careergoResumeSchema } from "./ICareergoResume.ts";
import type { ICareergoResume } from "./ICareergoResume.ts";

/**
 * Structured response sent by the career assistant.
 */
export interface ICareerReply {
    /**
     * Indicates whether the response contains chat text or an application pack.
     */
    type: "message" | "application_pack";
    /**
     * Short explanation or follow-up for the chat thread.
     */
    message: string;
    /**
     * Draft materials when the assistant prepared an application pack.
     */
    applicationPack: IApplicationPack | null;
    /**
     * Reusable CV profile extracted during this turn, when available.
     */
    candidateProfile: ICareergoResume | null;
    /**
     * Things worth checking with the person before they send the CV, found by fixed rules. Empty when there is nothing to ask.
     */
    review: IReviewFinding[];
}

/**
 * Validates the career assistant's structured response.
 */
export const careerReplySchema = z.object({
    type: z.enum(["message", "application_pack"]),
    message: z.string(),
    applicationPack: applicationPackSchema.nullable().optional(),
    candidateProfile: careergoResumeSchema.nullable().optional()
});

/** Fills in empty fields the model left out, so every reply has the same shape.
 * @param reply Reply as validated, where empty fields may be missing.
 * @returns A complete reply with null for each empty field.
 */
export function normalizeCareerReply(reply: z.infer<typeof careerReplySchema>): ICareerReply {
    return {
        type: reply.type,
        message: shapeBubbles(reply.message),
        applicationPack: reply.applicationPack ?? null,
        candidateProfile: reply.candidateProfile ?? null,
        review: reply.applicationPack ? reviewResume(reply.applicationPack.resume) : []
    };
}
