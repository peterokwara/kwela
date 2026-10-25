package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BubbleSplitterTest {
    @Test
    fun blankLinesStartANewBubble() {
        assertEquals(listOf("Okay, done 🙂", "Your CV is ready.", "Next: tap a line to change it."), BubbleSplitter.split("Okay, done 🙂\n\nYour CV is ready.\n\nNext: tap a line to change it."))
    }

    @Test
    fun spacesAndExtraBlankLinesAreTidiedAway() {
        assertEquals(listOf("Hello", "How are you?"), BubbleSplitter.split("  Hello  \n\n\n\n  How are you?  \n"))
    }

    @Test
    fun singleLineBreaksStayInsideOneBubbleSoShortListsSurvive() {
        assertEquals(listOf("Quick check:\n- phone number\n- end date"), BubbleSplitter.split("Quick check:\n- phone number\n- end date"))
    }

    @Test
    fun anEmptyReplyHasNoBubbles() {
        assertEquals(emptyList<String>(), BubbleSplitter.split("  \n\n "))
    }
}
