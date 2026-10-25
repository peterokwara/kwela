package app.kwela.data

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.kwela.data.local.ApplicationTaskEntity
import app.kwela.data.local.ChatMessageEntity
import app.kwela.data.local.KwelaDatabase
import app.kwela.data.local.SavedDocumentEntity
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KwelaRepositoryAccountIsolationTest {
    @Test
    fun changingAccountsClearsPrivateLocalDataButReturningToTheSameAccountKeepsIt() = runBlocking {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val isolatedFiles = File(baseContext.cacheDir, "repository-account-test-${UUID.randomUUID()}")
        val isolatedContext = object : ContextWrapper(baseContext) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = File(isolatedFiles, "files").apply { mkdirs() }
            override fun getNoBackupFilesDir(): File = File(isolatedFiles, "no_backup").apply { mkdirs() }
        }
        val database = Room.inMemoryDatabaseBuilder(isolatedContext, KwelaDatabase::class.java).build()
        val repository = KwelaRepository(isolatedContext, database)

        try {
            assertTrue(repository.signIn("amina@example.test", "token-a"))
            repository.saveMessage(ChatMessageEntity("a-message", "USER", "Private CV details", 1L))
            repository.saveTask(ApplicationTaskEntity("a-task", "Private task", "prompt", "COMPLETED", 1L, 2L, 2L))
            repository.saveDocument(SavedDocumentEntity("a-file", "Private CV.pdf", "Private content", 1L))
            repository.saveCandidateProfile("{\"name\":\"Amina\"}")
            repository.signOut()

            assertTrue(repository.signIn("brian@example.test", "token-b"))
            assertTrue(repository.messages.first().isEmpty())
            assertTrue(repository.tasks.first().isEmpty())
            assertTrue(repository.documents.first().isEmpty())
            assertNull(repository.candidateProfile.first())

            repository.saveMessage(ChatMessageEntity("b-message", "USER", "Brian's message", 3L))
            repository.signOut()
            assertFalse(repository.signIn("BRIAN@example.test", "new-token-b"))
            assertEquals(listOf("Brian's message"), repository.messages.first().map { it.content })
        } finally {
            repository.clearUserData()
            database.close()
        }
    }
}
