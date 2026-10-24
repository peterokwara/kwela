import { Agent, webSearchTool } from "@openai/agents";
import { z } from "zod";
import { jobResearchResultSchema } from "../../../models/careergo/IJobResearchResult.ts";
import { CAREER_AGENT_MODEL } from "./agentModel.ts";

/**
 * Finds a job listing and maps its details into Careergo's job fields.
 */
const jobResearchAgent = new Agent({
    name: "Kwela Job Page Researcher",
    instructions: `Research the job posting at the URL supplied by the user. Use web search to find the employer's listing and extract its details into Careergo's Job fields. Preserve the listing text in jobDescription and summarize the role in jobSummary. Extract visible application questions into applicationQuestions. Prefer the employer's own listing. If the listing cannot be found, say so in job.notes and leave unavailable fields empty. Keep the source URL in job.url.`,
    tools: [webSearchTool({ searchContextSize: "high" })],
    model: CAREER_AGENT_MODEL,
    outputType: jobResearchResultSchema
});

/**
 * Tool exposed to the career agent for researching a job link.
 */
export const researchJobPage = jobResearchAgent.asTool({
    toolName: "research_job_page",
    toolDescription:
        "Read and summarize the job posting from a URL the user shared. Use before tailoring when the job details are not already in the chat.",
    parameters: z.object({ url: z.string().min(1).max(2000) }),
    customOutputExtractor: (result) => JSON.stringify(result.finalOutput)
});
