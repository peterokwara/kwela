package app.kwela.ui

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.kwela.KwelaApplication
import app.kwela.data.DocumentPdf
import app.kwela.data.local.SavedDocumentEntity
import app.kwela.domain.DocBlock
import app.kwela.domain.DocumentBlocks
import app.kwela.domain.FileEntry
import app.kwela.domain.FileGroups
import app.kwela.domain.FileNames
import app.kwela.domain.FileTab
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private object DocIcons {
    val Back = com.composables.icons.lucide.R.drawable.lucide_ic_arrow_left
    val Download = com.composables.icons.lucide.R.drawable.lucide_ic_download
    val Share = com.composables.icons.lucide.R.drawable.lucide_ic_share_2
    val File = com.composables.icons.lucide.R.drawable.lucide_ic_file_text
}

/** What a person can do with a document. The same three actions are used in the chat cards, the Files list and the preview page. */
class DocumentActions(
    val open: (SavedDocumentEntity) -> Unit,
    val download: (SavedDocumentEntity) -> Unit,
    val share: (SavedDocumentEntity) -> Unit,
    /** Id of the document whose PDF is being prepared, or null. */
    val busyDocumentId: String?
)

private fun documentFileTitle(document: SavedDocumentEntity) = listOfNotNull(document.title, document.groupTitle).joinToString(" - ")

@Composable
fun rememberDocumentActions(open: (SavedDocumentEntity) -> Unit): DocumentActions {
    val context = LocalContext.current
    val app = context.applicationContext as KwelaApplication
    val scope = rememberCoroutineScope()
    val pdf = remember { DocumentPdf(context, app.api, app.repository) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var pendingSave by remember { mutableStateOf<ByteArray?>(null) }
    fun say(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val bytes = pendingSave
        pendingSave = null
        if (uri != null && bytes != null) scope.launch {
            val saved = runCatching { withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } } }.isSuccess
            say(if (saved) "Saved." else "Couldn’t save the PDF. Try Share instead.")
        }
    }

    val download: (SavedDocumentEntity) -> Unit = { document ->
        if (busyId == null) scope.launch {
            busyId = document.id
            try {
                val file = pdf.make(document)
                val name = FileNames.pdfName(documentFileTitle(document))
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    pendingSave = file.bytes
                    picker.launch(name)
                } else {
                    val saved = pdf.saveToDownloads(name, file.bytes)
                    val designed = document.kind == "resume" || document.kind == "cover_letter"
                    say(
                        when {
                            saved == null -> "Couldn’t save the PDF. Try Share instead."
                            designed && !file.fullDesign -> "Saved a simple version to Downloads. The full design needs internet."
                            else -> "Saved to your Downloads."
                        }
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                say("Couldn’t make the PDF. Please try again.")
            } finally {
                busyId = null
            }
        }
    }

    val share: (SavedDocumentEntity) -> Unit = { document ->
        if (busyId == null) scope.launch {
            busyId = document.id
            try {
                val file = pdf.make(document)
                val uri = pdf.forSharing(document, file.bytes)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_SUBJECT, documentFileTitle(document))
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = android.content.ClipData.newUri(context.contentResolver, document.title, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share ${document.title}"))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                say("Couldn’t make the PDF. Please try again.")
            } finally {
                busyId = null
            }
        }
    }
    return remember(busyId, open) { DocumentActions(open, download, share, busyId) }
}

