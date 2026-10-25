package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptsTest {
    private val finding = ReviewFinding("no_numbers", "Cleaned weekly sales reports for the regional team", "Roughly how many, or how often?", listOf("A few", "Dozens", "Hundreds", "I don’t count"))

    @Test
    fun theQuestionIsOneShortBubbleQuotingTheLine() {
        assertEquals("Quick one: “Cleaned weekly sales reports for the regional team” Roughly how many, or how often?", ReviewPrompts.bubble(finding))
    }

    @Test
    fun aVeryLongLineIsShortenedSoTheBubbleStaysShort() {
        val long = finding.copy(line = "Cleaned and reconciled weekly sales reports across all regional teams and filed them with the head office every Friday")

        val bubble = ReviewPrompts.bubble(long)

        assertEquals("Quick one: “Cleaned and reconciled weekly sales reports across all re…” Roughly how many, or how often?", bubble)
    }

    @Test
    fun aTappedAnswerBecomesAMessageThatNamesTheLine() {
        assertEquals("About “Cleaned weekly sales reports for the regional team”: Dozens", ReviewPrompts.answer(finding, "Dozens"))
    }

    @Test
    fun theReplyParserReadsFindingsAndIgnoresMalformedOnes() {
        val reply = CareerReplyParser.parse(
            """{"type":"message","message":"Hi","review":[
                {"kind":"duty_phrase","line":"Responsible for stock","question":"What changed because of your work on this?","options":["It got faster","Not sure"]},
                {"kind":"no_numbers","line":"","question":"x","options":[]},
                {"nonsense":true}]}"""
        )!!

        assertEquals(listOf(ReviewFinding("duty_phrase", "Responsible for stock", "What changed because of your work on this?", listOf("It got faster", "Not sure"))), reply.review)
        assertEquals(emptyList<ReviewFinding>(), CareerReplyParser.parse("""{"type":"message","message":"Hi"}""")!!.review)
        assertNull(null)
    }

    @Test
    fun theWriterAsksAtMostTwoQuickQuestionsInARow() {
        val quick = "Quick one: “Kept stock records” Roughly how many, or how often?"

        assertTrue(ReviewPrompts.shouldAsk(listOf("Okay, done 🙂", "Your CV is ready.")))
        assertTrue(ReviewPrompts.shouldAsk(listOf("Okay, done 🙂", quick, "Updated that line.")))
        assertFalse(ReviewPrompts.shouldAsk(listOf(quick, "Updated that line.", quick, "Updated that line too.")))
    }
}
