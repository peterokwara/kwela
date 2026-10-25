package app.kwela

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.kwela.ui.PendingAttachment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import app.kwela.domain.ActivityTimeline
import app.kwela.domain.ChatTimeHeader
import app.kwela.domain.DocumentSummary
import app.kwela.domain.ReviewDecision
import app.kwela.domain.TaskStatus
import app.kwela.domain.ThemeMode
import app.kwela.domain.TaskSummary
import app.kwela.ui.KwelaUiState
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.kwela.data.local.ApplicationTaskEntity
import app.kwela.data.local.ChatMessageEntity
import app.kwela.data.local.SavedDocumentEntity
import app.kwela.ui.AppScreen
import app.kwela.ui.KwelaViewModel
import app.kwela.ui.startingMoments
import app.kwela.ui.ArtifactCard
import app.kwela.ui.DocumentActions
import app.kwela.ui.DocumentPreviewScreen
import app.kwela.ui.FilesScreen
import app.kwela.ui.rememberDocumentActions
import app.kwela.domain.ChangeRequest
import app.kwela.domain.ContactGaps
import app.kwela.domain.ReviewFinding
import app.kwela.ui.DownloadGate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import app.kwela.domain.BubbleSplitter
import app.kwela.domain.ConversationResume
import app.kwela.domain.LocalMessages
import app.kwela.ui.theme.KwelaTheme
import java.io.OutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) receiveSharedIntent(intent)
        setContent { KwelaApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveSharedIntent(intent)
    }

    fun receiveSharedIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        }
    }

    companion object {
        var sharedText by mutableStateOf<String?>(null)
    }
}

