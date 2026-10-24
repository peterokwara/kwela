package app.kwela.domain

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTimeHeaderTest {
    private val nairobi = ZoneId.of("Africa/Nairobi")

    private fun at(day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, nairobi).toInstant().toEpochMilli()

    @Test
    fun theFirstMessageAlwaysGetsAHeader() {
        assertTrue(ChatTimeHeader.shouldShow(previous = null, current = at(24, 9, 2)))
    }

    @Test
    fun aHeaderAppearsAfterAThirtyMinuteGapOnly() {
        assertFalse(ChatTimeHeader.shouldShow(previous = at(24, 9, 0), current = at(24, 9, 29)))
        assertTrue(ChatTimeHeader.shouldShow(previous = at(24, 9, 0), current = at(24, 9, 30)))
    }

    @Test
    fun labelsTodayYesterdayAndOlderDays() {
        val now = at(24, 15, 0)

        assertEquals("Today 9:02 AM", ChatTimeHeader.label(at(24, 9, 2), now, nairobi))
        assertEquals("Yesterday 4:45 PM", ChatTimeHeader.label(at(23, 16, 45), now, nairobi))
        assertEquals("12 Oct, 8:05 AM", ChatTimeHeader.label(at(12, 8, 5), now, nairobi))
    }
}
