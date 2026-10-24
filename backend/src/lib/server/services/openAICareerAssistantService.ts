import { run, setDefaultOpenAIKey, type AgentInputItem } from "@openai/agents";
import { normalizeCareerReply, type ICareerReply } from "../../models/careergo/ICareerReply.ts";
import type { IChatMessage } from "../../models/chat/IChatMessage.ts";
import type { ICareerAssistant } from "../../models/services/ICareerAssistant.ts";
import { careerAgent } from "./agents/careerAgent.ts";
import { chatMessageToAgentInput } from "./chatMessageInputService.ts";

/**
 * Callback that receives the token counts of each model attempt.
 */
type UsageReporter = (usage: { inputTokens: number; outputTokens: number } | null) => void;

/**
 * Number of times the agent runs before Kwela gives up on a reply.
 */
const MAX_ATTEMPTS = 2;

/**
 * Longest a single agent attempt may run, in milliseconds.
 */
const ATTEMPT_TIMEOUT_MS = 70_000;

/** Runs the career agent to completion and retries when the model returns an invalid reply.
 * @param input Conversation items sent to the agent.
 * @returns A valid reply, or a polite retry message after repeated failures.
 */
async function runWithRetry(input: AgentInputItem[], onUsage?: UsageReporter): Promise<ICareerReply> {
    for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
        try {
            const result = await run(careerAgent, input, {
                maxTurns: 8,
                signal: AbortSignal.timeout(ATTEMPT_TIMEOUT_MS)
            });
            onUsage?.(result.state.usage);
            if (result.finalOutput) return normalizeCareerReply(result.finalOutput);
        } catch (error) {
            console.error(`Kwela agent attempt ${attempt} failed:`, error);
            onUsage?.(null);
            if (error instanceof Error && error.name === "TimeoutError") break;
        }
    }
    return {
        type: "message",
        message: "Kwela hit a snag while preparing that. Your files are safe. Please send your message again.",
        applicationPack: null,
        candidateProfile: null
    };
}

/** Creates the OpenAI-backed adapter for the career-assistant contract.
 * @param apiKey OpenAI credential configured for this runtime.
 * @returns An assistant implementation that returns streamed replies.
 */
export function createOpenAICareerAssistantService(apiKey: string): ICareerAssistant {
    setDefaultOpenAIKey(apiKey);
    return {
        async respond(messages: readonly IChatMessage[], onUsage?: UsageReporter) {
            const input: AgentInputItem[] = [];
            for (const message of messages.slice(-20)) input.push(await chatMessageToAgentInput(message));
            const reply = await runWithRetry(input, onUsage);
            const body = new TextEncoder().encode(JSON.stringify(reply));
            return new ReadableStream<Uint8Array>({
                start(controller) {
                    controller.enqueue(body);
                    controller.close();
                }
            });
        }
    };
}
