package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiMessagesTest {
    @Test
    fun usesTheServersOwnWordsWhenItSendsSome() {
        assertEquals("You have used all of your free Kwela credit.", ApiMessages.forStatus(429, "You have used all of your free Kwela credit."))
    }

    @Test
    fun fallsBackToPlainWordsWhenTheServerSaysNothing() {
        assertEquals("Please sign in again.", ApiMessages.forStatus(401, null))
        assertEquals("That conversation is too large. Remove a big file or start a new chat.", ApiMessages.forStatus(413, null))
        assertEquals("You are sending messages too fast. Wait a moment and try again.", ApiMessages.forStatus(429, " "))
        assertEquals("Kwela is unavailable right now. Try again in a moment.", ApiMessages.forStatus(503, null))
        assertEquals("Something went wrong. Please try again.", ApiMessages.forStatus(418, null))
    }

    @Test
    fun explainsAMissingConnection() {
        assertEquals("Couldn’t reach Kwela. Check your connection and try again.", ApiMessages.offline())
    }
}
