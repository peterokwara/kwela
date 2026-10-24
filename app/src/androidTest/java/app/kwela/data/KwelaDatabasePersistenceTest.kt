package app.kwela.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.kwela.data.local.ChatMessageEntity
import app.kwela.data.local.KwelaDatabase
import app.kwela.data.local.ApplicationTaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KwelaDatabasePersistenceTest {
    @Test
    fun conversationSurvivesClosingAndReopeningTheDatabase() {
        runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("kwela-persistence-test.db")

        val firstOpen = Room.databaseBuilder(
            context,
            KwelaDatabase::class.java,
            "kwela-persistence-test.db"
        ).build()
        firstOpen.kwelaDao().saveMessage(
            ChatMessageEntity(
                id = "known-message-id",
                role = "USER",
                content = "Help me apply for an internship",
                createdAt = 1_000L
            )
        )
        firstOpen.kwelaDao().saveTask(
            ApplicationTaskEntity(
                id = "running-task-id",
                title = "Application pack",
                prompt = "Tailor my CV",
                status = "RUNNING",
                startedAt = 1_000L,
                completesAt = 20_000L,
                updatedAt = 1_000L
            )
        )
        firstOpen.close()

        val reopened = Room.databaseBuilder(
            context,
            KwelaDatabase::class.java,
            "kwela-persistence-test.db"
        ).build()
        val restored = reopened.kwelaDao().observeMessages().first()

        assertEquals("Help me apply for an internship", restored.single().content)
        assertEquals("RUNNING", reopened.kwelaDao().task("running-task-id")?.status)
        reopened.close()
        context.deleteDatabase("kwela-persistence-test.db")
        }
    }
}
