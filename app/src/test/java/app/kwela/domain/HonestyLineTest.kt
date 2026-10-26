package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HonestyLineTest {
    @Test
    fun noticesWhenKwelaHasAlreadySaidIt() {
        assertTrue(HonestyLine.alreadySaid(listOf("Hi!", "I’m a digital helper, not a person, so read the details before you send it.")))
        assertFalse(HonestyLine.alreadySaid(listOf("Hi!", "Your CV is ready.")))
    }

    @Test
    fun removesTheSentenceButKeepsTheRestOfTheBubble() {
        val reply = "I’m a digital helper, not a person, so read the details before you send it. Hope it feels like you. Next: review the details."

        assertEquals("Hope it feels like you. Next: review the details.", HonestyLine.strip(reply))
    }

    @Test
    fun dropsABubbleThatWasOnlyThatSentence() {
        val reply = "Your CV is ready.\n\nI’m a digital helper, not a person, so read the details before you send it.\n\nHope it feels like you."

        assertEquals("Your CV is ready.\n\nHope it feels like you.", HonestyLine.strip(reply))
    }

    @Test
    fun leavesAReplyWithoutTheLineAlone() {
        assertEquals("Your CV is ready.\n\nNext: have a look.", HonestyLine.strip("Your CV is ready.\n\nNext: have a look."))
    }
}
