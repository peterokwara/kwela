package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayNameTest {
    @Test
    fun makesAFriendlyNameFromTheStartOfAnEmail() {
        assertEquals("Peter", DisplayName.fromEmail("peter@example.com"))
        assertEquals("Peter Okwara", DisplayName.fromEmail("peter.okwara@example.com"))
        assertEquals("Wanjiku Mwangi", DisplayName.fromEmail("wanjiku_mwangi@example.com"))
    }

    @Test
    fun ignoresPlusTagsAndDigits() {
        assertEquals("Peter", DisplayName.fromEmail("peter+kwela@example.com"))
        assertEquals("Amina", DisplayName.fromEmail("amina1987@example.com"))
    }

    @Test
    fun fallsBackToTheEmailWhenThereIsNothingNameLike() {
        assertEquals("12345@example.com", DisplayName.fromEmail("12345@example.com"))
        assertEquals("Kwela user", DisplayName.fromEmail(null))
    }

    @Test
    fun aKnownNameWinsOverTheEmail() {
        assertEquals("Peter Okwara", DisplayName.of("Peter Okwara", "pexe@example.com"))
        assertEquals("Peter", DisplayName.of("  ", "peter@example.com"))
    }

    @Test
    fun initialIsTheFirstLetterInCapitals() {
        assertEquals("P", DisplayName.initial("Peter Okwara"))
        assertEquals("K", DisplayName.initial(""))
    }
}
