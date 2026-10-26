package app.kwela.analytics

import android.app.Application
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

/** Sends events to PostHog straight from the phone, with no proxy. */
class PostHogSink(application: Application, apiKey: String, host: String) : AnalyticsSink {
    init {
        val config = PostHogAndroidConfig(apiKey, host).apply {
            // Kwela names its own events. The automatic ones use different names and would add noise.
            captureApplicationLifecycleEvents = false
            captureScreenViews = false
            captureDeepLinks = false
            sessionReplay = false
        }
        PostHogAndroid.setup(application, config)
    }

    override fun capture(event: String, properties: Map<String, Any>) = PostHog.capture(event, properties = properties)

    override fun identify(distinctId: String, properties: Map<String, Any>) = PostHog.identify(distinctId, userProperties = properties)

    override fun reset() = PostHog.reset()

    override fun setOptedOut(optedOut: Boolean) = if (optedOut) PostHog.optOut() else PostHog.optIn()
}

/** Used when no PostHog key is set, so the app runs the same with analytics off. */
object NoAnalyticsSink : AnalyticsSink {
    override fun capture(event: String, properties: Map<String, Any>) = Unit
    override fun identify(distinctId: String, properties: Map<String, Any>) = Unit
    override fun reset() = Unit
    override fun setOptedOut(optedOut: Boolean) = Unit
}
