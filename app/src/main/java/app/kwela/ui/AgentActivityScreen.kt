package app.kwela.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kwela.R
import app.kwela.domain.ActivityTimeline
import app.kwela.domain.ChatTimeHeader
import app.kwela.domain.DocumentSummary
import app.kwela.domain.TaskStatus
import app.kwela.domain.TaskSummary

/** Shows the activity available on this device, with a way back to the relevant screen. */
@Composable
fun AgentActivityScreen(state: KwelaUiState, close: () -> Unit, openChat: () -> Unit, openFiles: () -> Unit) {
    BackHandler(onBack = close)
    val colors = MaterialTheme.colorScheme
    val tasks = state.tasks.map {
        TaskSummary(it.id, it.title, runCatching { TaskStatus.valueOf(it.status) }.getOrDefault(TaskStatus.FAILED), it.updatedAt)
    }
    val entries = ActivityTimeline.build(tasks, state.documents.map { DocumentSummary(it.id, it.title, it.createdAt) })
    val status = when {
        state.pendingMessages.values.any { it } -> "Waiting for a connection"
        state.isReplying || state.pendingMessages.isNotEmpty() || tasks.any { it.status == TaskStatus.RUNNING || it.status == TaskStatus.QUEUED } -> "Working on your request"
        state.pendingReview != null || tasks.any { it.status == TaskStatus.WAITING_FOR_USER } -> "A question for you"
        state.failedReplies.isNotEmpty() || tasks.any { it.status == TaskStatus.FAILED } -> "Something needs your attention"
        else -> "Ready to help"
    }
    val now = System.currentTimeMillis()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        IconButton(onClick = close) { Icon(painterResource(R.drawable.ms_arrow_back), contentDescription = "Back to Kwela") }
        Column(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.kwela_avatar), contentDescription = null, modifier = Modifier.size(72.dp))
            Text("Kwela", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))
            Text(status, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.pendingReview != null || state.failedReplies.isNotEmpty()) {
                item {
                    Button(onClick = openChat) { Text("Go to chat") }
                }
            }
            item { Text("Recent activity", style = MaterialTheme.typography.titleMedium) }
            if (state.pendingMessages.isNotEmpty()) {
                items(state.pendingMessages.toList(), key = { "pending-${it.first}" }) { (_, waiting) ->
                    ActivityRow("Your request", if (waiting) "Waiting for a connection" else "In progress", null)
                }
            }
            if (state.failedReplies.isNotEmpty()) {
                items(state.failedReplies.toList(), key = { "failed-${it.first}" }) { (_, message) ->
                    ActivityRow("Your request", message, null)
                }
            }
            if (entries.isEmpty() && state.pendingMessages.isEmpty() && state.failedReplies.isEmpty()) {
                item { Text("Your tasks and saved files will appear here.", color = colors.onSurfaceVariant) }
            }
            items(entries, key = { "activity-${it.id}" }) { entry ->
                ActivityRow(entry.title, entry.detail, ChatTimeHeader.label(entry.at, now))
            }
            if (state.documents.isNotEmpty()) {
                item { TextButton(onClick = openFiles) { Text("Open Files") } }
            }
        }
    }
}

@Composable
private fun ActivityRow(title: String, detail: String, time: String?) {
    Column {
        Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (time != null) Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
