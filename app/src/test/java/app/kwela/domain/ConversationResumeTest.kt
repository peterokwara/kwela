package app.kwela.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationResumeTest {
    @Test
    fun aMessageThatNeverGotAnAnswerNeedsPickingUp() {
        assertTrue(ConversationResume.needsPickUp(lastRealRole = "USER", isReplying = false, hasErrorShown = false))
    }

    @Test
    fun nothingToPickUpWhileAReplyIsOnItsWay() {
        assertFalse(ConversationResume.needsPickUp(lastRealRole = "USER", isReplying = true, hasErrorShown = false))
    }

    @Test
    fun nothingExtraToShowWhenAnErrorCardAlreadyOffersTryAgain() {
        assertFalse(ConversationResume.needsPickUp(lastRealRole = "USER", isReplying = false, hasErrorShown = true))
    }

    @Test
    fun nothingToPickUpWhenKwelaHadTheLastWord() {
        assertFalse(ConversationResume.needsPickUp(lastRealRole = "ASSISTANT", isReplying = false, hasErrorShown = false))
        assertFalse(ConversationResume.needsPickUp(lastRealRole = null, isReplying = false, hasErrorShown = false))
    }
}
