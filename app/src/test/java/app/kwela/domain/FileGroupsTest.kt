package app.kwela.domain

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class FileGroupsTest {
    private val nairobi = ZoneId.of("Africa/Nairobi")
    private val now = at(2026, 10, 21, 15)

    private fun at(year: Int, month: Int, day: Int, hour: Int = 9) =
        ZonedDateTime.of(year, month, day, hour, 0, 0, 0, nairobi).toInstant().toEpochMilli()

    private fun file(id: String, title: String, groupId: String?, groupTitle: String?, createdAt: Long) =
        FileEntry(id, title, groupId, groupTitle, createdAt)

    @Test
    fun filesFromOneReplyBecomeOneApplicationCardInSavedOrder() {
        val files = listOf(
            file("cv", "CV", "g1", "Senior Engineer · Techno Brain", at(2026, 10, 21, 10)),
            file("letter", "Cover letter", "g1", "Senior Engineer · Techno Brain", at(2026, 10, 21, 10) + 1),
            file("answers", "Answers", "g1", "Senior Engineer · Techno Brain", at(2026, 10, 21, 10) + 2)
        )

        val sections = FileGroups.group(files.shuffled(), now, nairobi)

        assertEquals(1, sections.size)
        assertEquals("Today", sections[0].label)
        assertEquals("Senior Engineer · Techno Brain", sections[0].applications.single().title)
        assertEquals(listOf("cv", "letter", "answers"), sections[0].applications.single().files.map { it.id })
    }

    @Test
    fun sectionsAndCardsAreNewestFirst() {
        val files = listOf(
            file("old", "CV", "g-old", "Barista · Cafe", at(2026, 9, 12)),
            file("a", "CV", "g-a", "Cook · Serena", at(2026, 10, 21, 8)),
            file("b", "CV", "g-b", "Chef · Java", at(2026, 10, 21, 12))
        )

        val sections = FileGroups.group(files, now, nairobi)

        assertEquals(listOf("Today", "Last month"), sections.map { it.label })
        assertEquals(listOf("Chef · Java", "Cook · Serena"), sections[0].applications.map { it.title })
    }

    @Test
    fun olderFilesWithoutAJobEachGetTheirOwnCard() {
        val files = listOf(
            file("x", "Test CV", null, null, at(2026, 10, 20)),
            file("y", "Test cover letter", null, null, at(2026, 10, 20) + 1)
        )

        val applications = FileGroups.group(files, now, nairobi).single().applications

        assertEquals(listOf("Test cover letter", "Test CV"), applications.map { it.title })
        assertEquals("Yesterday", FileGroups.group(files, now, nairobi).single().label)
    }

    @Test
    fun noFilesMeansNoSections() {
        assertEquals(emptyList<DatedFiles>(), FileGroups.group(emptyList(), now, nairobi))
    }
}
