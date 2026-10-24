import type { AgentInputItem } from "@openai/agents";
import type { IChatMessage } from "../../models/chat/IChatMessage.ts";
import { uploadedFileToAgentContent, type AgentFileContent } from "./uploadedFileInputService.ts";

/** Converts Kwela chat messages into the OpenAI Agents SDK input format.
 * @param message Message received by Kwela.
 * @returns An Agents SDK message with its text and attached files.
 */
export async function chatMessageToAgentInput(message: IChatMessage): Promise<AgentInputItem> {
    if (message.role === "assistant") {
        const priorDraft = message.application
            ? `\n\nCurrent application draft (Careergo-shaped job, resume, and cover-letter JSON):\n${JSON.stringify(message.application)}`
            : "";
        const candidateProfile = message.candidateProfile
            ? `\n\nSaved candidate CV profile:\n${JSON.stringify(message.candidateProfile)}`
            : "";
        return {
            type: "message",
            role: "assistant",
            status: "completed",
            content: [{ type: "output_text", text: `${message.content}${priorDraft}${candidateProfile}` }]
        };
    }

    const content: AgentFileContent[] = [];
    if (message.content.trim()) content.push({ type: "input_text", text: message.content });
    for (const file of message.files ?? []) content.push(...(await uploadedFileToAgentContent(file)));
    if (content.length === 0)
        content.push({ type: "input_text", text: "Please help me prepare for a job application." });
    return { type: "message", role: "user", content };
}
