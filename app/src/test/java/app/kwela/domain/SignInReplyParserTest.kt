package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SignInReplyParserTest {
    @Test
    fun readsTheTokenAndEmailFromASuccessfulSignIn() {
        val reply = SignInReplyParser.parse("""{"token":"abc123","email":"amina@example.test"}""")

        assertEquals(SignedIn("abc123", "amina@example.test"), reply)
    }

    @Test
    fun returnsNothingWhenEitherPartIsMissingOrBlank() {
        assertNull(SignInReplyParser.parse("""{"email":"amina@example.test"}"""))
        assertNull(SignInReplyParser.parse("""{"token":" ","email":"amina@example.test"}"""))
        assertNull(SignInReplyParser.parse("not json"))
    }
}
