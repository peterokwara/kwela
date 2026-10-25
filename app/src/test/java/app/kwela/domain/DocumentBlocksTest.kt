package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentBlocksTest {
    private val cv = "Amina Wanjiru\namina@example.test · 0700 000 000\n\n" +
        "PROFESSIONAL SUMMARY\nCareful analyst.\n\n" +
        "EXPERIENCE\nData Clerk, Example Co (Jan 2022 - Present)\n- Cleaned reports\n- Cut errors\n\n" +
        "SKILLS\nExcel, SQL"

    @Test
    fun theNameAndContactLineOpenTheDocument() {
        val blocks = DocumentBlocks.parse(cv)

        assertEquals(DocBlock.Title("Amina Wanjiru"), blocks[0])
        assertEquals(DocBlock.Subtitle("amina@example.test · 0700 000 000"), blocks[1])
    }

    @Test
    fun capitalLettersLinesAreSectionHeadings() {
        val headings = DocumentBlocks.parse(cv).filterIsInstance<DocBlock.Heading>().map { it.text }

        assertEquals(listOf("PROFESSIONAL SUMMARY", "EXPERIENCE", "SKILLS"), headings)
    }

    @Test
    fun dashLinesAreBulletsAndOtherLinesAreParagraphs() {
        val blocks = DocumentBlocks.parse(cv)

        assertEquals(
            listOf(
                DocBlock.Paragraph("Data Clerk, Example Co (Jan 2022 - Present)"),
                DocBlock.Bullet("Cleaned reports"),
                DocBlock.Bullet("Cut errors")
            ),
            blocks.subList(blocks.indexOfFirst { it == DocBlock.Heading("EXPERIENCE") } + 1, blocks.indexOfFirst { it == DocBlock.Heading("SKILLS") })
        )
    }

    @Test
    fun aCoverLetterReadsAsParagraphs() {
        val blocks = DocumentBlocks.parse("Amina Wanjiru\namina@example.test\n\nExample Bank\n\nDear Hiring Team,\n\nI would like to apply.")

        assertEquals(
            listOf(DocBlock.Title("Amina Wanjiru"), DocBlock.Subtitle("amina@example.test"), DocBlock.Paragraph("Example Bank"), DocBlock.Paragraph("Dear Hiring Team,"), DocBlock.Paragraph("I would like to apply.")),
            blocks
        )
    }

    @Test
    fun onlyThePlainLinesCanBeTappedToChange() {
        val tappable = DocumentBlocks.parse(cv).filter { it.canChange }.map { it.text }

        assertEquals(listOf("Careful analyst.", "Data Clerk, Example Co (Jan 2022 - Present)", "Cleaned reports", "Cut errors", "Excel, SQL"), tappable)
    }

    @Test
    fun anEmptyDocumentHasNoBlocks() {
        assertEquals(emptyList<DocBlock>(), DocumentBlocks.parse("  \n\n "))
    }
}
