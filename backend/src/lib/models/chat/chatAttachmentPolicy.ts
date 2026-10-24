/**
 * Shared size and count limits for chat attachments.
 */
export const CHAT_ATTACHMENT_POLICY = {
    /**
     * Maximum number of files attached to one message.
     */
    maximumFiles: 8,
    /**
     * Maximum raw size of one file.
     */
    maximumFileSizeBytes: 5 * 1024 * 1024,
    /**
     * Maximum combined raw size of the files in one message.
     */
    maximumTotalFileSizeBytes: 20 * 1024 * 1024,
    /**
     * Maximum encoded data URL length for one file, including its media prefix.
     */
    maximumEncodedFileLength: Math.ceil((5 * 1024 * 1024 * 4) / 3) + 256,
    /**
     * Maximum combined encoded data URL length for all files in one message.
     */
    maximumEncodedTotalLength: Math.ceil((20 * 1024 * 1024 * 4) / 3) + 8 * 256
} as const;

const acceptedExtensions = new Set(["pdf", "docx", "txt", "md", "jpg", "jpeg", "png", "webp"]);
const acceptedMimeTypes = new Set([
    "application/pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "text/plain",
    "text/markdown",
    "image/jpeg",
    "image/jpg",
    "image/png",
    "image/webp",
    "application/octet-stream"
]);

/** Returns whether a filename and browser media type match supported chat attachments.
 * @param name Original filename.
 * @param mimeType Browser-reported media type.
 * @returns True when Kwela accepts the file format.
 */
export function isSupportedChatFile(name: string, mimeType: string): boolean {
    const extension = name.split(".").pop()?.toLowerCase();
    return Boolean(extension && acceptedExtensions.has(extension) && (!mimeType || acceptedMimeTypes.has(mimeType)));
}
