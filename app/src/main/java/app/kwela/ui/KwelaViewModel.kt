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
import kotlinx.coroutines.Job
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
                KwelaUiState(
                    isRestoring = false,
                    email = values[0] as String?,
                    screen = if (values[0] == null) AppScreen.CHAT else mutableState.value.screen,
                    messages = values[1] as List<ChatMessageEntity>,
                    tasks = values[2] as List<ApplicationTaskEntity>,
                    documents = values[3] as List<SavedDocumentEntity>,
                    notificationsEnabled = values[4] as Boolean,
                    draftText = (values[5] as String?) ?: mutableState.value.draftText,
                    themeMode = values[6] as ThemeMode,
                    pendingEmail = mutableState.value.pendingEmail,
                    otpRequested = mutableState.value.otpRequested,
                    loginError = mutableState.value.loginError
                )
            }.collect { next ->
                mutableState.value = next
                recoverActiveTask(next.tasks)
            }
        }
    }

    fun setScreen(screen: AppScreen) { mutableState.value = mutableState.value.copy(screen = screen) }
    fun setDraft(text: String) { mutableState.value = mutableState.value.copy(draftText = text) }
    fun setPendingEmail(email: String) { mutableState.value = mutableState.value.copy(pendingEmail = email, loginError = null) }

    fun requestOtp() {
        val email = mutableState.value.pendingEmail.trim()
        if (!email.contains('@') || !email.substringAfter('@').contains('.')) {
            mutableState.value = mutableState.value.copy(loginError = "Enter a valid email address.")
            return
        }
        mutableState.value = mutableState.value.copy(otpRequested = true, loginError = null)
    }

    fun verifyOtp(code: String) {
        if (code != "123456") {
            mutableState.value = mutableState.value.copy(loginError = "That code isn’t right. Try again.")
            return
        }
        viewModelScope.launch {
            repository.signIn(mutableState.value.pendingEmail.trim())
            mutableState.value = mutableState.value.copy(otpRequested = false, pendingEmail = "", screen = AppScreen.CHAT)
            repository.pendingSharedText.first()?.let { shared ->
                repository.savePendingSharedText(null)
                setDraft(shared)
            }
        }
    }

    fun backToEmail() { mutableState.value = mutableState.value.copy(otpRequested = false, loginError = null) }

    fun sendDraft() {
        val text = mutableState.value.draftText.trim()
        if (text.isBlank()) return
        mutableState.value = mutableState.value.copy(draftText = "")
        viewModelScope.launch {
            repository.saveMessage(ChatMessageEntity(UUID.randomUUID().toString(), "USER", text, System.currentTimeMillis()))
            repository.saveMessage(
                ChatMessageEntity(UUID.randomUUID().toString(), "ASSISTANT",
                    "Thanks, I’ve saved your message.", System.currentTimeMillis() + 1)
            )
        }
    }

    fun sendSuggestion(prompt: String) { setDraft(prompt); sendDraft() }

    fun attachFile(name: String, uri: String) {
        viewModelScope.launch {
            repository.saveMessage(ChatMessageEntity(UUID.randomUUID().toString(), "USER", "Attached a file.", System.currentTimeMillis(), name, uri))
        }
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
        viewModelScope.launch { repository.signOut(); mutableState.value = mutableState.value.copy(screen = AppScreen.CHAT) }
    }

    fun deleteLocalData() {
        taskJob?.cancel()
        viewModelScope.launch { repository.clearUserData(); mutableState.value = KwelaUiState(isRestoring = false) }
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
