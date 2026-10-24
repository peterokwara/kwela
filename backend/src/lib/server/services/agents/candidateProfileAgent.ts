import { Agent } from "@openai/agents";
import { z } from "zod";
import { careergoResumeSchema } from "../../../models/careergo/ICareergoResume.ts";
import { CAREER_AGENT_MODEL } from "./agentModel.ts";

/**
 * Extracts candidate details from an uploaded CV into Careergo's resume shape.
 */
const candidateProfileAgent = new Agent({
    name: "Kwela CV Extractor",
    instructions: `Extract a candidate profile from the supplied CV text into Careergo's resume fields. Preserve names, dates, titles, employers, education, skills, projects, and other CV sections in their matching fields. Keep concise summaries concise.`,
    model: CAREER_AGENT_MODEL,
    outputType: careergoResumeSchema
});

/**
 * Tool exposed to the career agent for converting CV text into structured fields.
 */
export const extractCandidateProfile = candidateProfileAgent.asTool({
    toolName: "extract_candidate_profile",
    toolDescription: "Convert uploaded CV text into Careergo resume fields before preparing application materials.",
    parameters: z.object({ candidateCv: z.string().min(1).max(30000) }),
    customOutputExtractor: (result) => JSON.stringify(result.finalOutput)
});
