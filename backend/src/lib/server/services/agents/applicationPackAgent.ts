import { Agent } from "@openai/agents";
import { z } from "zod";
import { applicationPackSchema } from "../../../models/careergo/IApplicationPack.ts";
import { CAREER_AGENT_MODEL } from "./agentModel.ts";

/**
 * Drafts Careergo-shaped application materials for one job.
 */
const applicationPackAgent = new Agent({
    name: "Kwela Application Pack Drafter",
    instructions: `You prepare a job application pack. The input contains a candidate profile, job information, requested documents, optional application questions, user preferences, and sometimes a current application pack. Map the role into Careergo's Job fields: title, company, location, salary, jobDescription, jobSummary, applicationDeadline, contactPerson, contactEmail, notes, url, keyRequirements, desiredSkills, responsibilities, atsKeywords, softSkills, hardSkills, and industryTerms. Keep jobDescription as the available listing text. Tailor the CV by emphasizing relevant experience and skills. Fill every Careergo-compatible resume field. Use empty strings and empty arrays for fields that do not apply. Write the cover letter using the Careergo fields. Draft each application answer from details in candidateProfile and job information; keep the candidate's first-person voice. When a current application pack is supplied and the user asks for a change, keep its existing details and change only what the user requested. For requests to move a section or work bullet, update resume.sectionOrder or the matching employment achievements array in the requested direction. Preserve sectionOrder from the current pack unless the user requests a new order. Return the job, documents, and answers in the structured output.`,
    model: CAREER_AGENT_MODEL,
    outputType: applicationPackSchema
});

/**
 * Tool exposed to the career agent for preparing a job application pack.
 */
export const draftApplicationPack = applicationPackAgent.asTool({
    toolName: "draft_application_pack",
    toolDescription:
        "Draft the tailored CV, cover letter, and application answers after the candidate profile and job description are available.",
    parameters: z.object({
        candidateProfile: z.string().min(1).max(30000),
        currentApplication: z.string().max(30000).default(""),
        jobDescription: z.string().min(1).max(18000),
        jobUrl: z.string().max(2000),
        requestedMaterials: z
            .array(z.enum(["tailored_cv", "cover_letter", "application_answers"]))
            .min(1)
            .max(3),
        applicationQuestions: z.array(z.string().max(2000)).max(20),
        preferences: z.string().max(3000)
    }),
    customOutputExtractor: (result) => JSON.stringify(result.finalOutput)
});
