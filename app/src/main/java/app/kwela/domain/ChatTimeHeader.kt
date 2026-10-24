package app.kwela.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ChatTimeHeader {
    private const val GAP_MILLIS = 30 * 60 * 1000L
    private val time = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val olderDay = DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH)

    fun shouldShow(previous: Long?, current: Long): Boolean = previous == null || current - previous >= GAP_MILLIS

    fun label(at: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val moment = Instant.ofEpochMilli(at).atZone(zone)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return when (moment.toLocalDate()) {
            today -> "Today ${time.format(moment)}"
            today.minusDays(1) -> "Yesterday ${time.format(moment)}"
            else -> olderDay.format(moment)
        }
    }
}
