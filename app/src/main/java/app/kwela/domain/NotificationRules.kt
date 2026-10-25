package app.kwela.domain

/** When Kwela may tap someone on the shoulder. Restraint is the point: only for a reply they asked for, only after they have left. */
object NotificationRules {
    fun shouldNotify(appInForeground: Boolean, repliesEnabled: Boolean, permissionGranted: Boolean): Boolean =
        !appInForeground && repliesEnabled && permissionGranted
}

/** What a notification says, kept short like a chat app's. */
object NotificationText {
    const val PUBLIC_TEXT = "New message from Kwela"
    private const val MAX_LINES = 3
    private const val MAX_LENGTH = 140

    /** The first few bubbles of a reply, each shortened so it fits on one line of the lock screen. */
    fun lines(reply: String): List<String> =
        BubbleSplitter.split(reply).take(MAX_LINES).map { if (it.length > MAX_LENGTH) it.take(MAX_LENGTH - 1).trimEnd() + "…" else it }

    /** The text typed into the notification's reply box, or null when there is nothing to send. */
    fun cleanReply(typed: CharSequence?): String? = typed?.toString()?.trim()?.takeIf { it.isNotEmpty() }
}
