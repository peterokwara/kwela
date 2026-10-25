package app.kwela.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Reads the optional private-test code from a successful code request. */
object SignInCodeRequestParser {
    fun parseTestCode(body: String): String? {
        val root = runCatching { Json.parseToJsonElement(body) }.getOrNull() as? JsonObject ?: return null
        return (root["testCode"] as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.content
            ?.takeIf { it.matches(Regex("\\d{6}")) }
    }
}
