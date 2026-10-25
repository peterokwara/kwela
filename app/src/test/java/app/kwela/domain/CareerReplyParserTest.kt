package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CareerReplyParserTest {
    private val pack = """
        {"type":"application_pack","message":"Here is your pack.","candidateProfile":{"professionalSummary":"Careful analyst."},
         "applicationPack":{
           "job":{"title":"Data Analyst","company":"Example Bank"},
           "resume":{
             "personalDetails":{"firstName":"Amina","lastName":"Wanjiru","email":"amina@example.test","phoneNumber":"0700 000 000","city":"Nairobi","country":"Kenya"},
             "professionalSummary":"Careful analyst.",
             "employments":[{"jobTitle":"Data Clerk","employer":"Example Co","startDate":"Jan 2022","endDate":"","isPresent":true,"achievements":["Cleaned reports","Cut errors"]}],
             "education":[{"degree":"BSc Statistics","institution":"Example University","startDate":"2018","endDate":"2022","isPresent":false}],
             "skills":"Excel, SQL"},
           "coverLetter":{"fullName":"Amina Wanjiru","email":"amina@example.test","phoneNumber":"0700 000 000","companyName":"Example Bank","letterContent":"Dear Hiring Team,\n\nI would like to apply."},
           "applicationAnswers":[{"question":"Why us?","answer":"I like your work."}]}}
    """.trimIndent()

    @Test
    fun readsAPlainMessageWhenOtherFieldsAreMissing() {
        val reply = CareerReplyParser.parse("""{"type":"message","message":"Send your CV."}""")!!

        assertEquals("Send your CV.", reply.message)
        assertTrue(reply.documents.isEmpty())
        assertNull(reply.candidateProfileJson)
    }

    @Test
    fun keepsTheCvProfileSoItCanBeSentBackNextTime() {
        val reply = CareerReplyParser.parse(pack)!!

        assertNotNull(reply.candidateProfileJson)
        assertTrue(reply.candidateProfileJson!!.contains("Careful analyst."))
    }

    @Test
    fun turnsAnApplicationPackIntoThreeNamedDocuments() {
        val documents = CareerReplyParser.parse(pack)!!.documents

        assertEquals(
            listOf("CV - Data Analyst, Example Bank", "Cover letter - Data Analyst, Example Bank", "Application answers - Data Analyst, Example Bank"),
            documents.map { it.title }
        )
        assertEquals(
            "Amina Wanjiru\namina@example.test · 0700 000 000 · Nairobi, Kenya\n\n" +
                "PROFESSIONAL SUMMARY\nCareful analyst.\n\n" +
                "EXPERIENCE\nData Clerk, Example Co (Jan 2022 - Present)\n- Cleaned reports\n- Cut errors\n\n" +
                "EDUCATION\nBSc Statistics, Example University (2018 - 2022)\n\n" +
                "SKILLS\nExcel, SQL",
            documents[0].body
        )
        assertEquals(
            "Amina Wanjiru\namina@example.test · 0700 000 000\n\nExample Bank\n\nDear Hiring Team,\n\nI would like to apply.",
            documents[1].body
        )
        assertEquals("Q: Why us?\nA: I like your work.", documents[2].body)
    }

    @Test
    fun keepsTheWholeApplicationPackSoItCanBeRevisedLater() {
        val withPack = CareerReplyParser.parse(pack)!!
        assertTrue(withPack.applicationJson!!.contains("Data Analyst"))
        assertTrue(withPack.applicationJson!!.contains("Why us?"))

        assertNull(CareerReplyParser.parse("""{"type":"message","message":"Send your CV."}""")!!.applicationJson)
    }

    @Test
    fun skipsTheAnswersDocumentWhenThereAreNoAnswers() {
        val withoutAnswers = pack.replace(""""applicationAnswers":[{"question":"Why us?","answer":"I like your work."}]""", """"applicationAnswers":[]""")

        assertEquals(2, CareerReplyParser.parse(withoutAnswers)!!.documents.size)
    }

    @Test
    fun returnsNothingForTextThatIsNotAReply() {
        assertNull(CareerReplyParser.parse("not json"))
        assertNull(CareerReplyParser.parse("""{"type":"message"}"""))
    }
}
