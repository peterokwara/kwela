package app.kwela.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_documents")
data class SavedDocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val createdAt: Long
)
