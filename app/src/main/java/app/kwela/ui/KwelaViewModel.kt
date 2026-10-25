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
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.kwela.data.AttachmentReader
import app.kwela.data.ReplyScheduler
import app.kwela.data.ReplyWorker
import app.kwela.data.remote.ApiResult
import app.kwela.domain.AttachmentPolicy
import app.kwela.domain.CareerReplyParser
import app.kwela.domain.ChangeRequest
import app.kwela.domain.ContactGaps
import app.kwela.domain.ReviewFinding
import app.kwela.domain.ReviewPrompts
import app.kwela.domain.ShowRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import app.kwela.domain.LocalMessages
import app.kwela.domain.ReplyPacing
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
        restoreDraft()
        observePendingReview()
        observeReplyWork()
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

    /** Mirrors the background reply job, so the chat knows whether the writer is working. The error and sign-in note come from storage. */
    private fun observeReplyWork() {
        viewModelScope.launch {
            WorkManager.getInstance(getApplication<Application>()).getWorkInfosForUniqueWorkFlow(ReplyWorker.UNIQUE).collect { infos ->
                val info = infos.firstOrNull { !it.state.isFinished }
                mutableState.value = mutableState.value.copy(isReplying = info != null, replyIsSlow = info != null && ReplyWorker.TAG_SLOW in info.tags)
            }
        }
        viewModelScope.launch { repository.replyError.collect { error -> mutableState.value = mutableState.value.copy(replyError = error) } }
        viewModelScope.launch {
            repository.signInNotice.collect { notice ->
                if (notice != null) {
                    mutableState.value = mutableState.value.copy(message = notice)
                    repository.setSignInNotice(null)
                }
            }
        }
    }

    /** The writer's quick question is kept on disk, so it is still there when the person comes back. */
    private fun observePendingReview() {
        viewModelScope.launch {
            repository.pendingReviewJson.collect { json ->
                mutableState.value = mutableState.value.copy(pendingReview = json?.let { runCatching { Json.decodeFromString<ReviewFinding>(it) }.getOrNull() })
            }
        }
    }

    private fun restoreDraft() {
        viewModelScope.launch {
            val saved = repository.draft()
            if (saved.isNotEmpty() && mutableState.value.draftText.isEmpty()) mutableState.value = mutableState.value.copy(draftText = saved)
        }
    }

    fun setScreen(screen: AppScreen) { mutableState.value = mutableState.value.copy(screen = screen) }
    private var draftSave: Job? = null

    fun setDraft(text: String) {
        mutableState.value = mutableState.value.copy(draftText = text)
        draftSave?.cancel()
        draftSave = viewModelScope.launch { delay(400); repository.saveDraft(text) }
    }
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
        draftSave?.cancel()
        mutableState.value = current.copy(draftText = "", pendingAttachment = null)
        val wanted = if (attachment == null) ShowRequest.parse(text) else null
        if (wanted != null) { showFile(text, wanted); return }
        sendText(text.ifBlank { "Please review my file." }, attachment)
    }

    /** Sends a message to the writer without touching what the person is typing. A newer message replaces a reply still being written. */
    private fun sendText(text: String, attachment: PendingAttachment? = null) {
        val slow = ReplyPacing.isSlow(text, attachment != null)
        mutableState.value = mutableState.value.copy(replyError = null, pendingReview = null, downloadGate = null, isReplying = true, replyIsSlow = slow)
        viewModelScope.launch {
            repository.saveDraftNow(mutableState.value.draftText)
            repository.savePendingReview(null)
            repository.setReplyError(null)
            repository.saveMessage(ChatMessageEntity(UUID.randomUUID().toString(), "USER", text, System.currentTimeMillis(), attachment?.name, attachment?.uri))
            ReplyScheduler.enqueue(getApplication(), slow)
        }
    }

    /** "Let me see the CV": answered right here, with no wait. The file comes back as a card in the chat, ready to open. */
    private fun showFile(said: String, wanted: ShowRequest) {
        mutableState.value = mutableState.value.copy(replyError = null)
        viewModelScope.launch {
            repository.saveDraftNow(mutableState.value.draftText)
            val now = System.currentTimeMillis()
            repository.saveMessage(ChatMessageEntity(LocalMessages.PREFIX + UUID.randomUUID(), "USER", said, now))
            val document = mutableState.value.documents.filter { wanted.matches(it.kind, it.title) }.maxByOrNull { it.createdAt }
            if (document == null) {
                repository.sayLocally(
                    if (wanted == ShowRequest.JOB) "I don’t have a job to show yet.\n\nSend me the ad or its link and I’ll break it down."
                    else "I haven’t made a ${wanted.label} for you yet.\n\nTell me which job it’s for and I’ll start."
                )
            } else {
                repository.sayLocally("Here’s your ${wanted.label}${document.groupTitle?.let { " for $it" }.orEmpty()}.", document.id)
            }
        }
    }

    fun retryReply() {
        if (mutableState.value.isReplying) return
        viewModelScope.launch {
            val last = repository.messages.first().lastOrNull { !LocalMessages.isLocal(it.id) && it.role == "USER" }
            val slow = last != null && ReplyPacing.isSlow(last.content, last.attachmentUri != null)
            mutableState.value = mutableState.value.copy(isReplying = true, replyIsSlow = slow, replyError = null)
            repository.setReplyError(null)
            ReplyScheduler.enqueue(getApplication(), slow)
        }
    }

    fun removeAttachment() { mutableState.value = mutableState.value.copy(pendingAttachment = null) }

    suspend fun claimNotificationPrompt(): Boolean = repository.claimNotificationPrompt()

    fun messageShown() { mutableState.value = mutableState.value.copy(message = null) }

    fun sendSuggestion(prompt: String) = sendText(prompt)

    /** The person tapped one of the quick answers under the writer's question. */
    fun answerReview(option: String) {
        val finding = mutableState.value.pendingReview ?: return
        viewModelScope.launch { repository.addAnsweredReviewLine(finding.line) }
        sendText(ReviewPrompts.answer(finding, option))
    }

    /** The person tapped a line in a document and said what should change. */
    fun sendChangeRequest(documentLabel: String, line: String, note: String) {
        mutableState.value = mutableState.value.copy(screen = AppScreen.CHAT)
        sendText(ChangeRequest.message(documentLabel, line, note))
    }

    private val skippedChecks = mutableSetOf<String>()

    /** True when this document has already been waved through the missing-details check. */
    fun checkSkipped(documentId: String): Boolean = documentId in skippedChecks

    /** A CV is about to go out with details missing. Ask first, in the chat, like a friend would. */
    fun askBeforeDownload(documentId: String, gaps: List<String>, share: Boolean = false) {
        viewModelScope.launch {
            repository.savePendingReview(null)
            repository.sayLocally("Quick check before this goes out: I don’t have ${ContactGaps.sentence(gaps)} on it. Want to add ${if (gaps.size == 1) "it" else "them"}?")
            mutableState.value = mutableState.value.copy(screen = AppScreen.CHAT, downloadGate = DownloadGate(documentId, gaps, share), pendingReview = null)
        }
    }

    fun addMissingDetails() {
        val gate = mutableState.value.downloadGate ?: return
        sendText("Let me add ${ContactGaps.sentence(gate.gaps).removePrefix("your ")}")
    }

    fun downloadAnyway() {
        val gate = mutableState.value.downloadGate ?: return
        skippedChecks += gate.documentId
        mutableState.value = mutableState.value.copy(downloadGate = null, downloadNowId = gate.documentId, downloadNowShare = gate.share)
    }

    fun downloadHandled() { mutableState.value = mutableState.value.copy(downloadNowId = null, downloadNowShare = false) }

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
            WorkManager.getInstance(getApplication<Application>()).cancelUniqueWork(ReplyWorker.UNIQUE)
            repository.sessionToken()?.let { api.signOut(it) }
            repository.signOut()
            mutableState.value = mutableState.value.copy(screen = AppScreen.CHAT)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            WorkManager.getInstance(getApplication<Application>()).cancelUniqueWork(ReplyWorker.UNIQUE)
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
