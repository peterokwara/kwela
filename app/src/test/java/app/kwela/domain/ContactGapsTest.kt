package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactGapsTest {
    @Test
    fun aCompleteCvHasNoGaps() {
        val cv = """{"personalDetails":{"firstName":"Amina","lastName":"Wanjiru","email":"a@example.test","phoneNumber":"0700 000 000"}}"""

        assertEquals(emptyList<String>(), ContactGaps.of(cv))
    }

    @Test
    fun missingPhoneAndEmailAreNamedInPlainWords() {
        val cv = """{"personalDetails":{"firstName":"Amina","lastName":"Wanjiru","email":" ","phoneNumber":""}}"""

        assertEquals(listOf("phone number", "email address"), ContactGaps.of(cv))
    }

    @Test
    fun aMissingNameIsAGapToo() {
        assertEquals(listOf("name", "phone number", "email address"), ContactGaps.of("""{"personalDetails":{}}"""))
    }

    @Test
    fun documentsWithoutAStructuredSourceAreNotChecked() {
        assertEquals(emptyList<String>(), ContactGaps.of(null))
        assertEquals(emptyList<String>(), ContactGaps.of("not json"))
        assertEquals(emptyList<String>(), ContactGaps.of("""{"letterContent":"Dear team"}"""))
    }

    @Test
    fun theGapsReadAsOneFriendlyLine() {
        assertEquals("your phone number", ContactGaps.sentence(listOf("phone number")))
        assertEquals("your phone number and email address", ContactGaps.sentence(listOf("phone number", "email address")))
        assertEquals("your name, phone number and email address", ContactGaps.sentence(listOf("name", "phone number", "email address")))
    }
}
