package app.kwela.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_documents")
data class SavedDocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    /** "resume", "cover_letter" or "answers". Empty for older files. */
    val kind: String? = null,
    /** Structured draft used to make the good PDF on the server. */
    val sourceJson: String? = null,
    /** Documents written together for one job share a group id. */
    val groupId: String? = null,
    val groupTitle: String? = null
)
