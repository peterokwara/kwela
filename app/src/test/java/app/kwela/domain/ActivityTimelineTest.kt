package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityTimelineTest {
    @Test
    fun listsTasksAndSavedFilesNewestFirst() {
        val entries = ActivityTimeline.build(
            tasks = listOf(TaskSummary("t1", "Sample application pack", TaskStatus.COMPLETED, updatedAt = 3_000L)),
            documents = listOf(
                DocumentSummary("d1", "Sample CV.pdf", createdAt = 1_000L),
                DocumentSummary("d2", "Sample cover letter.pdf", createdAt = 2_000L)
            )
        )

        assertEquals(
            listOf(
                ActivityEntry("t1", "Sample application pack", "Completed", 3_000L),
                ActivityEntry("d2", "Sample cover letter.pdf", "Saved to Files", 2_000L),
                ActivityEntry("d1", "Sample CV.pdf", "Saved to Files", 1_000L)
            ),
            entries
        )
    }

    @Test
    fun describesEachTaskStatusInPlainWords() {
        fun detail(status: TaskStatus) =
            ActivityTimeline.build(listOf(TaskSummary("t", "Task", status, 1L)), emptyList()).single().detail

        assertEquals("Queued", detail(TaskStatus.QUEUED))
        assertEquals("Working on it", detail(TaskStatus.RUNNING))
        assertEquals("Waiting for your review", detail(TaskStatus.WAITING_FOR_USER))
        assertEquals("Completed", detail(TaskStatus.COMPLETED))
        assertEquals("Cancelled", detail(TaskStatus.CANCELLED))
        assertEquals("Something went wrong", detail(TaskStatus.FAILED))
    }
}
