package app.kwela.domain

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DateSectionTest {
    private val nairobi = ZoneId.of("Africa/Nairobi")

    // Today is Wednesday 21 October 2026.
    private val now = at(2026, 10, 21, 15, 0)

    private fun at(year: Int, month: Int, day: Int, hour: Int = 9, minute: Int = 0) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, nairobi).toInstant().toEpochMilli()

    private fun label(timeMillis: Long) = DateSection.label(timeMillis, now, nairobi)

    @Test
    fun todayAndYesterday() {
        assertEquals("Today", label(at(2026, 10, 21, 8)))
        assertEquals("Yesterday", label(at(2026, 10, 20, 23, 59)))
    }

    @Test
    fun earlierDaysOfTheSameWeekAreThisWeek() {
        assertEquals("This week", label(at(2026, 10, 19)))
    }

    @Test
    fun theWeekBeforeIsLastWeek() {
        assertEquals("Last week", label(at(2026, 10, 18)))
        assertEquals("Last week", label(at(2026, 10, 12)))
    }

    @Test
    fun earlierInTheSameMonthIsThisMonth() {
        assertEquals("This month", label(at(2026, 10, 5)))
    }

    @Test
    fun thePreviousCalendarMonthIsLastMonth() {
        assertEquals("Last month", label(at(2026, 9, 30)))
        assertEquals("Last month", label(at(2026, 9, 1)))
    }

    @Test
    fun olderThanThatIsTheMonthAndYear() {
        assertEquals("August 2026", label(at(2026, 8, 15)))
        assertEquals("December 2025", label(at(2025, 12, 31)))
    }

    @Test
    fun afterANewYearLastDecemberIsLastMonth() {
        val january = at(2027, 1, 10)
        assertEquals("Last month", DateSection.label(at(2026, 12, 20), january, nairobi))
    }
}
