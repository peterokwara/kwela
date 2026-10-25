package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMessagesTest {
    @Test
    fun theAppsOwnBubblesAreRecognisedByTheirId() {
        assertTrue(LocalMessages.isLocal("local-ack-123"))
        assertFalse(LocalMessages.isLocal("5f2c1e5a-1111-2222-3333-444455556666"))
    }

    @Test
    fun theLastRealRoleSkipsTheAppsOwnBubbles() {
        val messages = listOf("m1" to "USER", "m2" to "ASSISTANT", "m3" to "USER", "local-ack-1" to "ASSISTANT")

        assertEquals("USER", LocalMessages.lastRealRole(messages))
        assertEquals(null, LocalMessages.lastRealRole(listOf("local-ack-1" to "ASSISTANT")))
        assertEquals(null, LocalMessages.lastRealRole(emptyList()))
    }
}
