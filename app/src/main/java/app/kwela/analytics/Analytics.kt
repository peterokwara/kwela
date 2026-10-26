package app.kwela.analytics

import android.util.Log

/** Where events end up. The real one is PostHog; tests use a recorder. */
interface AnalyticsSink {
    fun capture(event: String, properties: Map<String, Any>)
    fun identify(distinctId: String, properties: Map<String, Any>)
    fun reset()
    fun setOptedOut(optedOut: Boolean)
}

/**
 * The one door every analytics call goes through. It can be switched off by the person, it tags internal and test accounts,
 * and a failing sender can never break the app. Properties hold counts and kinds only, never a CV, a message, a file name or an email.
 */
class Analytics(
    private val sink: AnalyticsSink,
    /** True for debug builds, so a developer's own phone never counts as a real user. */
    private val internalByDefault: Boolean = false,
    private val onProblem: (Exception) -> Unit = { Log.w("KwelaAnalytics", "Analytics failed, ignoring it", it) }
) {
    // Off until the person's saved choice has been read, so an opt-out is honoured from the very first event.
    @Volatile private var enabled = false
    @Volatile private var internal = internalByDefault

    fun track(event: String, properties: Map<String, Any> = emptyMap()) {
        if (!enabled) return
        safely { sink.capture(event, if (internal) properties + (INTERNAL to true) else properties) }
    }

    /** Links this phone to the account id the server made. That id is opaque and carries no email. */
    fun identify(distinctId: String, internal: Boolean) {
        this.internal = internal
        safely { sink.identify(distinctId, mapOf(INTERNAL to internal)) }
    }

    /** Forgets the person, on sign-out and on delete. */
    fun reset() {
        internal = internalByDefault
        safely { sink.reset() }
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        safely { sink.setOptedOut(!enabled) }
    }

    private inline fun safely(action: () -> Unit) {
        try {
            action()
        } catch (error: Exception) {
            onProblem(error)
        }
    }

    private companion object {
        const val INTERNAL = "is_internal_user"
    }
}
