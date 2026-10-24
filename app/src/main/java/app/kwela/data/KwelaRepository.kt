package app.kwela.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.kwela.data.local.ApplicationTaskEntity
import app.kwela.data.local.ChatMessageEntity
import app.kwela.data.local.KwelaDatabase
import app.kwela.data.local.SavedDocumentEntity
import app.kwela.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.preferencesDataStore by preferencesDataStore(name = "kwela_preferences")

class KwelaRepository(context: Context, database: KwelaDatabase) {
    private val appContext = context.applicationContext
    private val dao = database.kwelaDao()

    val messages: Flow<List<ChatMessageEntity>> = dao.observeMessages()
    val latestTask: Flow<ApplicationTaskEntity?> = dao.observeLatestTask()
    val tasks: Flow<List<ApplicationTaskEntity>> = dao.observeTasks()
    val documents: Flow<List<SavedDocumentEntity>> = dao.observeDocuments()
    val signedInEmail: Flow<String?> = appContext.preferencesDataStore.data.map { it[EMAIL_KEY] }
    val pendingSharedText: Flow<String?> = appContext.preferencesDataStore.data.map { it[PENDING_SHARE_KEY] }
    val themeMode: Flow<ThemeMode> = appContext.preferencesDataStore.data.map { ThemeMode.fromStored(it[THEME_KEY]) }
    val notificationsEnabled: Flow<Boolean> = appContext.preferencesDataStore.data.map { it[NOTIFICATIONS_KEY] ?: true }

    suspend fun saveMessage(message: ChatMessageEntity) = dao.saveMessage(message)
    suspend fun saveTask(task: ApplicationTaskEntity) = dao.saveTask(task)
    suspend fun getLatestTask(): ApplicationTaskEntity? = dao.latestTask()
    suspend fun getTask(id: String): ApplicationTaskEntity? = dao.task(id)
    suspend fun getNextQueuedTask(): ApplicationTaskEntity? = dao.nextQueuedTask()
    suspend fun saveDocument(document: SavedDocumentEntity) = dao.saveDocument(document)
    suspend fun deleteDocument(id: String) = dao.deleteDocument(id)

    suspend fun signIn(email: String) {
        appContext.preferencesDataStore.edit { it[EMAIL_KEY] = email }
    }

    suspend fun signOut() {
        appContext.preferencesDataStore.edit { it.remove(EMAIL_KEY) }
    }

    suspend fun savePendingSharedText(text: String?) {
        appContext.preferencesDataStore.edit { preferences ->
            if (text == null) preferences.remove(PENDING_SHARE_KEY) else preferences[PENDING_SHARE_KEY] = text
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        appContext.preferencesDataStore.edit { it[NOTIFICATIONS_KEY] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        appContext.preferencesDataStore.edit { it[THEME_KEY] = mode.name }
    }

    suspend fun clearUserData() {
        dao.clearMessages()
        dao.clearDocuments()
        dao.clearTasks()
        appContext.preferencesDataStore.edit { preferences ->
            preferences.remove(EMAIL_KEY)
            preferences.remove(PENDING_SHARE_KEY)
        }
    }

    private companion object {
        val EMAIL_KEY = stringPreferencesKey("signed_in_email")
        val PENDING_SHARE_KEY = stringPreferencesKey("pending_shared_text")
        val THEME_KEY = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")
    }
}
