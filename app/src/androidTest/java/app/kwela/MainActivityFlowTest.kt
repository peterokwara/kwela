package app.kwela

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
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
        compose.onNodeWithText("For this local demo, use 123456. No email is sent.").assertIsDisplayed()
        compose.onNode(hasText("6-digit code", substring = true)).performTextInput("123456")
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
        compose.onNodeWithContentDescription("Jobs").performClick()
        compose.onNodeWithText("Examples only. Live listings are not connected yet.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Applications").performClick()
        compose.onNodeWithText("No applications yet. Ask Kwela in Chat, or discuss a job from Jobs.").assertIsDisplayed()
        compose.onNodeWithText("Kwela").performClick()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Notifications").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithContentDescription("Chat").performClick()
        compose.onNodeWithText("Your next opportunity starts here.").assertIsDisplayed()
    }
}
