package com.qiuye.calendarkotlin.tasks.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qiuye.calendarkotlin.tasks.TasksGraph
import com.qiuye.calendarkotlin.tasks.data.ReminderEntity
import com.qiuye.calendarkotlin.tasks.data.combineDateAndMinutes
import com.qiuye.calendarkotlin.tasks.service.ReminderService
import com.qiuye.calendarkotlin.tasks.service.SaveReminderResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TasksViewModel internal constructor(
    private val service: ReminderService,
) : ViewModel() {

    val reminders = service.observeReminders()
        .map { reminders ->
            val now = System.currentTimeMillis()
            reminders.sortedWith(
                compareBy<ReminderEntity> { reminderRank(it, now) }
                    .thenBy { it.scheduledAtMillis }
                    .thenByDescending { it.updatedAtMillis }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeReminder(id: Long): Flow<ReminderEntity?> = service.observeReminder(id)

    suspend fun loadReminder(id: Long): ReminderEntity? = service.getReminder(id)

    fun canScheduleExactAlarms(): Boolean = service.canScheduleExactAlarms()

    suspend fun saveReminder(
        reminderId: Long?,
        title: String,
        note: String,
        dateStartMillis: Long,
        minutesOfDay: Int,
        allowPast: Boolean
    ): SaveReminderResult {
        val scheduledAtMillis = combineDateAndMinutes(dateStartMillis, minutesOfDay)
        return service.saveReminder(reminderId, title, note, scheduledAtMillis, allowPast)
    }

    suspend fun toggleCompletion(reminderId: Long, completed: Boolean) {
        service.setReminderCompleted(reminderId, completed)
    }

    suspend fun deleteReminder(reminderId: Long) {
        service.deleteReminder(reminderId)
    }

    suspend fun rebuildSchedules() {
        service.restoreSchedules()
    }

    private fun reminderRank(reminder: ReminderEntity, now: Long): Int {
        return when {
            reminder.isCompleted -> 2
            reminder.scheduledAtMillis <= now -> 1
            else -> 0
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TasksViewModel(
                    service = TasksGraph.reminderService(context.applicationContext),
                )
            }
        }
    }
}