import { z } from "zod";
import { careergoCoverLetterSchema } from "./ICareergoCoverLetter.ts";
import { careergoJobSchema } from "./ICareergoJob.ts";
import { careergoResumeSchema } from "./ICareergoResume.ts";
import type { IApplicationAnswer } from "./IApplicationAnswer.ts";
import type { ICareergoCoverLetter } from "./ICareergoCoverLetter.ts";
import type { ICareergoJob } from "./ICareergoJob.ts";
import type { ICareergoResume } from "./ICareergoResume.ts";

/**
 * Careergo-shaped materials prepared for one job.
 */
export interface IApplicationPack {
    /**
     * Structured listing details for the target role.
     */
    job: ICareergoJob;
    /**
     * CV content prepared for the target role.
     */
    resume: ICareergoResume;
    /**
     * Cover letter content prepared for the target role.
     */
    coverLetter: ICareergoCoverLetter;
    /**
     * Draft responses to the employer's application questions.
     */
    applicationAnswers: IApplicationAnswer[];
}

/**
 * Validates a complete job application pack.
 */
export const applicationPackSchema: z.ZodType<IApplicationPack> = z.object({
    job: careergoJobSchema,
    resume: careergoResumeSchema,
    coverLetter: careergoCoverLetterSchema,
    applicationAnswers: z.array(z.object({ question: z.string(), answer: z.string() }))
});
