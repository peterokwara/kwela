package app.kwela.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class SignedIn(val token: String, val email: String, val name: String? = null)

/** Reads the Kwela API's reply to a successful sign-in. */
object SignInReplyParser {
    fun parse(body: String): SignedIn? {
        val root = runCatching { Json.parseToJsonElement(body) }.getOrNull() as? JsonObject ?: return null
        val token = (root["token"] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() } ?: return null
        val email = (root["email"] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() } ?: return null
        val name = (root["name"] as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }
        return SignedIn(token, email, name)
    }
}
