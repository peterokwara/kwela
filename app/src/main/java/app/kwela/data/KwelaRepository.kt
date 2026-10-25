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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.preferencesDataStore by preferencesDataStore(name = "kwela_preferences")

class KwelaRepository(context: Context, database: KwelaDatabase) {
    private val appContext = context.applicationContext
    private val dao = database.kwelaDao()

    val messages: Flow<List<ChatMessageEntity>> = dao.observeMessages()
    val latestTask: Flow<ApplicationTaskEntity?> = dao.observeLatestTask()
    val tasks: Flow<List<ApplicationTaskEntity>> = dao.observeTasks()
    val documents: Flow<List<SavedDocumentEntity>> = dao.observeDocuments()
    val signedInEmail: Flow<String?> = appContext.preferencesDataStore.data.map { preferences -> preferences[EMAIL_KEY].takeIf { preferences[TOKEN_KEY] != null } }
    val pendingSharedText: Flow<String?> = appContext.preferencesDataStore.data.map { it[PENDING_SHARE_KEY] }
    val themeMode: Flow<ThemeMode> = appContext.preferencesDataStore.data.map { ThemeMode.fromStored(it[THEME_KEY]) }
    /** The last reply failure to show as a Try again card, kept on disk so it never replays by accident. */
    val replyError: Flow<String?> = appContext.preferencesDataStore.data.map { it[REPLY_ERROR_KEY] }

    /** A one-time note for the sign-in screen, such as "Please sign in again." */
    val signInNotice: Flow<String?> = appContext.preferencesDataStore.data.map { it[SIGN_IN_NOTICE_KEY] }

    val pendingReviewJson: Flow<String?> = appContext.preferencesDataStore.data.map { it[REVIEW_KEY] }
    val candidateProfile: Flow<String?> = appContext.preferencesDataStore.data.map { it[PROFILE_KEY] }
    val conversationContext: Flow<String?> = appContext.preferencesDataStore.data.map { it[CONVERSATION_CONTEXT_KEY] }
    val notificationsEnabled: Flow<Boolean> = appContext.preferencesDataStore.data.map { it[NOTIFICATIONS_KEY] ?: true }

    suspend fun saveMessage(message: ChatMessageEntity) = dao.saveMessage(message)
    suspend fun deleteMessage(id: String) = dao.deleteMessage(id)
    suspend fun saveTask(task: ApplicationTaskEntity) = dao.saveTask(task)
    suspend fun getLatestTask(): ApplicationTaskEntity? = dao.latestTask()
    suspend fun getTask(id: String): ApplicationTaskEntity? = dao.task(id)
    suspend fun getNextQueuedTask(): ApplicationTaskEntity? = dao.nextQueuedTask()
    suspend fun saveDocument(document: SavedDocumentEntity) = dao.saveDocument(document)
    suspend fun deleteDocument(id: String) = dao.deleteDocument(id)

    /** Stores a session and clears account-scoped local data when this phone changes accounts. */
    suspend fun signIn(email: String, sessionToken: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        val previousOwner = appContext.preferencesDataStore.data.first()[DATA_OWNER_EMAIL_KEY]
        val changedAccount = previousOwner != normalizedEmail
        if (changedAccount) {
            dao.clearMessages()
            dao.clearDocuments()
            dao.clearTasks()
        }
        appContext.preferencesDataStore.edit { preferences ->
            if (changedAccount) {
                preferences.remove(PROFILE_KEY)
                preferences.remove(CONVERSATION_CONTEXT_KEY)
            }
            preferences[DATA_OWNER_EMAIL_KEY] = normalizedEmail
            preferences[EMAIL_KEY] = normalizedEmail
            preferences[TOKEN_KEY] = sessionToken
        }
        return changedAccount
    }

    suspend fun signOut() {
        appContext.preferencesDataStore.edit { it.remove(EMAIL_KEY); it.remove(TOKEN_KEY) }
    }

    /** The bearer token for the Kwela API, or null when signed out. */
    suspend fun sessionToken(): String? = appContext.preferencesDataStore.data.first()[TOKEN_KEY]

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

    suspend fun draft(): String = appContext.preferencesDataStore.data.first()[DRAFT_KEY].orEmpty()

    suspend fun savePendingReview(json: String?) {
        appContext.preferencesDataStore.edit { if (json == null) it.remove(REVIEW_KEY) else it[REVIEW_KEY] = json }
    }

