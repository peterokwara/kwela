import { z } from "zod";

/**
 * Cover letter fields used by Careergo's document template.
 */
export interface ICareergoCoverLetter {
    /**
     * Heading shown at the top of the letter.
     */
    coverLetterTitle: string;
    /**
     * Candidate's full name.
     */
    fullName: string;
    /**
     * Position the candidate is applying for.
     */
    jobTitle: string;
    /**
     * Candidate's email address.
     */
    email: string;
    /**
     * Candidate's phone number.
     */
    phoneNumber: string;
    /**
     * Candidate's postal address, when provided.
     */
    address: string | null;
    /**
     * Employer's name.
     */
    companyName: string;
    /**
     * Hiring manager's name, when known.
     */
    hiringManagerName: string | null;
    /**
     * Candidate's LinkedIn URL, when provided.
     */
    linkedin: string | null;
    /**
     * Main letter content.
     */
    letterContent: string;
}

/**
 * Validates cover letter fields for Careergo's document template.
 */
export const careergoCoverLetterSchema: z.ZodType<ICareergoCoverLetter> = z.object({
    coverLetterTitle: z.string(),
    fullName: z.string(),
    jobTitle: z.string(),
    email: z.string(),
    phoneNumber: z.string(),
    address: z.string().nullable(),
    companyName: z.string(),
    hiringManagerName: z.string().nullable(),
    linkedin: z.string().nullable(),
    letterContent: z.string()
});
