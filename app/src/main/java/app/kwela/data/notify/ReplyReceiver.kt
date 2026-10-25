package app.kwela.data.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import app.kwela.KwelaApplication
import app.kwela.data.ReplyScheduler
import app.kwela.data.local.ChatMessageEntity
import app.kwela.domain.NotificationText
import app.kwela.domain.ReplyPacing
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles Reply and Mark as read from the notification. */
class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notifications = ReplyNotifications(context)
        when (intent.action) {
            ReplyNotifications.ACTION_MARK_READ -> notifications.cancel()
            ReplyNotifications.ACTION_REPLY -> {
                val text = NotificationText.cleanReply(RemoteInput.getResultsFromIntent(intent)?.getCharSequence(ReplyNotifications.KEY_REPLY))
                if (text == null) { notifications.cancel(); return }
                val pending = goAsync()
                val app = context.applicationContext as KwelaApplication
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        app.repository.saveMessage(ChatMessageEntity(UUID.randomUUID().toString(), "USER", text, System.currentTimeMillis()))
                        ReplyScheduler.enqueue(context, ReplyPacing.isSlow(text, hasAttachment = false))
                        notifications.show(emptyList(), youSaid = text)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
