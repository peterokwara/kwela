import { applicationPackSchema } from "../../models/careergo/IApplicationPack.ts";
import type { IChatAttachment } from "../../models/chat/IChatAttachment.ts";
import type { IChatMessage } from "../../models/chat/IChatMessage.ts";
import { CHAT_ATTACHMENT_POLICY, isSupportedChatFile } from "../../models/chat/chatAttachmentPolicy.ts";

/**
 * Maximum JSON request body size accepted by the chat endpoint.
 */
const maximumRequestLength = 32 * 1024 * 1024;
/**
 * Maximum conversation messages accepted in one request.
 */
const maximumMessages = 40;

type ChatRequestResult = { messages: IChatMessage[] } | { response: Response };

/** Creates a JSON error response for invalid chat requests.
 * @param message User-facing explanation.
 * @param status HTTP status code.
 * @returns JSON response with the supplied status.
 */
function badRequest(message: string, status: number): Response {
    return Response.json({ message }, { status });
}

/** Reads the media type from a base64 data URL.
 * @param file Attachment containing the encoded data URL.
 * @returns Parsed media type, or null for an invalid data URL.
 */
function decodeDataUrl(file: IChatAttachment): { mimeType: string } | null {
    const match = /^data:([^;,]+);base64,([\s\S]+)$/.exec(file.data);
    return match ? { mimeType: match[1].toLowerCase() } : null;
}

/** Checks one untrusted chat message against the chat request model.
 * @param value Unknown value read from a request body.
 * @returns True when the value matches a valid chat message.
 */
function isValidMessage(value: unknown): value is IChatMessage {
    if (!value || typeof value !== "object") return false;
    const message = value as Partial<IChatMessage>;
    if (
        typeof message.id !== "string" ||
        (message.role !== "user" && message.role !== "assistant") ||
        typeof message.content !== "string" ||
        message.content.length > 20000
    )
        return false;
    if (
        message.application !== undefined &&
        (message.role !== "assistant" || !applicationPackSchema.safeParse(message.application).success)
    )
        return false;
    if (
        message.files !== undefined &&
        (!Array.isArray(message.files) ||
            message.files.length > CHAT_ATTACHMENT_POLICY.maximumFiles ||
            message.role !== "user")
    )
        return false;
    let totalFileDataLength = 0;
    for (const file of message.files ?? []) {
        if (
            !file ||
            typeof file.id !== "string" ||
            typeof file.name !== "string" ||
            typeof file.type !== "string" ||
            typeof file.data !== "string"
        )
            return false;
        if (file.data.length === 0) continue;
        const parsed = decodeDataUrl(file);
        if (!parsed || !isSupportedChatFile(file.name, parsed.mimeType) || !isSupportedChatFile(file.name, file.type))
            return false;
        if (file.data.length > CHAT_ATTACHMENT_POLICY.maximumEncodedFileLength) return false;
        totalFileDataLength += file.data.length;
    }
    return totalFileDataLength <= CHAT_ATTACHMENT_POLICY.maximumEncodedTotalLength;
}

/** Checks the number of messages and validates every message in a request.
 * @param value Unknown messages value from a request body.
 * @returns True when all messages meet the chat request limits.
 */
function isValidMessages(value: unknown): value is IChatMessage[] {
    return Array.isArray(value) && value.length > 0 && value.length <= maximumMessages && value.every(isValidMessage);
}

/** Reads and validates chat messages before an assistant receives them.
 * @param request Incoming chat request.
 * @returns Validated messages, or an HTTP response describing the input error.
 */
export async function readChatMessages(request: Request): Promise<ChatRequestResult> {
    const declaredLength = Number(request.headers.get("content-length") ?? 0);
    if (declaredLength > maximumRequestLength) {
        return {
            response: badRequest("This conversation is too large. Start a new chat or remove a large file.", 413)
        };
    }

    let body: { messages?: unknown };
    try {
        const rawBody = await request.text();
        if (new TextEncoder().encode(rawBody).byteLength > maximumRequestLength) {
            return {
                response: badRequest("This conversation is too large. Start a new chat or remove a large file.", 413)
            };
        }
        const parsed: unknown = JSON.parse(rawBody);
        if (!parsed || typeof parsed !== "object" || Array.isArray(parsed) || !("messages" in parsed)) {
            return { response: badRequest("Send a valid chat message.", 400) };
        }
        body = parsed as { messages?: unknown };
    } catch {
        return { response: badRequest("The chat request was not valid JSON.", 400) };
    }

    if (!isValidMessages(body.messages)) {
        const tooLong =
            Array.isArray(body.messages) &&
            body.messages.some(
                (message) =>
                    typeof (message as IChatMessage)?.content === "string" &&
                    (message as IChatMessage).content.length > 20000
            );
        if (tooLong) {
            return {
                response: badRequest(
                    "That message is too long. Paste only the job description, or attach the job as a file.",
                    400
                )
            };
        }
        return {
            response: badRequest(
                "Send a valid chat message with up to 8 files. Add PDF, DOCX, TXT, Markdown, JPG, PNG, or WEBP files under 5 MB each and 20 MB total.",
                400
            )
        };
    }
    return { messages: body.messages };
}
