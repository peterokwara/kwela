package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ChangeRequestTest {
    @Test
    fun aTappedLineAndTheNoteBecomeOneClearMessage() {
        assertEquals(
            "Change this line in my CV: “Cut errors by 20%” — make it sound less boastful",
            ChangeRequest.message("CV", "Cut errors by 20%", "make it sound less boastful")
        )
    }

    @Test
    fun theNoteIsTrimmed() {
        assertEquals("Change this line in my Cover letter: “Dear team,” — shorter", ChangeRequest.message("Cover letter", "Dear team,", "  shorter \n"))
    }

    @Test
    fun theChipsAreTheFourAgreedRequests() {
        assertEquals(listOf("Make it shorter", "Make it simpler", "Add something I forgot", "Something else"), ChangeRequest.chips)
    }
}
