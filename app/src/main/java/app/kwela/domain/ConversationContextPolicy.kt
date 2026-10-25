package app.kwela.domain

/** Keeps all turns not yet summarized, plus the latest twelve turns in full. */
object ConversationContextPolicy {
    const val KEEP_RECENT_MESSAGES = 12
    const val MIN_MESSAGES_TO_COMPACT = 6
    const val MAX_MESSAGES = 400

    fun compactionEnd(messageCount: Int): Int = (messageCount - KEEP_RECENT_MESSAGES).coerceAtLeast(0)

    fun windowStart(messageIds: List<String>, throughMessageId: String?): Int {
        val cursor = messageIds.indexOf(throughMessageId)
        return if (cursor < 0) 0 else minOf(cursor + 1, compactionEnd(messageIds.size))
    }
}
