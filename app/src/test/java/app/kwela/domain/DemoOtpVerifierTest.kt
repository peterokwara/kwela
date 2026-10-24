package app.kwela.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoOtpVerifierTest {
    @Test
    fun acceptsTheDisplayedDemoCode() {
        assertTrue(DemoOtpVerifier.isValid("123456"))
    }

    @Test
    fun rejectsWrongOrMalformedCodes() {
        assertFalse(DemoOtpVerifier.isValid("654321"))
        assertFalse(DemoOtpVerifier.isValid("12345"))
        assertFalse(DemoOtpVerifier.isValid("12345a"))
    }
}