/** A finished document shown as a card in the chat, like an attachment in a messaging app. */
@Composable
fun ArtifactCard(document: SavedDocumentEntity, actions: DocumentActions) {
    val colors = MaterialTheme.colorScheme
    val busy = actions.busyDocumentId == document.id
    val preview = remember(document.body) {
        DocumentBlocks.parse(document.body).filter { it.canChange }.take(2).joinToString(" ") { it.text }.ifBlank { document.body.take(140) }
    }
    Surface(shape = RoundedCornerShape(20.dp), color = colors.surfaceContainerHigh, modifier = Modifier.fillMaxWidth(0.92f).clickable { actions.open(document) }) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(DocIcons.File), null, Modifier.size(22.dp), tint = colors.primary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(document.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.onSurface)
                    document.groupTitle?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant) }
                }
            }
            Text(preview, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = { actions.open(document) }) { Text("Open") }
                TextButton(onClick = { actions.download(document) }, enabled = actions.busyDocumentId == null) { Text(if (busy) "Preparing…" else "Download") }
                TextButton(onClick = { actions.share(document) }, enabled = actions.busyDocumentId == null) { Text("Share") }
            }
        }
    }
}

/** Files, laid out like WhatsApp's media screen: tabs along the top, date headings, newest first. */
@Composable
fun FilesScreen(documents: List<SavedDocumentEntity>, actions: DocumentActions) {
    val colors = MaterialTheme.colorScheme
    if (documents.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(42.dp))
            Text("Your files will appear here", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Spacer(Modifier.height(8.dp))
            Text("Your CV and cover letters show up here, newest first.", color = colors.onSurfaceVariant)
        }
        return
    }
    var tab by rememberSaveable { mutableStateOf(FileTab.JOBS) }
    val now = remember { System.currentTimeMillis() }
    val byId = remember(documents) { documents.associateBy { it.id } }
    val time = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab.ordinal, containerColor = androidx.compose.ui.graphics.Color.Transparent) {
            FileTab.entries.forEach { entry ->
                Tab(selected = tab == entry, onClick = { tab = entry }, text = { Text(entry.label) })
            }
        }
        val shown = if (tab == FileTab.JOBS) documents else documents.filter { FileTab.of(it.kind, it.title) == tab }
        if (shown.isEmpty()) {
            Text("Nothing here yet.", Modifier.padding(20.dp), color = colors.onSurfaceVariant)
            return@Column
        }
        // Resumes and Letters list each file on its own; Jobs keeps a document's job together.
        val sections = remember(shown, tab) {
            FileGroups.group(shown.map { FileEntry(it.id, it.title, if (tab == FileTab.JOBS) it.groupId else null, it.groupTitle, it.createdAt) }, now)
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sections.forEach { section ->
                item(key = "$tab-section-${section.label}") {
                    Text(section.label, Modifier.padding(start = 8.dp, top = 12.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                }
                items(section.applications, key = { "$tab-${it.id}" }) { application ->
                    Surface(shape = RoundedCornerShape(24.dp), color = colors.surfaceContainer) {
                        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 6.dp)) {
                            val single = application.files.singleOrNull()?.let { byId[it.id] }
                            Text(
                                if (tab != FileTab.JOBS && single?.groupTitle != null) single.groupTitle else application.title,
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onSurface
                            )
                            Text(time.format(Instant.ofEpochMilli(application.at).atZone(ZoneId.systemDefault())), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            application.files.forEach { file ->
                                val document = byId[file.id] ?: return@forEach
                                val busy = actions.busyDocumentId == document.id
                                Row(Modifier.fillMaxWidth().clickable { actions.open(document) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(painterResource(DocIcons.File), null, Modifier.size(20.dp), tint = colors.onSurfaceVariant)
                                    Spacer(Modifier.width(10.dp))
                                    Text(document.title.substringBefore(" - "), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                                    TextButton(onClick = { actions.download(document) }, enabled = actions.busyDocumentId == null) { Text(if (busy) "Preparing…" else "Download") }
                                    TextButton(onClick = { actions.share(document) }, enabled = actions.busyDocumentId == null) { Text("Share") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Full-page review of a document. Built for reading the content quickly, not for matching the final PDF pixel for pixel. */
@Composable
fun DocumentPreviewScreen(document: SavedDocumentEntity, actions: DocumentActions, changeRequest: (line: String, note: String) -> Unit, close: () -> Unit) {
    BackHandler(onBack = close)
    val colors = MaterialTheme.colorScheme
    val blocks = remember(document.body) { DocumentBlocks.parse(document.body) }
    val busy = actions.busyDocumentId != null
    var selected by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(colors.surface).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(close, Modifier.size(56.dp)) { Icon(painterResource(DocIcons.Back), "Back", Modifier.size(24.dp), tint = colors.onSurface) }
            Column {
                Text(document.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
                Text(document.groupTitle?.let { "$it · Tap a line to change it" } ?: "Tap a line to change it", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
            items(blocks) { block -> BlockView(block, selected = block.canChange && block.text == selected, onTap = if (block.canChange) ({ selected = if (selected == block.text) null else block.text }) else null) }
            item { Spacer(Modifier.height(24.dp)) }
        }
        val line = selected
        if (line != null) {
            ChangeBar(line, cancel = { selected = null }) { note -> changeRequest(line, note) }
        } else Surface(color = colors.surfaceContainer) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { actions.download(document) }, Modifier.weight(1f).height(52.dp), enabled = !busy) {
                    Icon(painterResource(DocIcons.Download), null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (actions.busyDocumentId == document.id) "Preparing…" else "Download PDF")
                }
                FilledTonalButton(onClick = { actions.share(document) }, Modifier.height(52.dp), enabled = !busy) {
                    Icon(painterResource(DocIcons.Share), null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
            }
        }
    }
}

/** The WhatsApp-style reply bar: the line you tapped is quoted, and you say what should change. */
@Composable
private fun ChangeBar(line: String, cancel: () -> Unit, send: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var note by remember { mutableStateOf("") }
    Surface(color = colors.surfaceContainer) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().background(colors.surfaceContainerHigh, RoundedCornerShape(14.dp)).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).height(36.dp).background(colors.primary, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(10.dp))
                Text(line, Modifier.weight(1f), maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                IconButton(cancel, Modifier.size(44.dp)) { Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_x), "Cancel", Modifier.size(18.dp), tint = colors.onSurfaceVariant) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.TextField(
                    note, { note = it }, Modifier.weight(1f), placeholder = { Text("What should change?") }, shape = RoundedCornerShape(28.dp), maxLines = 3,
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent, unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedContainerColor = colors.surfaceContainerHigh, unfocusedContainerColor = colors.surfaceContainerHigh
                    )
                )
                androidx.compose.material3.FilledIconButton(onClick = { send(note) }, Modifier.size(52.dp), enabled = note.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Outlined.Send, "Send")
                }
            }
        }
    }
}

@Composable
private fun BlockView(block: DocBlock, selected: Boolean = false, onTap: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val tap = if (onTap != null) Modifier.clip(RoundedCornerShape(10.dp)).background(if (selected) colors.primaryContainer else androidx.compose.ui.graphics.Color.Transparent).clickable(onClick = onTap) else Modifier
    when (block) {
        is DocBlock.Title -> Text(block.text, Modifier.padding(start = 8.dp, top = 8.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onSurface)
        is DocBlock.Subtitle -> Text(block.text, Modifier.padding(start = 8.dp, bottom = 4.dp), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        is DocBlock.Heading -> Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 20.dp, bottom = 6.dp)) {
            Text(block.text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.primary)
            HorizontalDivider(Modifier.padding(top = 4.dp), color = colors.outlineVariant)
        }
        is DocBlock.Paragraph -> Text(block.text, Modifier.fillMaxWidth().then(tap).padding(horizontal = 8.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyLarge, color = if (selected) colors.onPrimaryContainer else colors.onSurface)
        is DocBlock.Bullet -> Row(Modifier.fillMaxWidth().then(tap).padding(horizontal = 8.dp, vertical = 8.dp)) {
            Text("•", Modifier.width(18.dp), style = MaterialTheme.typography.bodyLarge, color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant)
            Text(block.text, style = MaterialTheme.typography.bodyLarge, color = if (selected) colors.onPrimaryContainer else colors.onSurface)
        }
    }
}
