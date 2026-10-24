package app.kwela.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "application_tasks")
data class ApplicationTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val prompt: String,
    val status: String,
    val startedAt: Long,
    val completesAt: Long,
    val updatedAt: Long
)
