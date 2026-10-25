package app.kwela.ui

import app.kwela.data.local.ApplicationTaskEntity
import app.kwela.data.local.ChatMessageEntity
import app.kwela.data.local.SavedDocumentEntity
import app.kwela.domain.ReviewFinding
import app.kwela.domain.ThemeMode

/** A file the user picked that will be sent with their next message. */
data class PendingAttachment(val name: String, val uri: String)

/** Asked before a CV goes out with details missing. */
data class DownloadGate(val documentId: String, val gaps: List<String>, val share: Boolean = false)

data class KwelaUiState(
    val isRestoring: Boolean = true,
    val email: String? = null,
    val pendingEmail: String = "",
    val otpRequested: Boolean = false,
    val loginTestCode: String? = null,
    val isSigningIn: Boolean = false,
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
    val replyIsSlow: Boolean = false,
    val replyError: String? = null,
    val message: String? = null,
    val pendingReview: ReviewFinding? = null,
    val downloadGate: DownloadGate? = null,
    val downloadNowId: String? = null,
    val downloadNowShare: Boolean = false,
    /** A file the person asked to see in the chat. The screen opens it, then clears this. */
    val openDocumentId: String? = null
) {
    val activeTask: ApplicationTaskEntity?
        get() = tasks.firstOrNull { it.status == "RUNNING" || it.status == "WAITING_FOR_USER" || it.status == "QUEUED" }
}
