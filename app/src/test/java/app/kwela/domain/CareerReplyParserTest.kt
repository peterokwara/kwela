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
    fun turnsAnApplicationPackIntoThreeShortlyNamedDocumentsForOneJob() {
        val reply = CareerReplyParser.parse(pack)!!
        val documents = reply.documents

        assertEquals("Data Analyst · Example Bank", reply.groupTitle)
        assertEquals(listOf("CV", "Cover letter", "Answers"), documents.map { it.title })
        assertEquals(listOf("resume", "cover_letter", "answers"), documents.map { it.kind })
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
    fun theCvAndLetterKeepTheirStructuredSourceForTheProperPdfButTheAnswersDoNot() {
        val documents = CareerReplyParser.parse(pack)!!.documents

        assertTrue(documents[0].sourceJson!!.contains("\"professionalSummary\":\"Careful analyst.\""))
        assertTrue(documents[1].sourceJson!!.contains("\"letterContent\""))
        assertNull(documents[2].sourceJson)
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

    @Test
    fun savesAQuickBreakdownOfTheJobWhenTheAdHadDetails() {
        val detailed = pack.replace(
            """"job":{"title":"Data Analyst","company":"Example Bank"}""",
            """"job":{"title":"Data Analyst","company":"Example Bank","location":"Nairobi","salary":"","applicationDeadline":"30 Nov","jobSummary":"Turn bank data into decisions.","keyRequirements":["BSc Statistics","2 years with SQL"],"responsibilities":["Build weekly reports"],"desiredSkills":[],"contactPerson":"","contactEmail":"jobs@example.test"}"""
        )

        val job = CareerReplyParser.parse(detailed)!!.documents.last()

        assertEquals("Job", job.title)
        assertEquals("job", job.kind)
        assertEquals(
            "Data Analyst\nExample Bank · Nairobi · Apply by 30 Nov\n\n" +
                "THE JOB IN A LINE\nTurn bank data into decisions.\n\n" +
                "WHAT THEY WANT\n- BSc Statistics\n- 2 years with SQL\n\n" +
                "WHAT YOU WILL DO\n- Build weekly reports\n\n" +
                "SEND IT TO\njobs@example.test",
            job.body
        )
    }

    @Test
    fun showsEverySectionOfTheCvNotJustTheFirstFour() {
        val full = """
            {"type":"application_pack","message":"Done.","applicationPack":{"job":{"title":"Analyst","company":"Bank"},"resume":{
              "personalDetails":{"firstName":"Amina","lastName":"Wanjiru","email":"a@example.test","phoneNumber":"0700","city":"Nairobi","country":"Kenya"},
              "professionalSummary":"Careful analyst.",
              "employments":[{"jobTitle":"Clerk","employer":"Co","startDate":"2020","endDate":"","isPresent":true,"achievements":["Cleaned reports"]}],
              "education":[],"skills":"Excel",
              "projects":[{"name":"Budget app","description":"Tracks spending","technologies":["Kotlin"],"url":"","startDate":"","endDate":""}],
              "certifications":[{"name":"AWS Cloud Practitioner","issuer":"Amazon","issueDate":"2024","expiryDate":"","credentialId":"ABC123","url":""}],
              "accomplishments":[{"title":"Employee of the year","description":"Top score","date":"2023"}],
              "referees":[{"name":"Jo Otieno","position":"Manager","company":"Co","email":"jo@example.test","phone":"0711"}],
              "languages":[{"language_name":"Swahili","proficiency":"Native","certification":""}],
              "professionalDevelopment":[{"course_name":"Leadership","institution":"KIM","issued_date":"2022","certificate_id":"","description":""}],
              "professionalMemberships":[{"organization_name":"ICPAK","membership_number":"1","membership_type":"Member","member_since":"2021","expires_on":"","notes":""}],
              "volunteerExperience":[{"organization":"Red Cross","role":"Volunteer","start_date":"2019","end_date":"2020","description":"First aid"}]
            }}}
        """.trimIndent()

        val body = CareerReplyParser.parse(full)!!.documents.first().body

        assertTrue(body.contains("CERTIFICATIONS\nAWS Cloud Practitioner, Amazon (2024 · ABC123)"))
        assertTrue(body.contains("PROJECTS\nBudget app (Kotlin): Tracks spending"))
        assertTrue(body.contains("ACCOMPLISHMENTS\nEmployee of the year (2023): Top score"))
        assertTrue(body.contains("PROFESSIONAL DEVELOPMENT\nLeadership, KIM (2022)"))
        assertTrue(body.contains("PROFESSIONAL MEMBERSHIPS\nICPAK, Member (2021)"))
        assertTrue(body.contains("VOLUNTEER EXPERIENCE\nVolunteer, Red Cross (2019 - 2020): First aid"))
        assertTrue(body.contains("LANGUAGES\nSwahili, Native"))
        assertTrue(body.contains("REFEREES\nJo Otieno, Manager, Co, jo@example.test, 0711"))
        // Same order as the PDF: certifications come before skills, referees last.
        assertTrue(body.indexOf("CERTIFICATIONS") < body.indexOf("SKILLS"))
        assertTrue(body.indexOf("SKILLS") < body.indexOf("REFEREES"))
    }

    @Test
    fun followsTheSectionOrderTheCvAsksFor() {
        val ordered = """
            {"type":"application_pack","message":"Done.","applicationPack":{"job":{"title":"A","company":"B"},"resume":{
              "personalDetails":{"firstName":"A","lastName":"B","email":"","phoneNumber":"","city":"","country":""},
              "professionalSummary":"Summary.","employments":[],"education":[],"skills":"Excel",
              "certifications":[{"name":"CPA","issuer":"ICPAK","issueDate":"","expiryDate":"","credentialId":"","url":""}],
              "sectionOrder":["certifications","professionalSummary","skills"]
            }}}
        """.trimIndent()

        val body = CareerReplyParser.parse(ordered)!!.documents.first().body

        assertTrue(body.indexOf("CERTIFICATIONS") < body.indexOf("PROFESSIONAL SUMMARY"))
        assertTrue(body.indexOf("PROFESSIONAL SUMMARY") < body.indexOf("SKILLS"))
    }
}
