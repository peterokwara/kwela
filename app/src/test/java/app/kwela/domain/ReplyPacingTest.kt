package app.kwela.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyPacingTest {
    @Test
    fun anAttachedFileMeansASlowReply() {
        assertTrue(ReplyPacing.isSlow("Please review my file.", hasAttachment = true))
    }

    @Test
    fun aLongMessageMeansASlowReply() {
        assertTrue(ReplyPacing.isSlow("x".repeat(300), hasAttachment = false))
    }

    @Test
    fun aJobLinkMeansASlowReplyBecauseTheWriterHasToReadThePage() {
        assertTrue(ReplyPacing.isSlow("Can you do this one https://example.test/jobs/42", hasAttachment = false))
    }

    @Test
    fun aShortChatIsQuick() {
        assertFalse(ReplyPacing.isSlow("ok", hasAttachment = false))
        assertFalse(ReplyPacing.isSlow("Make it shorter", hasAttachment = false))
        assertFalse(ReplyPacing.isSlow("I saw a job I want", hasAttachment = false))
    }
}
