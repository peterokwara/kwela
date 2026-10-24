package app.kwela.ui

import app.kwela.data.local.ApplicationTaskEntity
import app.kwela.data.local.ChatMessageEntity
import app.kwela.data.local.SavedDocumentEntity
import app.kwela.domain.ThemeMode

/** A file the user picked that will be sent with their next message. */
data class PendingAttachment(val name: String, val uri: String)

data class KwelaUiState(
    val isRestoring: Boolean = true,
    val email: String? = null,
    val pendingEmail: String = "",
    val otpRequested: Boolean = false,
    val screen: AppScreen = AppScreen.CHAT,
    val draftText: String = "",
    val messages: List<ChatMessageEntity> = emptyList(),
    val tasks: List<ApplicationTaskEntity> = emptyList(),
    val documents: List<SavedDocumentEntity> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val loginError: String? = null,
    val pendingAttachment: PendingAttachment? = null,
    val isReplying: Boolean = false,
    val replyError: String? = null,
    val message: String? = null
) {
    val activeTask: ApplicationTaskEntity?
        get() = tasks.firstOrNull { it.status == "RUNNING" || it.status == "WAITING_FOR_USER" || it.status == "QUEUED" }
}
