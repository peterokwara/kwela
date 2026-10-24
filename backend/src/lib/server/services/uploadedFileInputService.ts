import mammoth from "mammoth";
import type { IChatAttachment } from "../../models/chat/IChatAttachment.ts";

/**
 * Content blocks accepted by the OpenAI Agents SDK for uploaded files.
 */
export type AgentFileContent =
    | { type: "input_text"; text: string }
    | { type: "input_file"; file: string; filename: string }
    | { type: "input_image"; image: string; detail?: string };

/**
 * Decodes the media type and base64 body from a data URL.
 */
function decodeDataUrl(file: IChatAttachment): { mimeType: string; base64: string } | null {
    const match = /^data:([^;,]+);base64,([\s\S]+)$/.exec(file.data);
    if (!match) return null;
    return { mimeType: match[1].toLowerCase(), base64: match[2] };
}

/** Converts one attached file into the text, image, or file block sent to the model.
 * @param file Uploaded chat attachment.
 * @returns Content blocks accepted by the Agents SDK.
 */
export async function uploadedFileToAgentContent(file: IChatAttachment): Promise<AgentFileContent[]> {
    const parsed = decodeDataUrl(file);
    if (!parsed) return [];

    const extension = file.name.split(".").pop()?.toLowerCase();
    if (parsed.mimeType.startsWith("image/")) {
        return [{ type: "input_image", image: file.data, detail: "high" }];
    }
    if (extension === "pdf" || parsed.mimeType === "application/pdf") {
        return [{ type: "input_file", file: file.data, filename: file.name }];
    }
    if (extension === "docx") {
        const bytes = Uint8Array.from(atob(parsed.base64), (character) => character.charCodeAt(0));
        const result = await mammoth.extractRawText({ arrayBuffer: bytes.buffer });
        return [{ type: "input_text", text: `Contents of ${file.name}:\n${result.value.slice(0, 30000)}` }];
    }
    return [
        {
            type: "input_text",
            text: `Contents of ${file.name}:\n${Buffer.from(parsed.base64, "base64").toString("utf8").slice(0, 30000)}`
        }
    ];
}
