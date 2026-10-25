package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRulesTest {
    @Test
    fun onlyNotifyWhenThePersonHasLeftTheApp() {
        assertTrue(NotificationRules.shouldNotify(appInForeground = false, repliesEnabled = true, permissionGranted = true))
        assertFalse(NotificationRules.shouldNotify(appInForeground = true, repliesEnabled = true, permissionGranted = true))
    }

    @Test
    fun neverNotifyAgainstTheirSettingsOrPermission() {
        assertFalse(NotificationRules.shouldNotify(appInForeground = false, repliesEnabled = false, permissionGranted = true))
        assertFalse(NotificationRules.shouldNotify(appInForeground = false, repliesEnabled = true, permissionGranted = false))
    }

    @Test
    fun theNotificationShowsUpToThreeBubblesAsLines() {
        val lines = NotificationText.lines("Okay, done 🙂\n\nYour CV is ready.\n\nHope it feels like you.\n\nNext: have a look.")

        assertEquals(listOf("Okay, done 🙂", "Your CV is ready.", "Hope it feels like you."), lines)
    }

    @Test
    fun aVeryLongBubbleIsShortenedForTheLockScreen() {
        val lines = NotificationText.lines("word ".repeat(80))

        assertTrue(lines.single().length <= 140)
        assertTrue(lines.single().endsWith("…"))
    }

    @Test
    fun theLockScreenVersionRevealsNothing() {
        assertEquals("New message from Kwela", NotificationText.PUBLIC_TEXT)
    }

    @Test
    fun aTypedReplyIsTrimmedAndEmptyOnesAreDropped() {
        assertEquals("Yes please", NotificationText.cleanReply("  Yes please \n"))
        assertNull(NotificationText.cleanReply("   "))
        assertNull(NotificationText.cleanReply(null))
    }
}
