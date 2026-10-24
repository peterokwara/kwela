package app.kwela.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface KwelaDao {
    @Query("SELECT * FROM chat_messages ORDER BY createdAt ASC")
    fun observeMessages(): Flow<List<ChatMessageEntity>>

    @Upsert
    suspend fun saveMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages()

    @Query("SELECT * FROM application_tasks ORDER BY startedAt DESC LIMIT 1")
    fun observeLatestTask(): Flow<ApplicationTaskEntity?>

    @Query("SELECT * FROM application_tasks ORDER BY startedAt ASC")
    fun observeTasks(): Flow<List<ApplicationTaskEntity>>

    @Query("SELECT * FROM application_tasks ORDER BY startedAt DESC LIMIT 1")
    suspend fun latestTask(): ApplicationTaskEntity?

    @Query("SELECT * FROM application_tasks WHERE id = :id LIMIT 1")
    suspend fun task(id: String): ApplicationTaskEntity?

    @Upsert
    suspend fun saveTask(task: ApplicationTaskEntity)

    @Query("SELECT * FROM application_tasks WHERE status = 'QUEUED' ORDER BY startedAt ASC LIMIT 1")
    suspend fun nextQueuedTask(): ApplicationTaskEntity?

    @Query("DELETE FROM application_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Query("DELETE FROM application_tasks")
    suspend fun clearTasks()

    @Query("SELECT * FROM saved_documents ORDER BY createdAt DESC")
    fun observeDocuments(): Flow<List<SavedDocumentEntity>>

    @Upsert
    suspend fun saveDocument(document: SavedDocumentEntity)

    @Query("DELETE FROM saved_documents WHERE id = :id")
    suspend fun deleteDocument(id: String)

    @Query("DELETE FROM saved_documents")
    suspend fun clearDocuments()
}
