/**
 * File attached to a chat message.
 */
export interface IChatAttachment {
    /**
     * Stable identifier used by the chat interface.
     */
    id: string;
    /**
     * Original filename.
     */
    name: string;
    /**
     * Browser-reported media type.
     */
    type: string;
    /**
     * Base64 data URL sent to the assistant.
     */
    data: string;
}
