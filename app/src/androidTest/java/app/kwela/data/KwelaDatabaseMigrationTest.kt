package app.kwela.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.kwela.data.local.KwelaDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
