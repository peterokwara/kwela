package app.kwela.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class OutgoingRole(val wireName: String) { USER("user"), ASSISTANT("assistant") }

data class OutgoingFile(val id: String, val name: String, val type: String, val dataUrl: String)

data class OutgoingMessage(
    val id: String,
    val role: OutgoingRole,
    val content: String,
    val files: List<OutgoingFile> = emptyList(),
    val applicationJson: String? = null
)

/** Builds the JSON body the Kwela API expects for one chat turn. */
object ChatRequestBuilder {
    private const val DEFAULT_MAX_ENCODED_FILE_CHARS = 28_000_000
    private const val SAVED_PROFILE_NOTE = "Saved CV profile for this person. Reuse it when preparing job materials."

    fun build(
        messages: List<OutgoingMessage>,
        savedProfileJson: String?,
        maxEncodedFileChars: Int = DEFAULT_MAX_ENCODED_FILE_CHARS,
        conversationContextJson: String? = null
    ): String {
        val window = messages.filterNot { LocalMessages.isLocal(it.id) }
        require(window.size + (if (savedProfileJson != null) 1 else 0) <= ConversationContextPolicy.MAX_MESSAGES) { "Too many unsummarized messages" }
        val kept = keepNewestFiles(window, maxEncodedFileChars)
        val newestDraft = kept.indexOfLast { it.role == OutgoingRole.ASSISTANT && it.applicationJson != null }
        val body = buildJsonObject {
            put("messages", buildJsonArray {
                if (savedProfileJson != null) {
                    add(buildJsonObject {
                        put("id", "saved-cv-profile")
                        put("role", OutgoingRole.ASSISTANT.wireName)
                        put("content", SAVED_PROFILE_NOTE)
                        put("candidateProfile", Json.parseToJsonElement(savedProfileJson))
                    })
                }
                kept.forEachIndexed { index, message -> add(messageJson(message, includeDraft = index == newestDraft)) }
            })
            if (conversationContextJson != null) put("conversationContext", Json.parseToJsonElement(conversationContextJson))
        }
        return body.toString()
    }

    private fun keepNewestFiles(messages: List<OutgoingMessage>, limit: Int): List<OutgoingMessage> {
        var total = messages.sumOf { message -> message.files.sumOf { it.dataUrl.length } }
        return messages.map { message ->
            if (total > limit && message.files.isNotEmpty()) {
                total -= message.files.sumOf { it.dataUrl.length }
                message.copy(files = emptyList())
            } else message
        }
    }

    private fun messageJson(message: OutgoingMessage, includeDraft: Boolean): JsonObject = buildJsonObject {
        put("id", message.id)
        put("role", message.role.wireName)
        put("content", message.content)
        if (includeDraft && message.applicationJson != null) put("application", Json.parseToJsonElement(message.applicationJson))
        if (message.role == OutgoingRole.USER && message.files.isNotEmpty()) {
            put("files", JsonArray(message.files.map { file ->
                buildJsonObject {
                    put("id", file.id)
                    put("name", file.name)
                    put("type", file.type)
                    put("data", JsonPrimitive(file.dataUrl))
                }
            }))
        }
    }
}
