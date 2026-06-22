package com.qiuye.calendarkotlin.tasks.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qiuye.calendarkotlin.tasks.TasksGraph
import com.qiuye.calendarkotlin.tasks.scheduler.AlarmReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import android.util.Log

class ReminderAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != AlarmReminderScheduler.ACTION_REMINDER_ALERT) return

        val reminderId = intent.getLongExtra(AlarmReminderScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                TasksGraph.reminderService(context).deliverReminder(reminderId)
            }.onFailure { e ->
                Log.e("ReminderAlertReceiver", "Failed to deliver reminder $reminderId", e)
            }
            pendingResult.finish()
        }
    }
}


