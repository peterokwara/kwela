package app.kwela.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsEventsTest {
    private val verbs = listOf("click", "view", "create", "update", "delete", "send", "resend", "fail", "generate", "start", "end", "open", "download", "share", "render", "sign_in")
    private val convention = Regex("^[a-z]+(_[a-z]+)*:[a-z]+(_[a-z]+)*_(${verbs.joinToString("|")})$")

    @Test
    fun everyEventNameFollowsCategoryObjectActionInLowercaseSnakeCase() {
        AnalyticsEvents.all.forEach { assertTrue("$it breaks the convention", convention.matches(it)) }
    }

    @Test
    fun noEventNameIsUsedTwice() {
        assertEquals(AnalyticsEvents.all.size, AnalyticsEvents.all.toSet().size)
    }

    @Test
    fun theAppNeverSendsAnEventTheServerAlreadyOwns() {
        val server = setOf("account:user_create", "account:user_sign_in", "account:user_delete", "chat:application_generate", "chat:run_fail", "document:pdf_render")
        assertTrue(AnalyticsEvents.all.none { it in server })
    }

    @Test
    fun nothingIsSentUntilTheSavedChoiceHasBeenRead() {
        val sink = RecordingSink()
        Analytics(sink).track(AnalyticsEvents.messageSend)
        assertTrue(sink.events.isEmpty())
    }

    @Test
    fun theFacadeSendsNothingWhileAnalyticsIsOff() {
        val sink = RecordingSink()
        val analytics = Analytics(sink)
        analytics.setEnabled(false)
        analytics.track(AnalyticsEvents.messageSend)
        assertTrue(sink.events.isEmpty())
        analytics.setEnabled(true)
        analytics.track(AnalyticsEvents.messageSend, mapOf("has_attachment" to false))
        assertEquals(listOf(AnalyticsEvents.messageSend), sink.events.map { it.first })
    }

    @Test
    fun aBrokenSinkNeverBreaksTheApp() {
        val problems = mutableListOf<Exception>()
        val analytics = Analytics(sink = object : AnalyticsSink {
            override fun capture(event: String, properties: Map<String, Any>) = error("posthog down")
            override fun identify(distinctId: String, properties: Map<String, Any>) = error("posthog down")
            override fun reset() = error("posthog down")
            override fun setOptedOut(optedOut: Boolean) = error("posthog down")
        }, onProblem = { problems += it })
        analytics.setEnabled(true)
        analytics.track(AnalyticsEvents.messageSend)
        analytics.identify("id", internal = false)
        analytics.reset()
        analytics.setEnabled(false)
        assertEquals(5, problems.size)
    }

    @Test
    fun identifyingTagsInternalAccountsAndNeverUsesAnEmail() {
        val sink = RecordingSink()
        Analytics(sink).identify("abc123", internal = true)
        assertEquals("abc123", sink.identified.single().first)
        assertEquals(true, sink.identified.single().second["is_internal_user"])
        assertFalse(sink.identified.single().first.contains("@"))
    }

    @Test
    fun internalAccountsTagEveryLaterEventToo() {
        val sink = RecordingSink()
        val analytics = Analytics(sink)
        analytics.setEnabled(true)
        analytics.identify("abc123", internal = true)
        analytics.track(AnalyticsEvents.messageSend)
        assertEquals(true, sink.events.single().second["is_internal_user"])
    }
}

private class RecordingSink : AnalyticsSink {
    val events = mutableListOf<Pair<String, Map<String, Any>>>()
    val identified = mutableListOf<Pair<String, Map<String, Any>>>()
    override fun capture(event: String, properties: Map<String, Any>) { events += event to properties }
    override fun identify(distinctId: String, properties: Map<String, Any>) { identified += distinctId to properties }
    override fun reset() {}
    override fun setOptedOut(optedOut: Boolean) {}
}
