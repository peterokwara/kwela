package app.kwela.domain

import java.net.URI

data class SharedPayload(val kind: Kind, val value: String) {
    enum class Kind { TEXT, LINK }
}

object SharedPayloadParser {
    fun parse(rawText: String?): SharedPayload? {
        val value = rawText?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val uri = runCatching { URI(value) }.getOrNull()
        val kind = if (uri?.scheme.equals("https", ignoreCase = true) || uri?.scheme.equals("http", ignoreCase = true)) {
            SharedPayload.Kind.LINK
        } else {
            SharedPayload.Kind.TEXT
        }
        return SharedPayload(kind, value)
    }
}
