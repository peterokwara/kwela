package app.kwela.data.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.media.AudioAttributes
import android.net.Uri
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import app.kwela.MainActivity
import app.kwela.R
import app.kwela.domain.NotificationText

/** The chat-app style notification: a round "K", the sender's name, the real message, and Reply and Mark as read right inside it. */
class ReplyNotifications(private val context: Context) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        // A channel's sound cannot be changed once it exists, so the first version, which had no sound of its own, is replaced.
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        val sound = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.kwela_message}")
        val channel = NotificationChannel(CHANNEL_ID, "Replies", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Kwela's replies when you are away from the app."
            setSound(sound, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 120, 80, 120)
        }
        manager.createNotificationChannel(channel)
    }

    /** False when the person, or Android, has switched Kwela's notifications off. */
    fun canPost(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * Shows Kwela's latest lines. [youSaid] is what the person just typed in the notification, so it can show that it was received.
     * A new reply makes a sound, every time. [alert] is false only for the quiet "got your reply" update.
     */
    fun show(lines: List<String>, youSaid: String? = null, alert: Boolean = true) {
        ensureChannel()
        val me = Person.Builder().setName("You").build()
        val kwela = Person.Builder().setName("Kwela").setIcon(avatar()).setImportant(true).build()
        val style = NotificationCompat.MessagingStyle(me)
        val now = System.currentTimeMillis()
        youSaid?.let { style.addMessage(it, now, me) }
        lines.forEachIndexed { index, line -> style.addMessage(line, now + index + 1, kwela) }

        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val remoteInput = RemoteInput.Builder(KEY_REPLY).setLabel("Reply").build()
        val reply = NotificationCompat.Action.Builder(R.drawable.ic_notification, "Reply", receiverIntent(ACTION_REPLY, mutable = true))
            .addRemoteInput(remoteInput).setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY).setShowsUserInterface(false).setAllowGeneratedReplies(true).build()
        val markRead = NotificationCompat.Action.Builder(R.drawable.ic_notification, "Mark as read", receiverIntent(ACTION_MARK_READ, mutable = false))
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ).setShowsUserInterface(false).build()
        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.ic_notification).setContentTitle("Kwela").setContentText(NotificationText.PUBLIC_TEXT).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(!alert)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .addAction(reply)
            .addAction(markRead)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    fun cancel() = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)

    private fun receiverIntent(action: String, mutable: Boolean): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (mutable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, action.hashCode(), Intent(context, ReplyReceiver::class.java).setAction(action), flags)
    }

    /** The round "K" that stands in for a profile picture. */
    private fun avatar(): IconCompat {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = android.graphics.Color.rgb(0x2A, 0x40, 0x73)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = size * 0.5f
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("K", size / 2f, size / 2f + paint.textSize * 0.35f, paint)
        return IconCompat.createWithBitmap(bitmap)
    }

    companion object {
        const val CHANNEL_ID = "kwela_replies_v2"
        private const val LEGACY_CHANNEL_ID = "kwela_replies"
        const val NOTIFICATION_ID = 2001
        const val KEY_REPLY = "reply_text"
        const val ACTION_REPLY = "app.kwela.action.REPLY"
        const val ACTION_MARK_READ = "app.kwela.action.MARK_READ"
    }
}
