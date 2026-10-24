package app.kwela.domain

/** Plain-language messages shown when a call to the Kwela API does not succeed. */
object ApiMessages {
    fun forStatus(status: Int, serverMessage: String?): String {
        serverMessage?.takeIf { it.isNotBlank() }?.let { return it }
        return when (status) {
            401 -> "Please sign in again."
            413 -> "That conversation is too large. Remove a big file or start a new chat."
            429 -> "You are sending messages too fast. Wait a moment and try again."
            500, 502, 503, 504 -> "Kwela is unavailable right now. Try again in a moment."
            else -> "Something went wrong. Please try again."
        }
    }

    fun offline(): String = "Couldn’t reach Kwela. Check your connection and try again."
}
