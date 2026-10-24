package app.kwela

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        compose.activity.receiveSharedIntent(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://example.test/pending-internship")
        })
        compose.onNode(hasText("Email address", substring = true)).performTextInput("tester@example.test")
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Enter the 6-digit code to continue.").assertIsDisplayed()
        compose.onNode(hasText("6-digit code", substring = true) and hasSetTextAction()).performTextInput("123456")
        compose.onNodeWithText("Sign in").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Your next opportunity starts here.").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Your next opportunity starts here.").assertIsDisplayed()
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
        compose.onNodeWithText("Your next opportunity starts here.").assertIsDisplayed()
    }
}
