package app.kwela.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Finds missing contact details in a CV before it goes out. Only CVs with a structured source are checked. */
object ContactGaps {
    fun of(resumeJson: String?): List<String> {
        val root = resumeJson?.let { runCatching { Json.parseToJsonElement(it) }.getOrNull() } as? JsonObject ?: return emptyList()
        val details = root["personalDetails"] as? JsonObject ?: return emptyList()
        fun has(key: String) = ((details[key] as? JsonPrimitive)?.takeIf { it.isString }?.content).orEmpty().isNotBlank()
        return buildList {
            if (!has("firstName") && !has("lastName")) add("name")
            if (!has("phoneNumber")) add("phone number")
            if (!has("email")) add("email address")
        }
    }

    /** "your phone number", "your phone number and email address", "your name, phone number and email address". */
    fun sentence(gaps: List<String>): String = "your " + when (gaps.size) {
        0, 1 -> gaps.joinToString("")
        else -> gaps.dropLast(1).joinToString(", ") + " and " + gaps.last()
    }
}
