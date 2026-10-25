package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DocumentUpdatesTest {
    private fun doc(id: String, kind: String?, group: String?, title: String?, at: Long = 1) = FileEntry(id, kind ?: "?", group, title, at, kind)

    private val saved = listOf(
        doc("cv1", "resume", "g1", "Data Analyst · Example Bank", 10),
        doc("l1", "cover_letter", "g1", "Data Analyst · Example Bank", 11),
        doc("cv0", "resume", "g0", "Cook · Serena", 5)
    )

    @Test
    fun aRevisionOfTheSameJobUpdatesTheFilesInsteadOfAddingCopies() {
        val plan = DocumentUpdates.plan(saved, "Data Analyst · Example Bank", listOf("resume"))

        assertEquals("g1", plan.groupId)
        assertEquals(mapOf("resume" to "cv1"), plan.replace)
    }

    @Test
    fun aNewKindForAKnownJobJoinsItsGroup() {
        val plan = DocumentUpdates.plan(saved, "Data Analyst · Example Bank", listOf("resume", "answers"))

        assertEquals("g1", plan.groupId)
        assertEquals(mapOf("resume" to "cv1"), plan.replace)
    }

    @Test
    fun aDifferentJobStartsItsOwnGroup() {
        val plan = DocumentUpdates.plan(saved, "Nurse · Aga Khan", listOf("resume", "cover_letter"))

        assertNull(plan.groupId)
        assertEquals(emptyMap<String, String>(), plan.replace)
    }

    @Test
    fun aVagueJobNameIsNeverMergedWithAnotherVagueOne() {
        val vague = listOf(doc("a", "resume", "g9", "Application"))

        assertNull(DocumentUpdates.plan(vague, "Application", listOf("resume")).groupId)
    }

    @Test
    fun jobNamesAreComparedWithoutCaseOrSpacingFuss() {
        assertEquals("g0", DocumentUpdates.plan(saved, "  cook · serena ", listOf("resume")).groupId)
    }
}