@Composable
private fun KwelaApp(vm: KwelaViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val shared = MainActivity.sharedText
    LaunchedEffect(shared) {
        if (shared != null) {
            vm.acceptSharedText(shared)
            MainActivity.sharedText = null
        }
    }
    LaunchedEffect(state.message) {
        state.message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); vm.messageShown() }
    }
    KwelaTheme(darkTheme = state.themeMode.useDark(isSystemInDarkTheme())) {
        val colors = MaterialTheme.colorScheme
        val view = LocalView.current
        val window = (context as? Activity)?.window
        SideEffect {
            window?.let {
                it.statusBarColor = colors.surface.toArgb()
                it.navigationBarColor = colors.surface.toArgb()
                WindowCompat.getInsetsController(it, view).apply {
                    val useDarkIcons = colors.surface.luminance() > 0.5f
                    isAppearanceLightStatusBars = useDarkIcons
                    isAppearanceLightNavigationBars = useDarkIcons
                }
            }
        }
        Surface(Modifier.fillMaxSize(), color = colors.surface) {
            when {
                state.isRestoring -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                state.email == null -> LoginScreen(state.pendingEmail, state.otpRequested, state.loginTestCode, state.isSigningIn, state.loginError, vm::setPendingEmail, vm::requestOtp, vm::verifyOtp, vm::backToEmail)
                else -> {
                    var profileTab by rememberSaveable { mutableStateOf(ProfileTab.ACTIVITY) }
                    val reviewCount = state.tasks.count { it.taskStatus() == TaskStatus.WAITING_FOR_USER }
                    val openProfile = { profileTab = ProfileTab.ACTIVITY; vm.setScreen(AppScreen.PROFILE) }
                    var previewId by rememberSaveable { mutableStateOf<String?>(null) }
                    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
                    // Ask once, at the moment it makes sense: the first time Kwela needs a minute and the person might wander off.
                    LaunchedEffect(state.isReplying, state.replyIsSlow) {
                        if (state.isReplying && state.replyIsSlow && state.notificationsEnabled && Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && vm.claimNotificationPrompt()
                        ) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    val baseActions = rememberDocumentActions { previewId = it.id }
                    // Before a CV goes out with details missing, Kwela asks in the chat, like a friend would.
                    fun checked(share: Boolean, action: (SavedDocumentEntity) -> Unit): (SavedDocumentEntity) -> Unit = { document ->
                        val gaps = if (document.kind == "resume") ContactGaps.of(document.sourceJson) else emptyList()
                        if (gaps.isNotEmpty() && !vm.checkSkipped(document.id)) { previewId = null; vm.askBeforeDownload(document.id, gaps, share) } else action(document)
                    }
                    val docActions = DocumentActions(baseActions.open, checked(false, baseActions.download), checked(true, baseActions.share), baseActions.busyDocumentId)
                    val previewDocument = state.documents.firstOrNull { it.id == previewId }
                    LaunchedEffect(state.downloadNowId) {
                        state.downloadNowId?.let { id -> state.documents.firstOrNull { it.id == id }?.let(if (state.downloadNowShare) baseActions.share else baseActions.download); vm.downloadHandled() }
                    }
                    if (previewDocument != null) {
                        DocumentPreviewScreen(previewDocument, docActions, changeRequest = { line, note -> previewId = null; vm.sendChangeRequest(previewDocument.title, line, note) }) { previewId = null }
                    } else if (state.screen == AppScreen.PROFILE) {
                        ProfileScreen(state, profileTab, { profileTab = it }, { vm.setScreen(AppScreen.CHAT) }, vm::review, vm::setNotificationsEnabled, vm::setThemeMode, vm::signOut, vm::deleteAccount, vm::startTestTask)
                    } else MainShell(state.screen, vm::setScreen, reviewCount, openProfile) {
                        when (state.screen) {
                            AppScreen.CHAT, AppScreen.PROFILE -> ChatScreen(state.messages, state.tasks, state.draftText, vm::setDraft, vm::sendDraft, vm::sendSuggestion, vm::cancelTask, vm::attachFile, state.pendingAttachment, state.isReplying, state.replyIsSlow, state.replyError, vm::removeAttachment, vm::retryReply, state.documents, docActions, state.pendingReview, state.downloadGate, vm::answerReview, vm::addMissingDetails, vm::downloadAnyway)
                            AppScreen.FILES -> FilesScreen(state.documents, docActions)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(email: String, otp: Boolean, testCode: String?, working: Boolean, error: String?, onEmail: (String) -> Unit, request: () -> Unit, verify: (String) -> Unit, back: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var code by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp), verticalArrangement = Arrangement.Center) {
        BrandMark()
        Spacer(Modifier.height(24.dp))
        Text(if (otp) "Check your email" else "Welcome to Kwela", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(
            if (otp && testCode != null) "Private test code: $testCode. Enter it to continue."
            else if (otp) "We sent a 6-digit code to $email. Enter it to continue."
            else "Saw a job tonight? Be ready to apply in minutes. No blank page, no cyber café queue.",
            color = colors.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
        if (!otp) {
            OutlinedTextField(email, onEmail, Modifier.fillMaxWidth(), label = { Text("Email address") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            Spacer(Modifier.height(16.dp))
            Button(onClick = request, Modifier.fillMaxWidth().height(52.dp), enabled = !working, shape = CircleShape) { Text(if (working) "Sending code…" else "Continue") }
        } else {
            OutlinedTextField(code, { if (it.length <= 6 && it.all(Char::isDigit)) code = it }, Modifier.fillMaxWidth(), label = { Text("6-digit code") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
            Spacer(Modifier.height(16.dp))
            Button(onClick = { verify(code) }, Modifier.fillMaxWidth().height(52.dp), enabled = !working && code.length == 6, shape = CircleShape) { Text(if (working) "Checking…" else "Sign in") }
            Row {
                TextButton(onClick = back, enabled = !working) { Text("Use a different email") }
                TextButton(onClick = request, enabled = !working) { Text("Send a new code") }
            }
        }
        if (error != null) { Spacer(Modifier.height(8.dp)); Text(error, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(28.dp))
        Text("Your chats and files are saved on this device.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun BrandMark() {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(colors.primary, CircleShape), Alignment.Center) { Text("K", color = colors.onPrimary, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(12.dp)); Text("kwela", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
    }
}

private object LucideIcons {
    val Folder = com.composables.icons.lucide.R.drawable.lucide_ic_folder
    val MessageCircle = com.composables.icons.lucide.R.drawable.lucide_ic_message_circle
    val X = com.composables.icons.lucide.R.drawable.lucide_ic_x
}

private enum class ProfileTab(val label: String) { ACTIVITY("Activity"), REVIEW("Needs review"), SETTINGS("Settings") }

private fun ApplicationTaskEntity.taskStatus() = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.FAILED)

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MainShell(screen: AppScreen, select: (AppScreen) -> Unit, reviewCount: Int, openProfile: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(enabled = screen != AppScreen.CHAT) { select(AppScreen.CHAT) }
    val keyboardOpen = WindowInsets.isImeVisible
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { KwelaTopBar(reviewCount, openProfile) },
        bottomBar = { if (!keyboardOpen) KwelaTabBar(screen, select) }
    ) { padding -> Box(Modifier.padding(padding)) { content() } }
}

@Composable
private fun KwelaAvatar(size: Dp) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(size).background(colors.primaryContainer, CircleShape), Alignment.Center) {
        Text("K", color = colors.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.45f).sp)
    }
}

@Composable
private fun KwelaTopBar(reviewCount: Int, onAvatar: () -> Unit) {
    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).windowInsetsPadding(WindowInsets.statusBars).height(72.dp), Alignment.Center) {
        Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClickLabel = "Open profile", role = Role.Button, onClick = onAvatar).padding(horizontal = 16.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BadgedBox(badge = { if (reviewCount > 0) Badge() }) { KwelaAvatar(40.dp) }
            Text("Kwela", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private class Tab(val screen: AppScreen, val label: String, @DrawableRes val icon: Int)

private val tabs = listOf(
    Tab(AppScreen.CHAT, "Chat", LucideIcons.MessageCircle),
    Tab(AppScreen.FILES, "Files", LucideIcons.Folder)
)

@Composable
private fun KwelaTabBar(screen: AppScreen, select: (AppScreen) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().background(colors.surface).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Surface(shape = RoundedCornerShape(32.dp), color = colors.surfaceContainer) {
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEach { tab ->
                    val isSelected = tab.screen == screen
                    Box(Modifier.weight(1f).fillMaxHeight().clickable(remember { MutableInteractionSource() }, null, role = Role.Tab) { select(tab.screen) }.semantics { contentDescription = tab.label; selected = isSelected }, Alignment.Center) {
                        Box(Modifier.size(width = 64.dp, height = 44.dp).background(if (isSelected) colors.surfaceContainerHighest else Color.Transparent, CircleShape), Alignment.Center) {
                            Icon(painterResource(tab.icon), null, Modifier.size(24.dp), tint = colors.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    state: KwelaUiState, tab: ProfileTab, setTab: (ProfileTab) -> Unit, close: () -> Unit, review: (ApplicationTaskEntity, ReviewDecision) -> Unit,
    toggleNotifications: (Boolean) -> Unit, setThemeMode: (ThemeMode) -> Unit, signOut: () -> Unit, deleteData: () -> Unit, runTestTask: () -> Unit
) {
    BackHandler(onBack = close)
    val colors = MaterialTheme.colorScheme
    val waiting = state.tasks.filter { it.taskStatus() == TaskStatus.WAITING_FOR_USER }
    val working = state.tasks.any { it.taskStatus() == TaskStatus.RUNNING }
    Column(Modifier.fillMaxSize().background(colors.surface).windowInsetsPadding(WindowInsets.statusBars).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            IconButton(close, Modifier.size(56.dp).align(Alignment.CenterStart)) { Icon(painterResource(LucideIcons.X), "Close", Modifier.size(24.dp), tint = colors.onSurface) }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            KwelaAvatar(72.dp)
            Text("Kwela", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Text(if (working) "Working on your application" else if (waiting.isNotEmpty()) "Waiting for your review" else "Ready", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        }
        Surface(Modifier.fillMaxWidth().padding(16.dp), shape = CircleShape, color = colors.surfaceContainer) {
            Row(Modifier.padding(4.dp)) {
                ProfileTab.entries.forEach { entry ->
                    val label = if (entry == ProfileTab.REVIEW && waiting.isNotEmpty()) "${entry.label} (${waiting.size})" else entry.label
                    Box(Modifier.weight(1f).height(40.dp).clip(CircleShape).background(if (entry == tab) colors.surfaceContainerHighest else Color.Transparent).clickable(role = Role.Tab) { setTab(entry) }, Alignment.Center) {
                        Text(label, style = MaterialTheme.typography.labelLarge, color = colors.onSurface, maxLines = 1)
                    }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                ProfileTab.ACTIVITY -> ActivityList(state.tasks, state.documents)
                ProfileTab.REVIEW -> ReviewList(state.tasks, waiting, review)
                ProfileTab.SETTINGS -> SettingsScreen(state.email.orEmpty(), state.notificationsEnabled, toggleNotifications, state.themeMode, setThemeMode, signOut, deleteData, runTestTask)
            }
        }
    }
}

@Composable
private fun ActivityList(tasks: List<ApplicationTaskEntity>, documents: List<SavedDocumentEntity>) {
    val colors = MaterialTheme.colorScheme
    val now = remember { System.currentTimeMillis() }
    val entries = ActivityTimeline.build(tasks.map { TaskSummary(it.id, it.title, it.taskStatus(), it.updatedAt) }, documents.map { DocumentSummary(it.id, it.title, it.createdAt) })
    if (entries.isEmpty()) {
        Text("Nothing yet. What Kwela does for you will show up here.", Modifier.padding(horizontal = 20.dp), color = colors.onSurfaceVariant)
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(entries, key = { it.id }) { entry ->
            Column {
                Text(entry.title, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                Text(entry.detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Text(ChatTimeHeader.label(entry.at, now), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReviewList(tasks: List<ApplicationTaskEntity>, waiting: List<ApplicationTaskEntity>, review: (ApplicationTaskEntity, ReviewDecision) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val history = tasks.filter { it.taskStatus() == TaskStatus.COMPLETED || it.taskStatus() == TaskStatus.CANCELLED }.sortedByDescending { it.updatedAt }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (waiting.isEmpty()) item { Text("Nothing needs your review right now.", color = colors.onSurfaceVariant) }
        items(waiting, key = { it.id }) { task ->
            Surface(shape = RoundedCornerShape(24.dp), color = colors.surfaceContainer) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Send “${task.title}”?", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    Text("Kwela has finished and wants your OK before sending.", color = colors.onSurfaceVariant)
                    Button(onClick = { review(task, ReviewDecision.APPROVE) }, Modifier.fillMaxWidth()) { Text("Approve") }
                    FilledTonalButton(onClick = { review(task, ReviewDecision.DENY) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.surfaceContainerHighest, contentColor = colors.onSurface)) { Text("Deny") }
                }
            }
        }
        if (history.isNotEmpty()) {
            item { Text("History", Modifier.padding(top = 12.dp, start = 4.dp), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant) }
            item { GroupedList(history.map { ListRow(it.title, ActivityTimeline.describe(it.taskStatus())) }) }
        }
    }
}

@Composable
private fun ChatScreen(messages: List<ChatMessageEntity>, tasks: List<ApplicationTaskEntity>, draft: String, setDraft: (String) -> Unit, send: () -> Unit, suggestion: (String) -> Unit, cancelTask: (ApplicationTaskEntity) -> Unit, attach: (String) -> Unit, pendingAttachment: PendingAttachment?, isReplying: Boolean, slowWait: Boolean, replyError: String?, removeAttachment: () -> Unit, retry: () -> Unit, documents: List<SavedDocumentEntity>, docActions: DocumentActions, pendingReview: ReviewFinding?, downloadGate: DownloadGate?, answerReview: (String) -> Unit, addMissing: () -> Unit, downloadAnyway: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val now = remember { System.currentTimeMillis() }
    val listState = rememberLazyListState()
    val composerFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(messages.size, isReplying, replyError) { if (messages.isNotEmpty()) listState.animateScrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            attach(uri.toString())
        }
    }
    Column(Modifier.fillMaxSize()) {
        if (messages.isEmpty()) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item { KwelaBubble("Hi, I’m Kwela 👋") }
                item { KwelaBubble("I turn your CV and the job you want into an application you’re proud to send.") }
                item { KwelaBubble("What’s going on right now?") }
                items(startingMoments) { moment -> MomentChip(moment) { suggestion(moment) } }
                item { Spacer(Modifier.height(4.dp)); KwelaBubble("Or skip this and send me your CV (a photo works) and the job. I’ll take it from there.") }
            }
        } else {
            val newestWithGroup = remember(messages) { messages.filter { it.documentGroupId != null }.associate { it.documentGroupId to it.id } }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                    Column {
                        if (ChatTimeHeader.shouldShow(messages.getOrNull(index - 1)?.createdAt, message.createdAt)) {
                            Text(ChatTimeHeader.label(message.createdAt, now), Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        }
                        MessageBubble(message, documents, docActions, showsGroupCards = message.documentGroupId == null || newestWithGroup[message.documentGroupId] == message.id)
                    }
                }
                items(tasks.filter { it.status == "RUNNING" || it.status == "QUEUED" }) { task -> TaskCard(task, { cancelTask(task) }) }
                if (isReplying && !slowWait) item { TypingDots() }
                item { ChatChips(messages, isReplying, pendingReview, downloadGate, answerReview, addMissing, downloadAnyway, onRevision = { chip -> if (chip == "Something else") { composerFocus.requestFocus(); keyboard?.show() } else suggestion(chip) }) }
                if (ConversationResume.needsPickUp(LocalMessages.lastRealRole(messages.map { it.id to it.role }), isReplying, replyError != null)) item { PickUpCard(retry) }
                if (replyError != null) item { ReplyErrorCard(replyError, retry) }
            }
        }
        if (pendingAttachment != null) AttachmentChip(pendingAttachment.name, removeAttachment)
        Row(Modifier.fillMaxWidth().imePadding().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
            Row(Modifier.weight(1f).heightIn(min = 56.dp).background(colors.surfaceContainer, RoundedCornerShape(28.dp)), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { picker.launch(arrayOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain", "image/*")) }) { Icon(Icons.Outlined.AddCircleOutline, "Attach a CV", tint = colors.onSurfaceVariant) }
                Box(Modifier.weight(1f).padding(end = 16.dp, top = 8.dp, bottom = 8.dp)) {
                    if (draft.isEmpty()) Text("Message Kwela", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                    BasicTextField(draft, setDraft, Modifier.fillMaxWidth().focusRequester(composerFocus), textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface), cursorBrush = SolidColor(colors.primary), maxLines = 5)
                }
            }
            if ((draft.isNotBlank() || pendingAttachment != null) && !isReplying) {
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = send, Modifier.size(56.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer)) { Icon(Icons.AutoMirrored.Outlined.Send, "Send") }
            }
        }
    }
}

/** The tap answers that belong under the latest message: the missing-details check, the writer's quick question, or ways to change a finished document. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatChips(
    messages: List<ChatMessageEntity>, isReplying: Boolean, pendingReview: ReviewFinding?, gate: DownloadGate?,
    answerReview: (String) -> Unit, addMissing: () -> Unit, downloadAnyway: () -> Unit, onRevision: (String) -> Unit
) {
    if (isReplying) return
    val newest = messages.lastOrNull { !LocalMessages.isLocal(it.id) }
    val chips: List<Pair<String, () -> Unit>> = when {
        gate != null -> listOf("Add it" to addMissing, "Download anyway" to downloadAnyway)
        pendingReview != null -> pendingReview.options.map { option -> option to { answerReview(option) } }
        newest?.role == "ASSISTANT" && newest.documentGroupId != null -> ChangeRequest.chips.map { chip -> chip to { onRevision(chip) } }
        else -> emptyList()
    }
    if (chips.isEmpty()) return
    FlowRow(Modifier.fillMaxWidth().padding(start = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { (label, onTap) ->
            Surface(onClick = onTap, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                Text(label, Modifier.padding(horizontal = 16.dp, vertical = 12.dp), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun TypingDots() {
    val colors = MaterialTheme.colorScheme
    val phase by rememberInfiniteTransition(label = "typing").animateFloat(0f, 3f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "dots")
    Surface(shape = RoundedCornerShape(24.dp), color = colors.surfaceContainer, modifier = Modifier.padding(end = 56.dp).semantics { contentDescription = "Kwela is typing" }) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { index ->
                val on = phase.toInt() == index
                Box(Modifier.size(8.dp).background(colors.onSurfaceVariant.copy(alpha = if (on) 0.9f else 0.35f), CircleShape))
            }
        }
    }
}

@Composable
private fun PickUpCard(retry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(24.dp), color = colors.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Pick up where you left off", Modifier.weight(1f), color = colors.onSurface)
            TextButton(onClick = retry) { Text("Continue") }
        }
    }
}

@Composable
private fun KwelaBubble(text: String) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(end = 56.dp)) {
        Text(text, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ReplyErrorCard(message: String, retry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(24.dp), color = colors.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), color = colors.onErrorContainer)
            TextButton(onClick = retry) { Text("Try again", color = colors.onErrorContainer) }
        }
    }
}

@Composable
private fun AttachmentChip(name: String, remove: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceContainerHigh, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("📎 $name", Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
            IconButton(remove, Modifier.size(40.dp)) { Icon(painterResource(LucideIcons.X), "Remove file", Modifier.size(18.dp), tint = colors.onSurfaceVariant) }
        }
    }
}

@Composable
private fun MomentChip(label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, Modifier.fillMaxWidth().padding(start = 24.dp), shape = RoundedCornerShape(24.dp), color = colors.primaryContainer, contentColor = colors.onPrimaryContainer) {
        Text(label, Modifier.padding(horizontal = 18.dp, vertical = 16.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MessageBubble(message: ChatMessageEntity, documents: List<SavedDocumentEntity>, docActions: DocumentActions, showsGroupCards: Boolean) {
    val user = message.role == "USER"
    val colors = MaterialTheme.colorScheme
    val bubbles = if (user) listOf(message.content) else BubbleSplitter.split(message.content).ifEmpty { listOf(message.content) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        bubbles.forEachIndexed { index, text ->
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                Surface(
                    color = if (user) colors.primaryContainer else colors.surfaceContainer,
                    contentColor = if (user) colors.onPrimaryContainer else colors.onSurface,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.widthIn(max = maxWidth * 0.8f).align(if (user) Alignment.CenterEnd else Alignment.CenterStart)
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(text)
                        if (index == bubbles.lastIndex) message.attachmentName?.let { Text("📎 $it", color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp)) }
                    }
                }
            }
        }
        // Cards for a job's files sit under its newest reply only, so a revised CV is never shown twice.
        message.documentGroupId?.takeIf { showsGroupCards }?.let { group ->
            documents.filter { it.groupId == group && it.kind != "job" }.sortedBy { it.createdAt }.forEach { ArtifactCard(it, docActions) }
        }
        message.documentId?.let { id -> documents.firstOrNull { it.id == id }?.let { ArtifactCard(it, docActions) } }
    }
}

@Composable
private fun TaskCard(task: ApplicationTaskEntity, cancel: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(task.title, fontWeight = FontWeight.SemiBold); Text(if (task.status == "QUEUED") "Waiting in queue" else "Working on it", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            TextButton(onClick = cancel, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) { Text("Cancel") }
        }
    }
}

private class ListRow(val title: String, val subtitle: String? = null, val destructive: Boolean = false, val onClick: (() -> Unit)? = null, val trailing: (@Composable () -> Unit)? = null)

@Composable
private fun GroupedList(rows: List<ListRow>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEachIndexed { index, row ->
            val top = if (index == 0) 28.dp else 4.dp
            val bottom = if (index == rows.lastIndex) 28.dp else 4.dp
            val shape = RoundedCornerShape(top, top, bottom, bottom)
            val body: @Composable () -> Unit = {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(row.title, style = MaterialTheme.typography.bodyLarge, color = if (row.destructive) colors.error else colors.onSurface)
                        row.subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
                    }
                    row.trailing?.invoke()
                }
            }
            if (row.onClick != null) Surface(onClick = row.onClick, shape = shape, color = colors.surfaceContainer, content = body)
            else Surface(shape = shape, color = colors.surfaceContainer, content = body)
        }
    }
}

@Composable
private fun SettingsSection(title: String, rows: List<ListRow>) {
    if (rows.isEmpty()) return
    Column {
        Text(title, Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        GroupedList(rows)
    }
}

private const val LICENSES = "AndroidX, Jetpack Compose, Material 3, Room and DataStore: Apache License 2.0\n\nKotlin and kotlinx.coroutines: Apache License 2.0\n\nLucide icons: ISC License, © Lucide Contributors\n\nNoto Sans: SIL Open Font License 1.1"

private fun openUrl(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun sendEmail(context: android.content.Context, to: String, subject: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$to")).putExtra(Intent.EXTRA_SUBJECT, subject)
    runCatching { context.startActivity(intent) }.onFailure { Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show() }
}

private fun openNotificationSettings(context: android.content.Context) {
    val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
    runCatching { context.startActivity(intent) }
}

@Composable
private fun SettingsScreen(
    email: String, notifications: Boolean, toggleNotifications: (Boolean) -> Unit, themeMode: ThemeMode, setThemeMode: (ThemeMode) -> Unit,
    signOut: () -> Unit, deleteAccount: () -> Unit, runTestTask: () -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var confirmDelete by remember { mutableStateOf(false) }
    var pickTheme by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> toggleNotifications(granted) }
    val setNotifications = { enabled: Boolean ->
        if (enabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else toggleNotifications(enabled)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SettingsSection("Account", listOf(
            ListRow("Email", email),
            ListRow("Sign out", onClick = signOut),
            ListRow("Delete account and data", "Removes your chats, files and sign-in details from this phone.", destructive = true, onClick = { confirmDelete = true })
        ))
        SettingsSection("Notifications", listOf(
            ListRow("Replies", "Tell me when Kwela answers while I’m away", onClick = { setNotifications(!notifications) }, trailing = { Switch(notifications, setNotifications) }),
            ListRow("System notification settings", onClick = { openNotificationSettings(context) })
        ))
        SettingsSection("Appearance", listOf(ListRow("Theme", themeMode.label, onClick = { pickTheme = true })))
        SettingsSection("Privacy and legal", listOfNotNull(
            BuildConfig.PRIVACY_URL.takeIf { it.isNotBlank() }?.let { url -> ListRow("Privacy policy", onClick = { openUrl(context, url) }) },
            BuildConfig.TERMS_URL.takeIf { it.isNotBlank() }?.let { url -> ListRow("Terms of service", onClick = { openUrl(context, url) }) },
            ListRow("Your data", "Your chats and files are saved on this phone. Messages and files you send are processed by Kwela’s servers and OpenAI to prepare your documents, and are not kept on our servers. Your saved CV profile is kept on our server until you delete your account.")
        ))
        SettingsSection("Support", buildList {
            BuildConfig.SUPPORT_EMAIL.takeIf { it.isNotBlank() }?.let { to ->
                add(ListRow("Contact support", to, onClick = { sendEmail(context, to, "Kwela support") }))
                add(ListRow("Send feedback", onClick = { sendEmail(context, to, "Kwela feedback (${BuildConfig.VERSION_NAME})") }))
            }
        })
        SettingsSection("About", listOf(
            ListRow("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"),
            ListRow("Open-source licenses", onClick = { showLicenses = true })
        ))
        if (BuildConfig.DEBUG) SettingsSection("Developer", listOf(ListRow("Run test task", "Debug builds only", onClick = runTestTask)))
        Spacer(Modifier.height(16.dp))
    }
    if (pickTheme) AlertDialog(
        onDismissRequest = { pickTheme = false }, title = { Text("Theme") },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(Modifier.fillMaxWidth().clickable { setThemeMode(mode); pickTheme = false }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = mode == themeMode, onClick = null)
                        Spacer(Modifier.width(16.dp)); Text(mode.label)
                    }
                }
            }
        },
        confirmButton = {}, dismissButton = { TextButton(onClick = { pickTheme = false }) { Text("Cancel") } }
    )
    if (showLicenses) AlertDialog(onDismissRequest = { showLicenses = false }, title = { Text("Open-source licenses") }, text = { Text(LICENSES, Modifier.verticalScroll(rememberScrollState())) }, confirmButton = { TextButton(onClick = { showLicenses = false }) { Text("Close") } })
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false }, title = { Text("Delete account and data?") },
        text = { Text("This removes your chats, files and sign-in details from this phone and signs you out. This can’t be undone.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; deleteAccount() }, colors = ButtonDefaults.textButtonColors(contentColor = colors.error)) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } }
    )
}
