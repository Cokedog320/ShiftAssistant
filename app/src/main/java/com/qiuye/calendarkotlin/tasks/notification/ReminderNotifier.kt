package com.qiuye.calendarkotlin.tasks.notification

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.qiuye.calendarkotlin.MainActivity
import com.qiuye.calendarkotlin.R
import com.qiuye.calendarkotlin.tasks.data.ReminderEntity
import com.qiuye.calendarkotlin.tasks.data.formatDateTime

object ReminderNotifier {
    const val CHANNEL_ID = "reminder_alerts"
    const val CHANNEL_NAME = "Reminders"
    const val CHANNEL_DESCRIPTION = "Reminder notifications"
    const val EXTRA_OPEN_REMINDER_ID = "extra_open_reminder_id"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channelName = context.getString(R.string.channel_name_reminders)
        val channelDescription = context.getString(R.string.channel_description_reminders)
        val channel = NotificationChannel(
            CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = channelDescription
        }
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun notifyReminder(context: Context, reminder: ReminderEntity): Boolean {
        if (!hasNotificationPermission(context)) {
            return false
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_REMINDER_ID, reminder.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = buildString {
            append(formatDateTime(reminder.scheduledAtMillis))
            if (reminder.note.isNotBlank()) {
                append(" · ")
                append(reminder.note)
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        return runCatching {
            notifyInternal(context, reminder.id, notification)
            true
        }.getOrDefault(false)
    }

    fun cancelReminderNotification(context: Context, reminderId: Long) {
        NotificationManagerCompat.from(context).cancel(reminderId.toInt())
    }

    @SuppressLint("MissingPermission")
    private fun notifyInternal(context: Context, reminderId: Long, notification: android.app.Notification) {
        NotificationManagerCompat.from(context).notify(reminderId.toInt(), notification)
    }
}
