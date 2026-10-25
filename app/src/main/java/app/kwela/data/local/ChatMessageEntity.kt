package app.kwela.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val role: String,
    val content: String,
    val createdAt: Long,
    val attachmentName: String? = null,
    val attachmentUri: String? = null,
    val applicationJson: String? = null
)
