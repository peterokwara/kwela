package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FailedRepliesTest {
    @Test
    fun everyFailedMessageKeepsItsOwnReasonThroughStorage() {
        val failed = mapOf("m1" to "Slow down a little.", "m2" to "That one took too long.")

        assertEquals(failed, FailedReplies.decode(FailedReplies.encode(failed)))
    }

    @Test
    fun nothingFailedStoresNothing() {
        assertNull(FailedReplies.encode(emptyMap()))
        assertEquals(emptyMap<String, String>(), FailedReplies.decode(null))
    }

    @Test
    fun anUnreadableStoredValueIsTreatedAsNoFailures() {
        assertEquals(emptyMap<String, String>(), FailedReplies.decode("not json"))
    }

    @Test
    fun theReasonPointsAtResendInsteadOfTheOldTryAgainButton() {
        assertEquals("Kwela couldn’t finish that one. Tap Resend.", FailedReplies.reason("Kwela couldn’t finish that one. Tap Try again."))
        assertEquals("Slow down a little.", FailedReplies.reason("Slow down a little."))
        assertNull(FailedReplies.reason(null))
    }
}
