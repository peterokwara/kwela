package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNamesTest {
    @Test
    fun aTitleBecomesAPdfName() {
        assertEquals("CV - Senior Engineer, Techno Brain.pdf", FileNames.pdfName("CV - Senior Engineer, Techno Brain"))
    }

    @Test
    fun charactersAFileSystemDislikesAreRemoved() {
        assertEquals("CV Data Analyst Bank.pdf", FileNames.pdfName("CV: Data/Analyst? <Bank>"))
    }

    @Test
    fun anExistingPdfEndingIsNotDoubled() {
        assertEquals("Letter.pdf", FileNames.pdfName("Letter.pdf"))
    }

    @Test
    fun longTitlesAreShortenedAndEmptyOnesGetADefault() {
        assertTrue(FileNames.pdfName("x".repeat(200)).length <= 84)
        assertEquals("Kwela document.pdf", FileNames.pdfName("  ///  "))
    }
}
