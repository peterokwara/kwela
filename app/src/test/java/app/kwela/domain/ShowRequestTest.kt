package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShowRequestTest {
    @Test
    fun understandsHowPeopleAskToSeeAFile() {
        assertEquals(ShowRequest.CV, ShowRequest.parse("lemme have a look at the cv"))
        assertEquals(ShowRequest.CV, ShowRequest.parse("Show me my resume"))
        assertEquals(ShowRequest.LETTER, ShowRequest.parse("let me see the cover letter"))
        assertEquals(ShowRequest.JOB, ShowRequest.parse("lemme see the job description broken down"))
        assertEquals(ShowRequest.JOB, ShowRequest.parse("open the job breakdown pls"))
        assertEquals(ShowRequest.ANSWERS, ShowRequest.parse("show me the answers"))
    }

    @Test
    fun leavesEverythingElseToTheWriter() {
        assertNull(ShowRequest.parse("make my cv shorter"))
        assertNull(ShowRequest.parse("Change this line in my CV: “Cut errors” — less boastful"))
        assertNull(ShowRequest.parse("please write a cover letter for me"))
        assertNull(ShowRequest.parse("I saw a job I want"))
        assertNull(ShowRequest.parse("can you check my cv for typos"))
        assertNull(ShowRequest.parse("read my cv and tell me what is weak"))
        assertNull(ShowRequest.parse("tell me about the job"))
        assertNull(ShowRequest.parse("About “Cleaned reports”: Dozens"))
        assertNull(ShowRequest.parse("see my cv ".repeat(8)))
    }

    @Test
    fun aDoctorBetweenPatientsGetsTheRightFileWhateverTheTyping() {
        assertEquals(ShowRequest.CV, ShowRequest.parse("cv pls"))
        assertEquals(ShowRequest.CV, ShowRequest.parse("where is my cv"))
        assertEquals(ShowRequest.LETTER, ShowRequest.parse("SHOW ME THE LETTER!!"))
        assertEquals(ShowRequest.JOB, ShowRequest.parse("lemme see the job"))
    }
}
