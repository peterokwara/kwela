import { run, setDefaultOpenAIKey, type AgentInputItem } from "@openai/agents";
import type { z } from "zod";
import { careerReplySchema, normalizeCareerReply, type ICareerReply } from "../../models/careergo/ICareerReply.ts";
import type { IChatMessage } from "../../models/chat/IChatMessage.ts";
import type { IConversationContext } from "../../models/chat/IConversationContext.ts";
import type { ICareerAssistant } from "../../models/services/ICareerAssistant.ts";
import { careerAgent } from "./agents/careerAgent.ts";
import { chatMessageToAgentInput } from "./chatMessageInputService.ts";
import { compactConversationContext } from "./conversationContextService.ts";

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

/**
 * What one run of the agent produces: its structured answer (or none) and the tokens it used.
 */
export interface IAgentRunResult {
    /**
     * The agent's structured reply, or null when it produced nothing usable.
     */
    output: z.infer<typeof careerReplySchema> | null;
    /**
     * Token counts for the run, when known.
     */
    usage: { inputTokens: number; outputTokens: number } | null;
}

/**
 * Runs the agent once. Swappable so the retry rules can be tested without calling OpenAI.
 */
export type AgentRunner = (input: AgentInputItem[]) => Promise<IAgentRunResult>;

/**
 * Raised when the agent could not produce a reply after its attempts. It is never turned into a pretend chat message.
 */
export class AssistantFailedError extends Error {
    /**
     * Creates the error with a message safe to log.
     */
    constructor() {
        super("Kwela could not finish this reply.");
        this.name = "AssistantFailedError";
    }
}

/** Runs the career agent once with a time limit.
 * @param input Conversation items sent to the agent.
 * @returns The agent's structured output and usage.
 */
const runCareerAgent: AgentRunner = async (input) => {
    const result = await run(careerAgent, input, { maxTurns: 8, signal: AbortSignal.timeout(ATTEMPT_TIMEOUT_MS) });
    return { output: result.finalOutput ?? null, usage: result.state.usage };
};

/** Runs the agent and retries once when it fails or returns nothing usable.
 * @param input Conversation items sent to the agent.
 * @param runner Runs the agent once.
 * @param onUsage Receives the tokens used by each attempt, or null when an attempt failed.
 * @returns A valid reply.
 * @throws AssistantFailedError when every attempt fails.
 */
async function runWithRetry(input: AgentInputItem[], runner: AgentRunner, onUsage?: UsageReporter): Promise<ICareerReply> {
    for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
        try {
            const result = await runner(input);
            onUsage?.(result.usage);
            if (result.output) return normalizeCareerReply(result.output);
            console.error(`Kwela agent attempt ${attempt} returned no usable reply.`);
        } catch (error) {
            console.error(`Kwela agent attempt ${attempt} failed:`, error);
            onUsage?.(null);
            if (error instanceof Error && error.name === "TimeoutError") break;
        }
    }
    throw new AssistantFailedError();
}

/** Creates the OpenAI-backed adapter for the career-assistant contract.
 * @param apiKey OpenAI credential configured for this runtime.
 * @returns An assistant implementation that returns streamed replies.
 */
export function createOpenAICareerAssistantService(apiKey: string, runner: AgentRunner = runCareerAgent): ICareerAssistant {
    setDefaultOpenAIKey(apiKey);
    return {
        async respond(messages: readonly IChatMessage[], onUsage?: UsageReporter, context?: IConversationContext | null) {
            const compactedContext = await compactConversationContext(messages, context ?? null, onUsage);
            const input: AgentInputItem[] = [];
            if (compactedContext?.summary) {
                const recentMessages = messages.slice(-20);
                const recentDraft = [...recentMessages].reverse().some((message) => message.application);
                input.push(await chatMessageToAgentInput({
                    id: "kwela-compacted-context",
                    role: "assistant",
                    content: `Saved context from earlier in this conversation. Use it as background, not as a new message from the user:\n${compactedContext.summary}`,
                    ...(!recentDraft && compactedContext.currentApplication ? { application: compactedContext.currentApplication } : {})
                }));
            }
            for (const message of messages.slice(-20)) input.push(await chatMessageToAgentInput(message));
            const reply = await runWithRetry(input, runner, onUsage);
            const body = new TextEncoder().encode(JSON.stringify({ ...reply, conversationContext: compactedContext }));
            return new ReadableStream<Uint8Array>({
                start(controller) {
                    controller.enqueue(body);
                    controller.close();
                }
            });
        }
    };
}
