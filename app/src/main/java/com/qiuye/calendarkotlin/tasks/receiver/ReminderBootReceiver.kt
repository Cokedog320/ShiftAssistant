package com.qiuye.calendarkotlin.tasks.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qiuye.calendarkotlin.tasks.TasksGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import android.util.Log

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                TasksGraph.reminderService(context).restoreSchedules()
            }.onFailure { e ->
                Log.e("ReminderBootReceiver", "Failed to restore schedules on boot/permission change", e)
            }
            pendingResult?.finish()
        }
    }
}


