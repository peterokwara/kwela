package app.kwela.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChatMessageEntity::class, ApplicationTaskEntity::class, SavedDocumentEntity::class],
    version = 2,
    exportSchema = true
)
abstract class KwelaDatabase : RoomDatabase() {
    abstract fun kwelaDao(): KwelaDao

    companion object {
        /** Adds the saved application draft that revisions build on. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN applicationJson TEXT")
            }
        }
    }
}