    /** Returns true the first time it is called, so the permission question is asked at most once. */
    suspend fun claimNotificationPrompt(): Boolean {
        var first = false
        appContext.preferencesDataStore.edit { first = it[PROMPTED_KEY] != true; it[PROMPTED_KEY] = true }
        return first
    }

    suspend fun setReplyError(message: String?) {
        appContext.preferencesDataStore.edit { if (message == null) it.remove(REPLY_ERROR_KEY) else it[REPLY_ERROR_KEY] = message }
    }

    suspend fun setSignInNotice(message: String?) {
        appContext.preferencesDataStore.edit { if (message == null) it.remove(SIGN_IN_NOTICE_KEY) else it[SIGN_IN_NOTICE_KEY] = message }
    }

    /** Lines of the CV the person has already been asked about, so the writer never asks the same thing twice. */
    suspend fun answeredReviewLines(): Set<String> =
        appContext.preferencesDataStore.data.first()[ANSWERED_KEY]?.split(ANSWERED_SEPARATOR)?.filter { it.isNotBlank() }?.toSet().orEmpty()

    suspend fun addAnsweredReviewLine(line: String) {
        appContext.preferencesDataStore.edit { preferences ->
            val all = preferences[ANSWERED_KEY]?.split(ANSWERED_SEPARATOR).orEmpty().filter { it.isNotBlank() } + line
            preferences[ANSWERED_KEY] = all.takeLast(50).joinToString(ANSWERED_SEPARATOR)
        }
    }

    /** Adds a short bubble written by the app itself. It is shown in the chat but never sent to the writer. Returns its id. */
    suspend fun sayLocally(text: String, documentId: String? = null): String {
        val id = app.kwela.domain.LocalMessages.PREFIX + java.util.UUID.randomUUID()
        dao.saveMessage(ChatMessageEntity(id, "ASSISTANT", text, System.currentTimeMillis(), documentId = documentId))
        return id
    }

    suspend fun saveDraftNow(text: String) = saveDraft(text)

    suspend fun saveDraft(text: String) {
        appContext.preferencesDataStore.edit { if (text.isEmpty()) it.remove(DRAFT_KEY) else it[DRAFT_KEY] = text }
    }

    suspend fun saveCandidateProfile(profileJson: String) {
        appContext.preferencesDataStore.edit { it[PROFILE_KEY] = profileJson }
    }

    suspend fun saveConversationContext(contextJson: String) {
        appContext.preferencesDataStore.edit { it[CONVERSATION_CONTEXT_KEY] = contextJson }
    }

    suspend fun clearUserData() {
        dao.clearMessages()
        dao.clearDocuments()
        dao.clearTasks()
        appContext.preferencesDataStore.edit { preferences ->
            preferences.remove(EMAIL_KEY)
            preferences.remove(TOKEN_KEY)
            preferences.remove(PENDING_SHARE_KEY)
            preferences.remove(DRAFT_KEY)
            preferences.remove(REVIEW_KEY)
            preferences.remove(REPLY_ERROR_KEY)
            preferences.remove(ANSWERED_KEY)
            preferences.remove(PROFILE_KEY)
            preferences.remove(CONVERSATION_CONTEXT_KEY)
            preferences.remove(DATA_OWNER_EMAIL_KEY)
        }
    }

    private companion object {
        val EMAIL_KEY = stringPreferencesKey("signed_in_email")
        val DATA_OWNER_EMAIL_KEY = stringPreferencesKey("data_owner_email")
        val PENDING_SHARE_KEY = stringPreferencesKey("pending_shared_text")
        val TOKEN_KEY = stringPreferencesKey("device_token")
        val PROFILE_KEY = stringPreferencesKey("candidate_profile")
        val CONVERSATION_CONTEXT_KEY = stringPreferencesKey("conversation_context")
        val REPLY_ERROR_KEY = stringPreferencesKey("reply_error")
        val SIGN_IN_NOTICE_KEY = stringPreferencesKey("sign_in_notice")
        val ANSWERED_KEY = stringPreferencesKey("answered_review_lines")
        const val ANSWERED_SEPARATOR = "\u0001"
        val PROMPTED_KEY = booleanPreferencesKey("notification_prompt_shown")
        val REVIEW_KEY = stringPreferencesKey("pending_review")
        val DRAFT_KEY = stringPreferencesKey("draft_text")
        val THEME_KEY = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")
    }
}
