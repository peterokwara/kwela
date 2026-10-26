import { Agent, webSearchTool } from "@openai/agents";
import { z } from "zod";
import { CAREER_AGENT_MODEL } from "./agentModel.ts";

/**
 * What the pay researcher reports. Numbers are null when nothing reliable was found, never a guess.
 */
const payResearchSchema = z.object({
    found: z.boolean(),
    currency: z.string(),
    period: z.string(),
    low: z.number().nullable(),
    high: z.number().nullable(),
    sources: z.array(z.string()),
    note: z.string()
});

/**
 * Looks up what a role usually pays, so the career agent can answer "what should I ask for?" with evidence.
 */
const payResearchAgent = new Agent({
    name: "Kwela Pay Researcher",
    instructions: `Search the web for the typical pay range for the role described, in the country given (Kenya unless told otherwise). Prefer local sources such as BrighterMonday, MyJobMag, Glassdoor, PayScale and the employer's own listing. Report pay in the currency and period the sources use (usually KES per month). Give a realistic low and high, sources by site name, and one short note about how much to trust it, for example how few listings you found or that the figure is for a different seniority. If the exact title has little data, use the closest comparable roles and seniority (for example Head of Engineering, Engineering Manager, VP Engineering or CTO for a Deputy CTO) and say in the note which roles the figure comes from. If Kenyan data is thin, use East African or comparable remote-market data and say so, converting to KES and saying which rate you used. Set found to false only when nothing close exists, leave the numbers null and say why in the note. Never invent a number: every figure must come from something you found.`,
    tools: [webSearchTool({ searchContextSize: "medium" })],
    model: CAREER_AGENT_MODEL,
    outputType: payResearchSchema
});

/**
 * Tool exposed to the career agent for researching what a role pays.
 */
export const researchPay = payResearchAgent.asTool({
    toolName: "research_pay",
    toolDescription:
        "Search the web for the usual pay range for a role. Use when the person asks what pay to expect or ask for, or asks you to search online for it.",
    parameters: z.object({
        role: z.string().min(1).max(200),
        employer: z.string().max(200),
        country: z.string().max(100),
        seniority: z.string().max(100)
    }),
    customOutputExtractor: (result) => JSON.stringify(result.finalOutput)
});
