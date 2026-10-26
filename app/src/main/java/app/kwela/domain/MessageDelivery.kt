package app.kwela.domain

/** Where a message the person sent has got to, like the line under a message in Google Messages. */
enum class Delivery { SENT, WORKING, WAITING, FAILED }

object MessageDelivery {
    private const val TAG_PREFIX = "msg:"

    /** Tag put on a message's background job, so the job can be found and stopped from the message. */
    fun tag(messageId: String): String = TAG_PREFIX + messageId

    fun messageIdFromTags(tags: Set<String>): String? = tags.firstOrNull { it.startsWith(TAG_PREFIX) }?.removePrefix(TAG_PREFIX)

    /**
     * @param failedIds the messages that could not be answered.
     * @param pendingWaiting messages still being worked on, each mapped to whether it is waiting for a connection.
     * @param slow whether this message is one that takes a while, so quick replies show no extra line.
     */
    fun of(messageId: String, failedIds: Set<String>, pendingWaiting: Map<String, Boolean>, slow: Boolean): Delivery {
        val waiting = pendingWaiting[messageId]
        return when {
            waiting == true -> Delivery.WAITING
            waiting == false -> if (slow) Delivery.WORKING else Delivery.SENT
            messageId in failedIds -> Delivery.FAILED
            else -> Delivery.SENT
        }
    }

    fun line(delivery: Delivery): String? = when (delivery) {
        Delivery.SENT -> null
        Delivery.WORKING -> "Working on it."
        Delivery.WAITING -> "Waiting to connect."
        Delivery.FAILED -> "Not sent."
    }

    fun hasOptions(delivery: Delivery): Boolean = delivery != Delivery.SENT
}
