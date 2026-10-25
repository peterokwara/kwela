package app.kwela.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.kwela.data.local.KwelaDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KwelaDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        KwelaDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun upgradingKeepsEarlierMessagesAndAddsRoomForTheApplicationDraft() {
        helper.createDatabase("kwela-migration-test.db", 1).apply {
            execSQL("INSERT INTO chat_messages (id, role, content, createdAt, attachmentName, attachmentUri) VALUES ('m1', 'USER', 'Help me apply', 1000, NULL, NULL)")
            close()
        }

        val upgraded = helper.runMigrationsAndValidate("kwela-migration-test.db", 2, true, KwelaDatabase.MIGRATION_1_2)
        upgraded.query("SELECT content, applicationJson FROM chat_messages WHERE id = 'm1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Help me apply", cursor.getString(0))
            assertNull(cursor.getString(1).takeIf { !cursor.isNull(1) })
        }
        upgraded.execSQL("UPDATE chat_messages SET applicationJson = '{\"job\":{}}' WHERE id = 'm1'")
        upgraded.query("SELECT applicationJson FROM chat_messages WHERE id = 'm1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("{\"job\":{}}", cursor.getString(0))
        }
    }

    @Test
    fun upgradingToVersionThreeKeepsDocumentsAndMessages() {
        helper.createDatabase("kwela-migration-3-test.db", 2).apply {
            execSQL("INSERT INTO saved_documents (id, title, body, createdAt) VALUES ('d1', 'CV', 'Amina', 1000)")
            execSQL("INSERT INTO chat_messages (id, role, content, createdAt, attachmentName, attachmentUri, applicationJson) VALUES ('m1', 'ASSISTANT', 'Done', 2000, NULL, NULL, NULL)")
            close()
        }

        val upgraded = helper.runMigrationsAndValidate("kwela-migration-3-test.db", 3, true, KwelaDatabase.MIGRATION_2_3)
        upgraded.query("SELECT title, body, kind, groupId FROM saved_documents WHERE id = 'd1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("CV", cursor.getString(0))
            assertEquals("Amina", cursor.getString(1))
            assertTrue(cursor.isNull(2) && cursor.isNull(3))
        }
        upgraded.query("SELECT content, documentGroupId FROM chat_messages WHERE id = 'm1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Done", cursor.getString(0))
            assertTrue(cursor.isNull(1))
        }
    }

    @Test
    fun upgradingToVersionFourKeepsMessagesAndAddsTheFileLink() {
        helper.createDatabase("kwela-migration-4-test.db", 3).apply {
            execSQL("INSERT INTO chat_messages (id, role, content, createdAt, attachmentName, attachmentUri, applicationJson, documentGroupId) VALUES ('m1', 'ASSISTANT', 'Done', 2000, NULL, NULL, NULL, 'g1')")
            close()
        }

        val upgraded = helper.runMigrationsAndValidate("kwela-migration-4-test.db", 4, true, KwelaDatabase.MIGRATION_3_4)
        upgraded.query("SELECT content, documentGroupId, documentId FROM chat_messages WHERE id = 'm1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Done", cursor.getString(0))
            assertEquals("g1", cursor.getString(1))
            assertTrue(cursor.isNull(2))
        }
    }
}
