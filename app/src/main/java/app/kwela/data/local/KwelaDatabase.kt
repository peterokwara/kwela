package app.kwela.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChatMessageEntity::class, ApplicationTaskEntity::class, SavedDocumentEntity::class],
    version = 3,
    exportSchema = true
)
abstract class KwelaDatabase : RoomDatabase() {
    abstract fun kwelaDao(): KwelaDao

    companion object {
        /** Adds what the documents screen needs: type, structured source, job grouping, and the link from a reply to its documents. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_documents ADD COLUMN kind TEXT")
                db.execSQL("ALTER TABLE saved_documents ADD COLUMN sourceJson TEXT")
                db.execSQL("ALTER TABLE saved_documents ADD COLUMN groupId TEXT")
                db.execSQL("ALTER TABLE saved_documents ADD COLUMN groupTitle TEXT")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN documentGroupId TEXT")
            }
        }

        /** Adds the saved application draft that revisions build on. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN applicationJson TEXT")
            }
        }
    }
}
