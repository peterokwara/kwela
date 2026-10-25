import { z } from "zod";
import { applicationPackSchema } from "../careergo/IApplicationPack.ts";

/** Validates the compact conversation memory sent back by the Kwela app. */
export const conversationContextSchema = z.object({
    summary: z.string().max(6000),
    throughMessageId: z.string().min(1).max(200),
    currentApplication: applicationPackSchema.optional()
});
