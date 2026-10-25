package app.kwela.domain

/** Decides how a wait should feel. Slow requests get an instant "Got it" bubble. Quick ones just show typing dots. */
object ReplyPacing {
    private const val LONG_MESSAGE_CHARS = 250

    fun isSlow(content: String, hasAttachment: Boolean): Boolean =
        hasAttachment || content.length > LONG_MESSAGE_CHARS || content.contains("http://") || content.contains("https://")
}
