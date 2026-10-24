import { z } from "zod";

/**
 * Job information fields used by Careergo's application workflow.
 */
export interface ICareergoJob {
    /**
     * Position title.
     */
    title: string;
    /**
     * Employer name.
     */
    company: string;
    /**
     * Job location.
     */
    location: string;
    /**
     * Advertised salary or range.
     */
    salary: string;
    /**
     * Job listing text.
     */
    jobDescription: string;
    /**
     * Short role summary.
     */
    jobSummary: string;
    /**
     * Application deadline.
     */
    applicationDeadline: string;
    /**
     * Contact person named in the listing.
     */
    contactPerson: string;
    /**
     * Contact email named in the listing.
     */
    contactEmail: string;
    /**
     * Additional notes about the listing.
     */
    notes: string;
    /**
     * Source URL for the listing.
     */
    url: string;
    /**
     * Required qualifications and experience.
     */
    keyRequirements: string[];
    /**
     * Skills the employer prefers.
     */
    desiredSkills: string[];
    /**
     * Main responsibilities.
     */
    responsibilities: string[];
    /**
     * Terms useful for matching the CV to the listing.
     */
    atsKeywords: string[];
    /**
     * Interpersonal skills named in the listing.
     */
    softSkills: string[];
    /**
     * Role-specific technical skills named in the listing.
     */
    hardSkills: string[];
    /**
     * Industry-specific language in the listing.
     */
    industryTerms: string[];
}

/**
 * Validates job data against Careergo's job shape.
 */
export const careergoJobSchema: z.ZodType<ICareergoJob> = z.object({
    title: z.string(),
    company: z.string(),
    location: z.string(),
    salary: z.string(),
    jobDescription: z.string(),
    jobSummary: z.string(),
    applicationDeadline: z.string(),
    contactPerson: z.string(),
    contactEmail: z.string(),
    notes: z.string(),
    url: z.string(),
    keyRequirements: z.array(z.string()),
    desiredSkills: z.array(z.string()),
    responsibilities: z.array(z.string()),
    atsKeywords: z.array(z.string()),
    softSkills: z.array(z.string()),
    hardSkills: z.array(z.string()),
    industryTerms: z.array(z.string())
});
