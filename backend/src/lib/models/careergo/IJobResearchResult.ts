import { z } from "zod";
import { careergoJobSchema } from "./ICareergoJob.ts";
import type { ICareergoJob } from "./ICareergoJob.ts";

/**
 * Job listing details and questions found during job-page research.
 */
export interface IJobResearchResult {
    /**
     * Job data mapped to Careergo's job fields.
     */
    job: ICareergoJob;
    /**
     * Questions visible in the job application's public listing.
     */
    applicationQuestions: string[];
}

/**
 * Validates the result returned by job-page research.
 */
export const jobResearchResultSchema: z.ZodType<IJobResearchResult> = z.object({
    job: careergoJobSchema,
    applicationQuestions: z.array(z.string())
});
