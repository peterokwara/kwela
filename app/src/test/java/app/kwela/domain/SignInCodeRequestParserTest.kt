package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SignInCodeRequestParserTest {
    @Test
    fun readsOnlyASixDigitPrivateTestCode() {
        assertEquals("123456", SignInCodeRequestParser.parseTestCode("""{"ok":true,"testCode":"123456"}"""))
        assertNull(SignInCodeRequestParser.parseTestCode("""{"ok":true}"""))
        assertNull(SignInCodeRequestParser.parseTestCode("""{"testCode":"12345x"}"""))
        assertNull(SignInCodeRequestParser.parseTestCode("not json"))
    }
}
