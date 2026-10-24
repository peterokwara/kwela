package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedPayloadParserTest {
    @Test
    fun keepsPlainJobDescriptionText() {
        assertEquals(
            SharedPayload(SharedPayload.Kind.TEXT, "Graduate intern wanted in Nairobi"),
            SharedPayloadParser.parse("  Graduate intern wanted in Nairobi  ")
        )
    }

    @Test
    fun recognizesHttpAndHttpsLinks() {
        assertEquals(
            SharedPayload(SharedPayload.Kind.LINK, "https://jobs.example/intern"),
            SharedPayloadParser.parse(" https://jobs.example/intern ")
        )
        assertEquals(
            SharedPayload(SharedPayload.Kind.LINK, "http://jobs.example/intern"),
            SharedPayloadParser.parse("http://jobs.example/intern")
        )
    }

    @Test
    fun ignoresEmptyTextAndUnsupportedSchemes() {
        assertNull(SharedPayloadParser.parse(" \n "))
        assertEquals(
            SharedPayload(SharedPayload.Kind.TEXT, "file:///private/job.pdf"),
            SharedPayloadParser.parse("file:///private/job.pdf")
        )
    }
}
