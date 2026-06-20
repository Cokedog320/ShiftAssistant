package com.qiuye.calendarkotlin.tasks

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.qiuye.calendarkotlin.BaseUnitTest
import com.qiuye.calendarkotlin.R
import com.qiuye.calendarkotlin.tasks.data.ReminderEntity
import com.qiuye.calendarkotlin.tasks.notification.ReminderNotifier
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNotificationManager

class NotificationGeneratorTest : BaseUnitTest() {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        
        // Grant notification permission for Android 13+
        val shadowApp = org.robolectric.Shadows.shadowOf(context as android.app.Application)
        shadowApp.grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)
        ReminderNotifier.createChannel(context)
    }

    @Test
    fun `createChannel should create reminder_alerts channel`() {
        val channel = shadowNotificationManager.notificationChannels.find { it.id == ReminderNotifier.CHANNEL_ID }
        assertNotNull("Channel should be created", channel)
        assertEquals(context.getString(R.string.channel_name_reminders), channel?.name)
    }

    @Test
    fun `notifyReminder should build and post notification with correct details`() {
        val reminder = ReminderEntity(
            id = 101L,
            title = "Test Meeting",
            note = "Bring laptop",
            scheduledAtMillis = System.currentTimeMillis() + 5000,
            isCompleted = false,
            createdAtMillis = 0L,
            updatedAtMillis = 0L
        )

        // Ensure permission logic passes in test by default (Robolectric gives permission by default for post notifications)
        val success = ReminderNotifier.notifyReminder(context, reminder)
        
        assertTrue(success)
        
        // Check if notification was actually posted
        val activeNotifications = shadowNotificationManager.allNotifications
        assertEquals(1, activeNotifications.size)
        
        val notification = activeNotifications[0]
        
        // Use reflection or Shadow to check contents.
        // The simplest way to test notification content is checking the ticker or extras in shadows if needed.
        // Actually Robolectric's Notification has extras containing Title and Text
        val title = notification.extras.getString(android.app.Notification.EXTRA_TITLE)
        val text = notification.extras.getString(android.app.Notification.EXTRA_TEXT)
        
        assertEquals("Test Meeting", title)
        assertTrue(text?.contains("Bring laptop") == true)
        assertEquals(ReminderNotifier.CHANNEL_ID, notification.channelId)
    }

    @Test
    fun `cancelReminderNotification should remove notification`() {
        val reminderId = 102L
        val reminder = ReminderEntity(
            id = reminderId,
            title = "To Cancel",
            note = "",
            scheduledAtMillis = System.currentTimeMillis() + 5000,
            isCompleted = false,
            createdAtMillis = 0L,
            updatedAtMillis = 0L
        )

        ReminderNotifier.notifyReminder(context, reminder)
        assertEquals(1, shadowNotificationManager.allNotifications.size)

        ReminderNotifier.cancelReminderNotification(context, reminderId)
        assertEquals(0, shadowNotificationManager.allNotifications.size)
    }
}
