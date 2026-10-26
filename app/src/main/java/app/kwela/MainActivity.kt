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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import app.kwela.domain.ReplyPacing
import app.kwela.domain.MessageDelivery
import app.kwela.domain.Delivery
import app.kwela.domain.FailedReplies
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Delete
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
import app.kwela.domain.DisplayName
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
import app.kwela.ui.ProfileSheet
import app.kwela.ui.SettingsScreen
import app.kwela.ui.ProfileAvatar
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
                    var showProfile by rememberSaveable { mutableStateOf(false) }
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
                        SettingsScreen(state.notificationsEnabled, vm::setNotificationsEnabled, state.themeMode, vm::setThemeMode) { vm.setScreen(AppScreen.CHAT) }
                    } else MainShell(state.screen, vm::setScreen, state.email.orEmpty(), null, { showProfile = true }) {
                        when (state.screen) {
                            AppScreen.CHAT, AppScreen.PROFILE -> ChatScreen(state.messages, state.tasks, state.draftText, vm::setDraft, vm::sendDraft, vm::sendSuggestion, vm::cancelTask, vm::attachFile, state.pendingAttachment, state.isReplying, state.replyIsSlow, vm::removeAttachment, vm::retryReply, state.failedReplies, state.pendingMessages, vm::resendMessage, vm::deleteMessage, state.documents, docActions, state.pendingReview, state.downloadGate, vm::answerReview, vm::addMissingDetails, vm::downloadAnyway)
                            AppScreen.FILES -> FilesScreen(state.documents, docActions)
                        }
                    }
                    if (showProfile && previewDocument == null) ProfileSheet(state.email.orEmpty(), null, openSettings = { vm.setScreen(AppScreen.PROFILE) }, signOut = vm::signOut, deleteAccount = vm::deleteAccount) { showProfile = false }
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(email: String, otp: Boolean, testCode: String?, working: Boolean, error: String?, onEmail: (String) -> Unit, request: () -> Unit, verify: (String) -> Unit, back: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var code by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val requestCode = { focus.clearFocus(); request() }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 24.dp), verticalArrangement = Arrangement.Center) {
        BrandMark()
        Spacer(Modifier.height(24.dp))
        Text(if (otp && testCode != null) "Your sign-in code" else if (otp) "Check your email" else "Welcome to Kwela", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(
            if (otp && testCode != null) "Private test code: $testCode. Enter it to continue."
            else if (otp) "We sent a 6-digit code to $email. Enter it to continue."
            else "Saw a job tonight? Be ready to apply in minutes. No blank page, no cyber café queue.",
            color = colors.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
        if (!otp) {
            OutlinedTextField(email, onEmail, Modifier.fillMaxWidth(), label = { Text("Email address") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { requestCode() }))
            Spacer(Modifier.height(16.dp))
            Button(onClick = requestCode, Modifier.fillMaxWidth().height(52.dp), enabled = !working, shape = CircleShape) { Text(if (working) "Sending code…" else "Continue") }
        } else {
            OutlinedTextField(code, { if (it.length <= 6 && it.all(Char::isDigit)) code = it }, Modifier.fillMaxWidth(), label = { Text("6-digit code") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { if (code.length == 6) { focus.clearFocus(); verify(code) } }))
            Spacer(Modifier.height(16.dp))
            Button(onClick = { focus.clearFocus(); verify(code) }, Modifier.fillMaxWidth().height(52.dp), enabled = !working && code.length == 6, shape = CircleShape) { Text(if (working) "Checking…" else "Sign in") }
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

private fun ApplicationTaskEntity.taskStatus() = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.FAILED)

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MainShell(screen: AppScreen, select: (AppScreen) -> Unit, email: String, knownName: String?, openProfile: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(enabled = screen != AppScreen.CHAT) { select(AppScreen.CHAT) }
    val keyboardOpen = WindowInsets.isImeVisible
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { KwelaTopBar(email, knownName, openProfile) },
        bottomBar = { if (!keyboardOpen) KwelaTabBar(screen, select) }
    ) { padding -> Box(Modifier.padding(padding)) { content() } }
}

/** The top of every main screen: the Kwela name in the middle and a small profile chip at the right. */
@Composable
private fun KwelaTopBar(email: String, knownName: String?, onProfile: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val name = DisplayName.of(knownName, email)
    Box(Modifier.fillMaxWidth().background(colors.surface).windowInsetsPadding(WindowInsets.statusBars).height(72.dp)) {
        Text("kwela", Modifier.align(Alignment.Center), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
        Surface(
            onClick = onProfile, shape = CircleShape, color = colors.surfaceContainer,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).semantics { contentDescription = "Open profile" }
        ) { Box(Modifier.padding(4.dp)) { ProfileAvatar(name, 40.dp) } }
    }
}

private class Tab(val screen: AppScreen, val label: String, @DrawableRes val icon: Int)

private val tabs = listOf(
    Tab(AppScreen.CHAT, "Chat", LucideIcons.MessageCircle),
    Tab(AppScreen.FILES, "Files", LucideIcons.Folder)
)

/** A small floating pill with just the icons, the chosen one in a rounded highlight, like Material 3's compact navigation. */
@Composable
private fun KwelaTabBar(screen: AppScreen, select: (AppScreen) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().background(colors.surface).navigationBarsPadding().padding(vertical = 10.dp), Alignment.Center) {
        Surface(shape = CircleShape, color = colors.surfaceContainer) {
            Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEach { tab ->
                    val isSelected = tab.screen == screen
                    Box(
                        Modifier.size(width = if (isSelected) 88.dp else 64.dp, height = 48.dp).clip(CircleShape)
                            .background(if (isSelected) colors.surfaceContainerHighest else Color.Transparent)
                            .clickable(remember { MutableInteractionSource() }, null, role = Role.Tab) { select(tab.screen) }
                            .semantics { contentDescription = tab.label; selected = isSelected },
                        Alignment.Center
                    ) { Icon(painterResource(tab.icon), null, Modifier.size(24.dp), tint = if (isSelected) colors.onSurface else colors.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun ChatScreen(messages: List<ChatMessageEntity>, tasks: List<ApplicationTaskEntity>, draft: String, setDraft: (String) -> Unit, send: () -> Unit, suggestion: (String) -> Unit, cancelTask: (ApplicationTaskEntity) -> Unit, attach: (String) -> Unit, pendingAttachment: PendingAttachment?, isReplying: Boolean, slowWait: Boolean, removeAttachment: () -> Unit, retry: () -> Unit, failed: Map<String, String>, pendingMessages: Map<String, Boolean>, resend: (String) -> Unit, deleteMessage: (String) -> Unit, documents: List<SavedDocumentEntity>, docActions: DocumentActions, pendingReview: ReviewFinding?, downloadGate: DownloadGate?, answerReview: (String) -> Unit, addMissing: () -> Unit, downloadAnyway: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val now = remember { System.currentTimeMillis() }
    val listState = rememberLazyListState()
    val composerFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // The message whose "Check options" sheet is open.
    var optionsFor by rememberSaveable { mutableStateOf<String?>(null) }
    fun deliveryOf(message: ChatMessageEntity): Delivery =
        if (message.role != "USER" || LocalMessages.isLocal(message.id)) Delivery.SENT
        else MessageDelivery.of(message.id, failed.keys, pendingMessages, ReplyPacing.isSlow(message.content, message.attachmentUri != null))
    LaunchedEffect(messages.size, isReplying, failed.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1) }
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
                        MessageBubble(message, documents, docActions, showsGroupCards = message.documentGroupId == null || newestWithGroup[message.documentGroupId] == message.id, delivery = deliveryOf(message), onOptions = { optionsFor = message.id })
                    }
                }
                items(tasks.filter { it.status == "RUNNING" || it.status == "QUEUED" }) { task -> TaskCard(task, { cancelTask(task) }) }
                // "Typing" would be a lie while the phone is offline and the message is just waiting to go.
                if (isReplying && !slowWait && pendingMessages.values.none { waiting -> waiting }) item { TypingDots() }
                item { ChatChips(messages, isReplying, pendingReview, downloadGate, answerReview, addMissing, downloadAnyway, onRevision = { chip -> if (chip == "Something else") { composerFocus.requestFocus(); keyboard?.show() } else suggestion(chip) }) }
                if (ConversationResume.needsPickUp(LocalMessages.lastRealRole(messages.map { it.id to it.role }), isReplying, failed.isNotEmpty())) item { PickUpCard(retry) }
            }
        }
        val optionsMessage = messages.firstOrNull { it.id == optionsFor }
        val optionsDelivery = optionsMessage?.let(::deliveryOf) ?: Delivery.SENT
        // Close the sheet once the message has nothing left to report.
        LaunchedEffect(optionsDelivery, optionsMessage) { if (optionsFor != null && !MessageDelivery.hasOptions(optionsDelivery)) optionsFor = null }
        if (optionsMessage != null && MessageDelivery.hasOptions(optionsDelivery)) {
            DeliveryOptionsSheet(
                optionsDelivery, FailedReplies.reason(failed[optionsMessage.id]),
                resend = { optionsFor = null; resend(optionsMessage.id) },
                delete = { optionsFor = null; deleteMessage(optionsMessage.id) },
                dismiss = { optionsFor = null }
            )
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
            if (draft.isNotBlank() || pendingAttachment != null) {
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

/** The small line under a message that has not gone through, as in Google Messages: what happened, and where to find the choices. */
@Composable
private fun DeliveryLine(delivery: Delivery, onOptions: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val failed = delivery == Delivery.FAILED
    val tint = if (failed) colors.error else colors.onSurfaceVariant
    Row(Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        if (failed) Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(16.dp), tint = tint)
        Text(" ${MessageDelivery.line(delivery)} ", style = MaterialTheme.typography.labelMedium, color = tint)
        Text(
            "Check options", style = MaterialTheme.typography.labelMedium, textDecoration = TextDecoration.Underline,
            color = if (failed) colors.error else colors.primary,
            // A comfortable thumb target, not just the width of the words.
            modifier = Modifier.defaultMinSize(minHeight = 40.dp).clickable(onClick = onOptions).padding(horizontal = 4.dp).wrapContentHeight(Alignment.CenterVertically)
        )
        if (delivery == Delivery.WAITING) Icon(Icons.Outlined.Schedule, "Waiting", Modifier.size(16.dp), tint = tint)
    }
}

/** The small drawer from Google Messages. Not sent: Resend or Delete. Still sending: Sending..., or Stop sending and delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeliveryOptionsSheet(delivery: Delivery, error: String?, resend: () -> Unit, delete: () -> Unit, dismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = dismiss, containerColor = colors.surfaceContainer) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp)) {
            if (delivery == Delivery.FAILED) {
                error?.let { Text(it, Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
                SheetRow(Icons.AutoMirrored.Outlined.Send, "Resend", resend)
                SheetRow(Icons.Outlined.Delete, "Delete", delete)
            } else {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = colors.onSurfaceVariant)
                    Spacer(Modifier.width(24.dp))
                    Text("Sending…", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                }
                SheetRow(Icons.Outlined.Delete, "Stop sending and delete", delete)
            }
        }
    }
}

@Composable
private fun SheetRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(24.dp), tint = colors.onSurface)
        Spacer(Modifier.width(24.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
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
private fun MessageBubble(message: ChatMessageEntity, documents: List<SavedDocumentEntity>, docActions: DocumentActions, showsGroupCards: Boolean, delivery: Delivery, onOptions: () -> Unit) {
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
        if (user && MessageDelivery.hasOptions(delivery)) DeliveryLine(delivery, onOptions)
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
