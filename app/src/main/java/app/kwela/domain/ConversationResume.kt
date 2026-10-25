package app.kwela.domain

/** Helps a distracted person come back to where they left off. */
object ConversationResume {
    /** True when the last real message is the person's and nothing is on its way or already offering Try again. */
    fun needsPickUp(lastRealRole: String?, isReplying: Boolean, hasErrorShown: Boolean): Boolean =
        lastRealRole == "USER" && !isReplying && !hasErrorShown
}
