package app.kwela.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** The messages Kwela could not answer, each with the reason, so every one can show "Not sent" under it. */
object FailedReplies {
    fun decode(stored: String?): Map<String, String> =
        stored?.let { runCatching { Json.decodeFromString<Map<String, String>>(it) }.getOrNull() }.orEmpty()

    /** Null when nothing has failed, so the stored value can simply be removed. */
    fun encode(failed: Map<String, String>): String? = if (failed.isEmpty()) null else Json.encodeToString(failed)

    /** The reason shown in the drawer. The old "Tap Try again" wording now points at the Resend choice. */
    fun reason(error: String?): String? = error?.replace("Tap Try again", "Tap Resend")
}
