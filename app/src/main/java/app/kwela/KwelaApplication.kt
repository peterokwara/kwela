package app.kwela

import android.app.Application
import androidx.room.Room
import app.kwela.analytics.Analytics
import app.kwela.analytics.AnalyticsEvents
import app.kwela.analytics.NoAnalyticsSink
import app.kwela.analytics.PostHogSink
import app.kwela.data.KwelaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import app.kwela.data.notify.ReplyNotifications
import app.kwela.data.local.KwelaDatabase
import app.kwela.data.remote.KwelaApi

class KwelaApplication : Application() {
    /** Counts which parts of the app are used. Off for a person who switches it off in Settings. */
    lateinit var analytics: Analytics
        private set

    override fun onCreate() {
        super.onCreate()
        ReplyNotifications(this).ensureChannel()
        val sink = if (BuildConfig.POSTHOG_API_KEY.isBlank()) NoAnalyticsSink else PostHogSink(this, BuildConfig.POSTHOG_API_KEY, BuildConfig.POSTHOG_HOST)
        analytics = Analytics(sink, internalByDefault = BuildConfig.DEBUG)
        // Read what the person chose and who they are before sending anything, so an opt-out is honoured from the first event.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            analytics.setEnabled(repository.analyticsEnabled.first())
            repository.analyticsIdentity()?.let { (id, internal) -> analytics.identify(id, internal || BuildConfig.DEBUG) }
            analytics.track(AnalyticsEvents.sessionStart)
        }
    }

    val database: KwelaDatabase by lazy {
        Room.databaseBuilder(this, KwelaDatabase::class.java, "kwela.db").addMigrations(KwelaDatabase.MIGRATION_1_2, KwelaDatabase.MIGRATION_2_3, KwelaDatabase.MIGRATION_3_4).build()
    }

    val repository: KwelaRepository by lazy { KwelaRepository(this, database) }

    val api: KwelaApi by lazy { KwelaApi(BuildConfig.API_BASE_URL) }
}
