package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationContextPolicyTest {
    @Test fun keepsAllHistoryUntilASummaryExists() {
        val ids = (1..60).map { "m$it" }
        assertEquals(0, ConversationContextPolicy.windowStart(ids, null))
        assertEquals(0, ConversationContextPolicy.windowStart(ids, "unknown"))
    }
    @Test fun dropsOnlyAlreadySummarizedTurnsAndKeepsTwelveFullMessages() {
        val ids = (1..60).map { "m$it" }
        assertEquals(20, ConversationContextPolicy.windowStart(ids, "m20"))
        assertEquals(48, ConversationContextPolicy.windowStart(ids, "m60"))
    }
    @Test fun waitsForSixOldMessagesBeforeCompacting() {
        assertEquals(5, ConversationContextPolicy.compactionEnd(17))
        assertEquals(6, ConversationContextPolicy.compactionEnd(18))
        assertEquals(12, ConversationContextPolicy.KEEP_RECENT_MESSAGES)
        assertEquals(6, ConversationContextPolicy.MIN_MESSAGES_TO_COMPACT)
    }
}
