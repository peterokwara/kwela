package app.kwela.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.kwela.R
import app.kwela.data.notify.ReplyNotifications
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReplyNotificationsChannelTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Test
    fun repliesArriveWithKwelasOwnSoundAndAHighPriorityChannel() {
        // An older install had a silent channel under the old id. It must be replaced, not kept.
        manager.createNotificationChannel(NotificationChannel("kwela_replies", "Replies", NotificationManager.IMPORTANCE_HIGH))

        ReplyNotifications(context).ensureChannel()

        assertNull("the old channel is gone", manager.getNotificationChannel("kwela_replies"))
        val channel = manager.getNotificationChannel(ReplyNotifications.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertTrue("has Kwela's sound", channel.sound.toString().endsWith("/${R.raw.kwela_message}"))
        assertTrue(channel.shouldVibrate())
    }

    @Test
    fun settingUpTheChannelTwiceChangesNothing() {
        val notifications = ReplyNotifications(context)
        notifications.ensureChannel()
        notifications.ensureChannel()

        assertEquals(1, manager.notificationChannels.count { it.id == ReplyNotifications.CHANNEL_ID })
    }
}
