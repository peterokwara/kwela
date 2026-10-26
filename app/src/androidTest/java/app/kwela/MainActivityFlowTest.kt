package app.kwela

import android.content.Intent
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.assertTextEquals
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import app.kwela.data.ReplyWorker
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
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
        compose.onNodeWithText("Use email instead").assertDoesNotExist()

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
        compose.onNodeWithContentDescription("Open profile").performClick()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Reply notifications").assertIsDisplayed()
        compose.onNodeWithText("Theme").assertIsDisplayed()
        compose.onNodeWithText("About Kwela").performClick()
        compose.onNodeWithText("Licences").assertIsDisplayed()
        compose.onNodeWithText("Privacy Policy").assertIsDisplayed()
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithContentDescription("Close settings").performClick()
        compose.onNodeWithContentDescription("Chat").performClick()
        compose.onNodeWithText("What’s going on right now?").assertIsDisplayed()

        // A delayed worker keeps Kwela busy without calling the real API.
        val manager = WorkManager.getInstance(compose.activity)
        val pending = OneTimeWorkRequestBuilder<ReplyWorker>().setInitialDelay(1, TimeUnit.HOURS).build()
        manager.enqueueUniqueWork(ReplyWorker.UNIQUE, ExistingWorkPolicy.REPLACE, pending).result.get()
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("Keep working; this is my queued follow-up.")
        compose.onNodeWithContentDescription("Send").assertIsDisplayed().performClick()
        compose.waitUntil(5_000) {
            runBlocking { app.repository.messages.first().any { it.content == "Keep working; this is my queued follow-up." } }
        }
        compose.onNode(hasSetTextAction()).assertTextEquals("")
        manager.cancelUniqueWork(ReplyWorker.UNIQUE).result.get()
    }
}
