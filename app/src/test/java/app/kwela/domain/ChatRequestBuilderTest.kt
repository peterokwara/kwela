package app.kwela.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRequestBuilderTest {
    private fun messages(body: String): JsonArray = Json.parseToJsonElement(body).jsonObject["messages"]!!.jsonArray

    @Test
    fun sendsRolesAndTextInOrder() {
        val body = ChatRequestBuilder.build(
            listOf(
                OutgoingMessage("1", OutgoingRole.USER, "Hello"),
                OutgoingMessage("2", OutgoingRole.ASSISTANT, "Hi, send your CV.")
            ),
            savedProfileJson = null
        )

        val sent = messages(body)
        assertEquals(listOf("user", "assistant"), sent.map { it.jsonObject["role"]!!.jsonPrimitive.content })
        assertEquals(listOf("Hello", "Hi, send your CV."), sent.map { it.jsonObject["content"]!!.jsonPrimitive.content })
        assertFalse(sent[0].jsonObject.containsKey("files"))
    }

    @Test
    fun attachesFilesToUserMessagesAsDataUrls() {
        val file = OutgoingFile("f1", "cv.pdf", "application/pdf", "data:application/pdf;base64,AAAA")
        val body = ChatRequestBuilder.build(listOf(OutgoingMessage("1", OutgoingRole.USER, "My CV", listOf(file))), null)

        val sentFile = messages(body)[0].jsonObject["files"]!!.jsonArray[0].jsonObject
        assertEquals("cv.pdf", sentFile["name"]!!.jsonPrimitive.content)
        assertEquals("application/pdf", sentFile["type"]!!.jsonPrimitive.content)
        assertEquals("data:application/pdf;base64,AAAA", sentFile["data"]!!.jsonPrimitive.content)
    }

    @Test
    fun putsTheSavedCvProfileFirstAndKeepsTwentyMessagesInTotal() {
        val history = (1..30).map { OutgoingMessage("m$it", OutgoingRole.USER, "message $it") }

        val sent = messages(ChatRequestBuilder.build(history, savedProfileJson = """{"professionalSummary":"Mine"}"""))

        assertEquals(20, sent.size)
        val first = sent[0].jsonObject
        assertEquals("assistant", first["role"]!!.jsonPrimitive.content)
        assertEquals("Mine", first["candidateProfile"]!!.jsonObject["professionalSummary"]!!.jsonPrimitive.content)
        assertEquals("message 12", sent[1].jsonObject["content"]!!.jsonPrimitive.content)
        assertEquals("message 30", sent[19].jsonObject["content"]!!.jsonPrimitive.content)
    }

    @Test
    fun dropsFilesFromTheOldestMessagesWhenTheRequestIsTooLarge() {
        val big = "data:application/pdf;base64," + "A".repeat(600)
        val history = listOf(
            OutgoingMessage("1", OutgoingRole.USER, "old", listOf(OutgoingFile("a", "old.pdf", "application/pdf", big))),
            OutgoingMessage("2", OutgoingRole.USER, "new", listOf(OutgoingFile("b", "new.pdf", "application/pdf", big)))
        )

        val sent = messages(ChatRequestBuilder.build(history, null, maxEncodedFileChars = 1_000))

        assertFalse(sent[0].jsonObject.containsKey("files"))
        assertTrue(sent[1].jsonObject.containsKey("files"))
        assertEquals("old", sent[0].jsonObject["content"]!!.jsonPrimitive.content)
    }

    @Test
    fun sendsOnlyTheNewestApplicationDraftBackSoRevisionsBuildOnIt() {
        val history = listOf(
            OutgoingMessage("1", OutgoingRole.USER, "Make me a CV"),
            OutgoingMessage("2", OutgoingRole.ASSISTANT, "First draft", applicationJson = """{"job":{"title":"Old"}}"""),
            OutgoingMessage("3", OutgoingRole.USER, "Add leadership"),
            OutgoingMessage("4", OutgoingRole.ASSISTANT, "Second draft", applicationJson = """{"job":{"title":"New"}}"""),
            OutgoingMessage("5", OutgoingRole.USER, "Move skills up")
        )

        val sent = messages(ChatRequestBuilder.build(history, null))

        assertFalse(sent[1].jsonObject.containsKey("application"))
        assertEquals("New", sent[3].jsonObject["application"]!!.jsonObject["job"]!!.jsonObject["title"]!!.jsonPrimitive.content)
        assertFalse(sent[0].jsonObject.containsKey("application"))
        assertFalse(sent[4].jsonObject.containsKey("application"))
    }

    @Test
    fun leavesOutTheAppsOwnGotItBubblesSoTheWriterNeverSeesThem() {
        val history = listOf(
            OutgoingMessage("1", OutgoingRole.USER, "Here is my CV"),
            OutgoingMessage("local-ack-1", OutgoingRole.ASSISTANT, "Got it, thank you. Give me about a minute."),
            OutgoingMessage("2", OutgoingRole.ASSISTANT, "Okay, done 🙂")
        )

        val sent = messages(ChatRequestBuilder.build(history, null))

        assertEquals(listOf("1", "2"), sent.map { it.jsonObject["id"]!!.jsonPrimitive.content })
    }
}
