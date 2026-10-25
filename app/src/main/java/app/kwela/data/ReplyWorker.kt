package app.kwela.data

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.kwela.KwelaApplication
import app.kwela.data.notify.ReplyNotifications
import app.kwela.domain.NotificationRules
import app.kwela.domain.NotificationText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Asks the writer for a reply in the background, so it still arrives if the person leaves the app. */
class ReplyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as KwelaApplication
        val outcome = ReplyEngine(applicationContext, app.repository, app.api).run()
        val notifications = ReplyNotifications(applicationContext)
        val allowed = outcome.replyText != null && NotificationRules.shouldNotify(
            appInForeground = isAppInForeground(),
            repliesEnabled = app.repository.notificationsEnabled.first(),
            permissionGranted = notifications.canPost()
        )
        // Show the reply, or clear any "sending" notification left by an inline reply so it never hangs around.
        if (allowed) notifications.show(NotificationText.lines(outcome.replyText.orEmpty())) else notifications.cancel()
        val output = Data.Builder().putBoolean(KEY_EXPIRED, outcome.sessionExpired).apply { outcome.error?.let { putString(KEY_ERROR, it) } }.build()
        return Result.success(output)
    }

    private suspend fun isAppInForeground(): Boolean =
        withContext(Dispatchers.Main) { ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }

    companion object {
        const val UNIQUE = "kwela-reply"
        const val TAG_SLOW = "slow"
        const val KEY_ERROR = "error"
        const val KEY_EXPIRED = "expired"
    }
}

/** Starts a reply job. A new request replaces any earlier one still waiting. */
object ReplyScheduler {
    fun enqueue(context: Context, slow: Boolean) {
        val request = OneTimeWorkRequestBuilder<ReplyWorker>().apply { if (slow) addTag(ReplyWorker.TAG_SLOW) }.build()
        WorkManager.getInstance(context).enqueueUniqueWork(ReplyWorker.UNIQUE, ExistingWorkPolicy.REPLACE, request)
    }
}
