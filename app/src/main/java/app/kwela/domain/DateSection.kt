package app.kwela.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** WhatsApp-style date headings, so a person can tell at a glance which files are from today, yesterday and so on. */
object DateSection {
    private val monthAndYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    fun label(at: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val day = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        if (!day.isBefore(today)) return "Today"
        if (day == today.minusDays(1)) return "Yesterday"
        val thisWeekStart = today.with(DayOfWeek.MONDAY)
        if (!day.isBefore(thisWeekStart)) return "This week"
        if (!day.isBefore(thisWeekStart.minusWeeks(1))) return "Last week"
        val thisMonth = YearMonth.from(today)
        return when (YearMonth.from(day)) {
            thisMonth -> "This month"
            thisMonth.minusMonths(1) -> "Last month"
            else -> day.format(monthAndYear)
        }
    }
}
