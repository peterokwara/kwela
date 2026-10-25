package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FileTabTest {
    @Test
    fun resumesAndLettersAreSeparatedByKind() {
        assertEquals(FileTab.RESUMES, FileTab.of(kind = "resume", title = "CV"))
        assertEquals(FileTab.LETTERS, FileTab.of(kind = "cover_letter", title = "Cover letter"))
        assertEquals(FileTab.LETTERS, FileTab.of(kind = "answers", title = "Answers"))
        assertEquals(FileTab.JOBS, FileTab.of(kind = "job", title = "Job"))
    }

    @Test
    fun olderFilesWithoutAKindAreSortedByTheirTitle() {
        assertEquals(FileTab.RESUMES, FileTab.of(kind = null, title = "CV - Cook"))
        assertEquals(FileTab.RESUMES, FileTab.of(kind = "", title = "My Resume"))
        assertEquals(FileTab.LETTERS, FileTab.of(kind = null, title = "Cover letter - Cook"))
        assertEquals(null, FileTab.of(kind = null, title = "Notes"))
    }
}
