package app.kwela.domain

/** Messages the app writes itself, like "Got it, give me a minute". They are shown in the chat but never sent to the writer. */
object LocalMessages {
    const val PREFIX = "local-"

    fun isLocal(id: String): Boolean = id.startsWith(PREFIX)

    /** Role of the newest message that is not one of the app's own, or null when there is none. */
    fun lastRealRole(messages: List<Pair<String, String>>): String? = messages.lastOrNull { !isLocal(it.first) }?.second
}
