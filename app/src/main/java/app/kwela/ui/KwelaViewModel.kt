package app.kwela.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.kwela.KwelaApplication
import app.kwela.data.AttachmentReader
import app.kwela.data.remote.ApiResult
import app.kwela.domain.AttachmentPolicy
import app.kwela.domain.CareerReplyParser
import app.kwela.domain.SignInReplyParser
import app.kwela.domain.SignInCodeRequestParser
import app.kwela.domain.ChatRequestBuilder
import app.kwela.domain.OutgoingMessage
import app.kwela.domain.OutgoingRole
import app.kwela.data.local.ApplicationTaskEntity
import app.kwela.data.local.ChatMessageEntity
import app.kwela.data.local.SavedDocumentEntity
import app.kwela.domain.ApprovalPlanner
import app.kwela.domain.RecoveryAction
import app.kwela.domain.ReviewDecision
import app.kwela.domain.SharedPayloadParser
import app.kwela.domain.TaskRecoveryPlanner
import app.kwela.domain.TaskRunSnapshot
import app.kwela.domain.TaskStatus
import app.kwela.domain.ThemeMode
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class KwelaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as KwelaApplication).repository
    private val api = (application as KwelaApplication).api
    private val mutableState = MutableStateFlow(KwelaUiState())
    val state: StateFlow<KwelaUiState> = mutableState.asStateFlow()
    private var taskJob: Job? = null
    private val finishLock = Mutex()

    init {
        viewModelScope.launch {
            combine(
                repository.signedInEmail,
                repository.messages,
                repository.tasks,
                repository.documents,
                repository.notificationsEnabled,
                repository.pendingSharedText,
                repository.themeMode
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                mutableState.value.copy(
                    isRestoring = false,
                    email = values[0] as String?,
                    screen = if (values[0] == null) AppScreen.CHAT else mutableState.value.screen,
                    messages = values[1] as List<ChatMessageEntity>,
                    tasks = values[2] as List<ApplicationTaskEntity>,
                    documents = values[3] as List<SavedDocumentEntity>,
                    notificationsEnabled = values[4] as Boolean,
                    draftText = (values[5] as String?) ?: mutableState.value.draftText,
                    themeMode = values[6] as ThemeMode
                )
            }.collect { next ->
                mutableState.value = next
                recoverActiveTask(next.tasks)
            }
        }
    }

    fun setScreen(screen: AppScreen) { mutableState.value = mutableState.value.copy(screen = screen) }
    fun setDraft(text: String) { mutableState.value = mutableState.value.copy(draftText = text) }
    fun setPendingEmail(email: String) { mutableState.value = mutableState.value.copy(pendingEmail = email, loginError = null, loginTestCode = null) }

    fun requestOtp() {
        val email = mutableState.value.pendingEmail.trim()
        if (!email.contains('@') || !email.substringAfter('@').contains('.')) {
            mutableState.value = mutableState.value.copy(loginError = "Enter a valid email address.")
            return
        }
        if (mutableState.value.isSigningIn) return
        mutableState.value = mutableState.value.copy(isSigningIn = true, loginError = null)
        viewModelScope.launch {
            val result = api.requestCode(email)
            val success = result as? ApiResult.Success
            mutableState.value = mutableState.value.copy(
                isSigningIn = false,
                otpRequested = success != null,
                loginTestCode = success?.let { SignInCodeRequestParser.parseTestCode(it.body) },
                loginError = (result as? ApiResult.Failure)?.message,
                message = if (result is ApiResult.Success && mutableState.value.otpRequested) "We sent a new code." else null
            )
        }
    }

    fun verifyOtp(code: String) {
        if (mutableState.value.isSigningIn) return
        mutableState.value = mutableState.value.copy(isSigningIn = true, loginError = null)
        viewModelScope.launch {
            val result = api.verifyCode(mutableState.value.pendingEmail.trim(), code.trim())
            val signedIn = (result as? ApiResult.Success)?.let { SignInReplyParser.parse(it.body) }
            if (signedIn == null) {
                mutableState.value = mutableState.value.copy(
                    isSigningIn = false,
                    loginError = (result as? ApiResult.Failure)?.message ?: "Kwela sent a reply the app couldn’t read. Please try again."
                )
                return@launch
            }
            taskJob?.cancel()
            taskJob = null
            val changedAccount = repository.signIn(signedIn.email, signedIn.token)
            mutableState.value = mutableState.value.copy(
                isSigningIn = false,
                otpRequested = false,
                loginTestCode = null,
                pendingEmail = "",
                screen = AppScreen.CHAT,
                draftText = if (changedAccount) "" else mutableState.value.draftText,
                pendingAttachment = if (changedAccount) null else mutableState.value.pendingAttachment,
                replyError = null
            )
            repository.pendingSharedText.first()?.let { shared ->
                repository.savePendingSharedText(null)
                setDraft(shared)
            }
        }
    }

    fun backToEmail() { mutableState.value = mutableState.value.copy(otpRequested = false, loginTestCode = null, loginError = null) }

    fun sendDraft() {
        val current = mutableState.value
        val text = current.draftText.trim()
        val attachment = current.pendingAttachment
        if ((text.isBlank() && attachment == null) || current.isReplying) return
        mutableState.value = current.copy(draftText = "", pendingAttachment = null, replyError = null)
        viewModelScope.launch {
            repository.saveMessage(
                ChatMessageEntity(UUID.randomUUID().toString(), "USER", text.ifBlank { "Please review my file." }, System.currentTimeMillis(), attachment?.name, attachment?.uri)
            )
            requestReply()
        }
    }

    fun retryReply() {
        if (mutableState.value.isReplying) return
        viewModelScope.launch { requestReply() }
    }

    fun removeAttachment() { mutableState.value = mutableState.value.copy(pendingAttachment = null) }

    fun messageShown() { mutableState.value = mutableState.value.copy(message = null) }

    private suspend fun requestReply() {
        mutableState.value = mutableState.value.copy(isReplying = true, replyError = null)
        val failure = runCatching { fetchReply() }.getOrElse { if (it is CancellationException) throw it else "Something went wrong. Please try again." }
        mutableState.value = mutableState.value.copy(isReplying = false, replyError = failure)
    }

    /** Sends the conversation to the Kwela API and saves the reply. Returns an error message, or null on success. */
    private suspend fun fetchReply(): String? {
        val reader = AttachmentReader(getApplication())
        val recent = repository.messages.first().takeLast(20)
        val newestUserId = recent.lastOrNull { it.role == "USER" }?.id
        val history = withContext(Dispatchers.IO) {
            recent.map { message ->
                // Only the newest message carries its file. Earlier files are already summarised in the saved CV profile.
                val uri = message.attachmentUri.takeIf { message.id == newestUserId }
                val file = uri?.let { reader.read(message.id, Uri.parse(it)) }
                if (uri != null && file == null) return@withContext null
                OutgoingMessage(message.id, if (message.role == "USER") OutgoingRole.USER else OutgoingRole.ASSISTANT, message.content, listOfNotNull(file), message.applicationJson)
            }
        } ?: return "Kwela couldn’t read your attached file. Attach it again."
        val token = repository.sessionToken() ?: return expireSession()
        val body = ChatRequestBuilder.build(history, repository.candidateProfile.first())
        val result = api.chat(token, body)
        if (result is ApiResult.Failure) return if (result.status == 401) expireSession() else result.message
        val reply = CareerReplyParser.parse((result as ApiResult.Success).body) ?: return "Kwela sent a reply the app couldn’t read. Please try again."
        val now = System.currentTimeMillis()
        reply.documents.forEachIndexed { index, document ->
            repository.saveDocument(SavedDocumentEntity(UUID.randomUUID().toString(), document.title, document.body, now + index))
        }
        val filesNote = if (reply.documents.isEmpty()) "" else "\n\nSaved to Files: " + reply.documents.joinToString(", ") { it.title } + "."
        repository.saveMessage(ChatMessageEntity(UUID.randomUUID().toString(), "ASSISTANT", reply.message + filesNote, now + reply.documents.size, applicationJson = reply.applicationJson))
        reply.candidateProfileJson?.let { profile ->
            repository.saveCandidateProfile(profile)
            api.saveProfile(token, profile)
        }
        return null
    }

    /** Ends a session the server no longer accepts and sends the person back to sign in. */
    private suspend fun expireSession(): String? {
        repository.signOut()
        mutableState.value = mutableState.value.copy(message = "Please sign in again.", screen = AppScreen.CHAT)
        return null
    }

    fun sendSuggestion(prompt: String) { setDraft(prompt); sendDraft() }

    fun attachFile(uriString: String) {
        val file = AttachmentReader(getApplication()).describe(Uri.parse(uriString))
        val problem = when {
            !AttachmentPolicy.isSupported(file.name, file.mediaType) -> "Kwela can read PDF, Word, text and photo files."
            file.sizeBytes > 0 && !AttachmentPolicy.isSmallEnough(file.sizeBytes) -> "That file is over 5 MB."
            else -> null
        }
        mutableState.value = if (problem != null) mutableState.value.copy(message = problem)
        else mutableState.value.copy(pendingAttachment = PendingAttachment(file.name, uriString))
    }

    fun acceptSharedText(rawText: String?) {
        val payload = SharedPayloadParser.parse(rawText) ?: return
        if (mutableState.value.email == null) {
            viewModelScope.launch { repository.savePendingSharedText(payload.value) }
            mutableState.value = mutableState.value.copy(screen = AppScreen.CHAT)
        } else {
            setDraft(payload.value)
            setScreen(AppScreen.CHAT)
        }
    }

    fun startTestTask() {
        val current = mutableState.value.tasks
        val busy = current.any { it.status == "RUNNING" }
        val now = System.currentTimeMillis()
        val task = ApplicationTaskEntity(
            id = UUID.randomUUID().toString(), title = "Test application pack", prompt = "Create a CV and cover letter",
            status = if (busy) "QUEUED" else "RUNNING", startedAt = now,
            completesAt = now + if (busy) 0L else TASK_DURATION, updatedAt = now
        )
        viewModelScope.launch {
            repository.saveTask(task)
            repository.saveMessage(ChatMessageEntity(UUID.randomUUID().toString(), "ASSISTANT",
                if (busy) "Test task queued. The earlier task will finish first." else "Test task started. It takes about 20 seconds.", System.currentTimeMillis()))
            if (!busy) scheduleTask(task)
        }
    }

    fun cancelTask(task: ApplicationTaskEntity) {
        viewModelScope.launch {
            val current = repository.getTask(task.id) ?: return@launch
            if (current.status != "RUNNING" && current.status != "QUEUED") return@launch
            if (current.status == "RUNNING") taskJob?.cancel()
            repository.saveTask(current.copy(status = "CANCELLED", updatedAt = System.currentTimeMillis()))
            if (current.status == "RUNNING") startNextQueued()
        }
    }

    fun review(task: ApplicationTaskEntity, decision: ReviewDecision) {
        viewModelScope.launch {
            val current = repository.getTask(task.id) ?: return@launch
            val before = TaskStatus.valueOf(current.status)
            val after = ApprovalPlanner.resolve(before, decision)
            if (after == before) return@launch
            val now = System.currentTimeMillis()
            repository.saveTask(current.copy(status = after.name, updatedAt = now))
            repository.saveMessage(ChatMessageEntity(UUID.randomUUID().toString(), "ASSISTANT",
                if (decision == ReviewDecision.APPROVE) "Approved." else "Okay, I won’t send it.", now))
        }
    }

    fun setThemeMode(mode: ThemeMode) { viewModelScope.launch { repository.setThemeMode(mode) } }

    fun setNotificationsEnabled(enabled: Boolean) { viewModelScope.launch { repository.setNotificationsEnabled(enabled) } }

    fun signOut() {
        viewModelScope.launch {
            repository.sessionToken()?.let { api.signOut(it) }
            repository.signOut()
            mutableState.value = mutableState.value.copy(screen = AppScreen.CHAT)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val token = repository.sessionToken()
            val remote = if (token != null) api.deleteAccount(token) else null
            taskJob?.cancel()
            repository.clearUserData()
            mutableState.value = KwelaUiState(
                isRestoring = false,
                message = if (remote is ApiResult.Failure) "Removed from this phone. Kwela couldn’t be reached to delete your account, so try again later or use the deletion page on our website." else null
            )
        }
    }

    private fun recoverActiveTask(tasks: List<ApplicationTaskEntity>) {
        val running = tasks.firstOrNull { it.status == "RUNNING" } ?: return
        if (taskJob?.isActive == true) return
        val action = TaskRecoveryPlanner.plan(TaskRunSnapshot(running.id, TaskStatus.RUNNING, running.completesAt), System.currentTimeMillis())
        when (action) {
            is RecoveryAction.ResumeAfter -> scheduleTask(running, action.delayMillis)
            RecoveryAction.CompleteNow -> viewModelScope.launch { finishTask(running) }
            RecoveryAction.NoOp -> Unit
        }
    }

    private fun scheduleTask(task: ApplicationTaskEntity, delayMillis: Long = (task.completesAt - System.currentTimeMillis()).coerceAtLeast(0)) {
        if (taskJob?.isActive == true) return
        taskJob = viewModelScope.launch {
            delay(delayMillis)
            finishTask(task)
        }
    }

    private suspend fun finishTask(task: ApplicationTaskEntity) = finishLock.withLock {
        if (repository.getTask(task.id)?.status != "RUNNING") return@withLock
        val now = System.currentTimeMillis()
        repository.saveTask(task.copy(status = ApprovalPlanner.afterRun(TaskStatus.RUNNING).name, updatedAt = now))
        repository.saveDocument(SavedDocumentEntity(UUID.randomUUID().toString(), "Test CV.pdf", TEST_CV, now))
        repository.saveDocument(SavedDocumentEntity(UUID.randomUUID().toString(), "Test cover letter.pdf", TEST_LETTER, now + 1))
        repository.saveMessage(ChatMessageEntity(UUID.randomUUID().toString(), "ASSISTANT",
            "Your test application pack is ready in Files. Open my profile to approve or deny it.", now + 2))
        postTaskNotification()
        taskJob = null
        startNextQueued()
    }

    private suspend fun startNextQueued() {
        val next = repository.getNextQueuedTask() ?: return
        val now = System.currentTimeMillis()
        val running = next.copy(status = "RUNNING", startedAt = now, completesAt = now + TASK_DURATION, updatedAt = now)
        repository.saveTask(running)
        scheduleTask(running)
    }

    private suspend fun postTaskNotification() {
        val context = getApplication<Application>()
        if (repository.notificationsEnabled.first().not()) return
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channelId = "kwela_task_updates"
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(channelId, "Task updates", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            android.app.Notification.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Kwela needs your review")
                .setContentText("Your test application pack is ready.")
                .setAutoCancel(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Kwela needs your review")
                .setContentText("Your test application pack is ready.")
                .setAutoCancel(true)
                .build()
        }
        manager.notify(1001, notification)
    }

    private companion object {
        const val TASK_DURATION = 20_000L
        const val TEST_CV = "TEST DOCUMENT\n\nAmina Example\nNairobi, Kenya · amina@example.test\n\nPROFILE\nRecent graduate interested in an internship. This sample contains no verified personal history.\n\nEDUCATION\nExample University · Bachelor’s degree · 2022–2026\n\nSKILLS\nCommunication · Research · Microsoft Office\n\nEXPERIENCE\nAdd your actual experience here. Kwela must not invent qualifications."
        const val TEST_LETTER = "TEST DOCUMENT\n\nDear Hiring Team,\n\nI am writing to express interest in the internship opportunity. Please replace this paragraph with specific, truthful experience and explain why the role fits your goals.\n\nYours sincerely,\nAmina Example"
    }
}
