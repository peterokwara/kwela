package app.kwela.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ChatMessageEntity::class, ApplicationTaskEntity::class, SavedDocumentEntity::class],
    version = 1,
    exportSchema = true
)
abstract class KwelaDatabase : RoomDatabase() {
    abstract fun kwelaDao(): KwelaDao
}
