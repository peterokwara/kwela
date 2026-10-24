package app.kwela.domain

data class TaskSummary(val id: String, val title: String, val status: TaskStatus, val updatedAt: Long)

data class DocumentSummary(val id: String, val title: String, val createdAt: Long)

data class ActivityEntry(val id: String, val title: String, val detail: String, val at: Long)

object ActivityTimeline {
    fun build(tasks: List<TaskSummary>, documents: List<DocumentSummary>): List<ActivityEntry> =
        (tasks.map { ActivityEntry(it.id, it.title, describe(it.status), it.updatedAt) } +
            documents.map { ActivityEntry(it.id, it.title, "Saved to Files", it.createdAt) })
            .sortedByDescending { it.at }

    fun describe(status: TaskStatus) = when (status) {
        TaskStatus.QUEUED -> "Queued"
        TaskStatus.RUNNING -> "Working on it"
        TaskStatus.WAITING_FOR_USER -> "Waiting for your review"
        TaskStatus.COMPLETED -> "Completed"
        TaskStatus.CANCELLED -> "Cancelled"
        TaskStatus.FAILED -> "Something went wrong"
    }
}
