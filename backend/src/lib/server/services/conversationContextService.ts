import { Agent, run, type AgentInputItem } from "@openai/agents";
import { z } from "zod";
import type { IConversationContext } from "../../models/chat/IConversationContext.ts";
import type { IChatMessage } from "../../models/chat/IChatMessage.ts";
import { CAREER_AGENT_MODEL } from "./agents/agentModel.ts";
import { chatMessageToAgentInput } from "./chatMessageInputService.ts";

const KEEP_RECENT_MESSAGES = 12;
const MIN_MESSAGES_TO_COMPACT = 6;
const MAX_SUMMARY_CHARS = 6000;
const SUMMARY_TIMEOUT_MS = 30_000;

const contextSummaryAgent = new Agent({
    name: "Kwela Conversation Note Taker",
    model: CAREER_AGENT_MODEL,
    outputType: z.object({ summary: z.string().max(MAX_SUMMARY_CHARS) }),
    instructions: `Write a compact memory for Kwela's next reply. Keep the active job, the user's goals and preferences, confirmed decisions, unanswered questions, and what should happen next. Keep important facts from attached job files. Keep facts accurate and distinguish confirmed facts from guesses. Drop greetings, repeated details, and completed side topics. The candidate CV profile is stored separately, so do not copy personal contact details or reproduce the full CV. Keep the note under 900 words. Return only the summary field.`
});

type UsageReporter = (usage: { inputTokens: number; outputTokens: number } | null) => void;

/** Folds older chat turns into a rolling summary while leaving the recent exchange intact. */
export async function compactConversationContext(
    messages: readonly IChatMessage[],
    current: IConversationContext | null,
    onUsage?: UsageReporter
): Promise<IConversationContext | null> {
    const eligibleEnd = Math.max(0, messages.length - KEEP_RECENT_MESSAGES);
    if (eligibleEnd === 0) return current;

    const cursor = current ? messages.findIndex((message) => message.id === current.throughMessageId) : -1;
    const start = cursor < 0 ? 0 : cursor + 1;
    if (eligibleEnd - start < MIN_MESSAGES_TO_COMPACT) return current;

    const compactedMessages = messages.slice(start, eligibleEnd);
    const input: AgentInputItem[] = [{
        type: "message",
        role: "user",
        content: [{
            type: "input_text",
            text: current?.summary
                ? `Update the existing conversation memory using the following turns. Existing memory:\n${current.summary}`
                : "Create a compact conversation memory from the following turns."
        }]
    }];

    try {
        for (const message of compactedMessages) input.push(await chatMessageToAgentInput(message));
        const result = await run(contextSummaryAgent, input, {
            maxTurns: 1,
            signal: AbortSignal.timeout(SUMMARY_TIMEOUT_MS)
        });
        onUsage?.(result.state.usage);
        const summary = result.finalOutput?.summary?.trim();
        if (!summary) return current;

        const latestApplication = [...compactedMessages].reverse().find((message) => message.application)?.application;
        return {
            summary: summary.slice(0, MAX_SUMMARY_CHARS),
            throughMessageId: compactedMessages.at(-1)!.id,
            ...(latestApplication ? { currentApplication: latestApplication } : current?.currentApplication ? { currentApplication: current.currentApplication } : {})
        };
    } catch (error) {
        console.error("Kwela conversation compaction failed:", error);
        onUsage?.(null);
        return current;
    }
}
