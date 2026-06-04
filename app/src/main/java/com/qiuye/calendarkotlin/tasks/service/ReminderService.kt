package com.qiuye.calendarkotlin.tasks.service

import android.content.Context
import com.qiuye.calendarkotlin.tasks.data.ReminderEntity
import com.qiuye.calendarkotlin.tasks.data.ReminderRepository
import com.qiuye.calendarkotlin.tasks.notification.ReminderNotifier
import com.qiuye.calendarkotlin.tasks.scheduler.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import android.util.Log

sealed interface SaveReminderResult {
    data class Success(
        val reminder: ReminderEntity,
        val scheduledAlarm: Boolean,
        val needsNotificationWarning: Boolean,
        val needsExactAlarmWarning: Boolean
    ) : SaveReminderResult

    data object NeedsPastConfirmation : SaveReminderResult
    data class ValidationError(val message: String) : SaveReminderResult
}

class ReminderService(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val context: Context,
    private val notificationPermissionChecker: (Context) -> Boolean = ReminderNotifier::hasNotificationPermission,
    private val notificationDeliverer: (Context, ReminderEntity) -> Boolean = ReminderNotifier::notifyReminder,
    private val notificationCanceller: (Context, Long) -> Unit = ReminderNotifier::cancelReminderNotification
) {
    fun observeReminders(): Flow<List<ReminderEntity>> = repository.observeAll()

    fun observeReminder(id: Long): Flow<ReminderEntity?> = repository.observeById(id)

    suspend fun getReminder(id: Long): ReminderEntity? = repository.getById(id)

    fun canScheduleExactAlarms(): Boolean = scheduler.canScheduleExactAlarms()

    suspend fun saveReminder(
        reminderId: Long?,
        title: String,
        note: String,
        scheduledAtMillis: Long,
        allowPast: Boolean
    ): SaveReminderResult {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            return SaveReminderResult.ValidationError("标题不能为空")
        }

        val now = System.currentTimeMillis()
        if (scheduledAtMillis <= now && !allowPast) {
            return SaveReminderResult.NeedsPastConfirmation
        }

        val existing = reminderId?.let { repository.getById(it) }
        val createdAt = existing?.createdAtMillis ?: now
        val reminder = createReminder(
            id = existing?.id ?: 0,
            title = trimmedTitle,
            note = note.trim(),
            scheduledAtMillis = scheduledAtMillis,
            isCompleted = existing?.isCompleted ?: false,
            createdAtMillis = createdAt,
            updatedAtMillis = now
        )

        val savedReminder = if (existing == null) {
            createReminder(
                id = repository.insert(reminder),
                title = reminder.title,
                note = reminder.note,
                scheduledAtMillis = reminder.scheduledAtMillis,
                isCompleted = reminder.isCompleted,
                createdAtMillis = reminder.createdAtMillis,
                updatedAtMillis = reminder.updatedAtMillis
            )
        } else {
            repository.update(reminder)
            reminder
        }

        scheduleAccordingToState(savedReminder)
        val scheduledAlarm = savedReminder.scheduledAtMillis > now && !savedReminder.isCompleted
        val warning = !notificationPermissionChecker(context) &&
            scheduledAlarm
        val exactAlarmWarning = scheduledAlarm && !scheduler.canScheduleExactAlarms()

        return SaveReminderResult.Success(
            reminder = savedReminder,
            scheduledAlarm = scheduledAlarm,
            needsNotificationWarning = warning,
            needsExactAlarmWarning = exactAlarmWarning
        )
    }

suspend fun deleteReminder(reminderId: Long) {
        scheduler.cancel(reminderId)
        notificationCanceller(context, reminderId)
        repository.getById(reminderId)?.let { reminder ->
            repository.delete(reminder)
        }
    }

    suspend fun setReminderCompleted(reminderId: Long, completed: Boolean) {
        val reminder = repository.getById(reminderId) ?: return
        val updated = createReminder(
            id = reminder.id,
            title = reminder.title,
            note = reminder.note,
            scheduledAtMillis = reminder.scheduledAtMillis,
            isCompleted = completed,
            createdAtMillis = reminder.createdAtMillis,
            updatedAtMillis = System.currentTimeMillis()
        )
        repository.update(updated)
        if (completed) {
            scheduler.cancel(reminderId)
            notificationCanceller(context, reminderId)
        } else {
            scheduleAccordingToState(updated)
        }
    }

    suspend fun restoreSchedules() {
        val now = System.currentTimeMillis()
        repository.getFutureActiveReminders(now).forEach { reminder ->
            runCatching {
                scheduler.schedule(reminder)
            }.onFailure { e ->
                Log.e("ReminderService", "Failed to restore schedule for reminder ${reminder.id}", e)
            }
        }
    }

    suspend fun deliverReminder(reminderId: Long) {
        val reminder = repository.getById(reminderId) ?: return
        if (reminder.isCompleted || reminder.scheduledAtMillis > System.currentTimeMillis()) {
            return
        }
        notificationDeliverer(context, reminder)
    }

    private fun scheduleAccordingToState(reminder: ReminderEntity) {
        if (reminder.isCompleted || reminder.scheduledAtMillis <= System.currentTimeMillis()) {
            scheduler.cancel(reminder.id)
        } else {
            scheduler.reschedule(reminder)
        }
    }

    private fun createReminder(
        id: Long,
        title: String,
        note: String,
        scheduledAtMillis: Long,
        isCompleted: Boolean,
        createdAtMillis: Long,
        updatedAtMillis: Long
    ): ReminderEntity {
        return ReminderEntity(
            id,
            title,
            note,
            scheduledAtMillis,
            isCompleted,
            createdAtMillis,
            updatedAtMillis
        )
    }
}


