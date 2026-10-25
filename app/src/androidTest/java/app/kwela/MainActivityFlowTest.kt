package app.kwela

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun demoOtpUnlocksChatAndBottomNavigationWorks() {
        compose.onNodeWithText("Welcome to Kwela").assertIsDisplayed()
        compose.onNode(hasText("Email address", substring = true)).performTextInput("not-an-email")
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Enter a valid email address.").assertIsDisplayed()

        compose.activity.receiveSharedIntent(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://example.test/pending-internship")
        })
        // Real sign-in needs the network and an inbox, so sign in through storage here.
        val app = compose.activity.application as app.kwela.KwelaApplication
        runBlocking { app.repository.signIn("tester@example.test", "test-session-token") }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("What’s going on right now?").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("What’s going on right now?").assertIsDisplayed()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("https://example.test/pending-internship").fetchSemanticsNodes().isNotEmpty()
        }

        compose.activity.receiveSharedIntent(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://example.test/shared-after-login")
        })
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("https://example.test/shared-after-login").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithContentDescription("Files").performClick()
        compose.onNodeWithText("Your files will appear here").assertIsDisplayed()
        compose.onNodeWithText("Kwela").performClick()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Notifications").assertIsDisplayed()
        compose.onNodeWithText("Theme").assertIsDisplayed()
        compose.onNodeWithText("Open-source licenses").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithContentDescription("Chat").performClick()
        compose.onNodeWithText("What’s going on right now?").assertIsDisplayed()
    }
}
