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

    @Test
    fun readsTheNameWhenGoogleSendsOne() {
        val reply = SignInReplyParser.parse("""{"token":"abc123","email":"amina@example.test","name":"Amina Wanjiru"}""")

        assertEquals(SignedIn("abc123", "amina@example.test", "Amina Wanjiru"), reply)
    }

    @Test
    fun aBlankNameIsTreatedAsNoName() {
        assertNull(SignInReplyParser.parse("""{"token":"abc123","email":"a@example.test","name":"  "}""")?.name)
    }

    @Test
    fun readsTheAnalyticsIdAndWhetherTheAccountIsInternal() {
        val reply = SignInReplyParser.parse("""{"token":"t","email":"a@example.test","analyticsId":"abc123","analyticsInternal":true}""")

        assertEquals("abc123", reply?.analyticsId)
        assertEquals(true, reply?.analyticsInternal)
    }

    @Test
    fun anOlderReplyWithoutThemStillSignsInAndIsNotInternal() {
        val reply = SignInReplyParser.parse("""{"token":"t","email":"a@example.test"}""")

        assertNull(reply?.analyticsId)
        assertEquals(false, reply?.analyticsInternal)
    }
}
