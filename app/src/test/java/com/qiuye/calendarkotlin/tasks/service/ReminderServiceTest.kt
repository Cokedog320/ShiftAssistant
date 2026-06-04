package com.qiuye.calendarkotlin.tasks.service

import androidx.room.Room
import com.qiuye.calendarkotlin.tasks.data.ReminderDatabase
import com.qiuye.calendarkotlin.tasks.data.ReminderEntity
import com.qiuye.calendarkotlin.tasks.data.ReminderRepository
import com.qiuye.calendarkotlin.tasks.scheduler.ReminderScheduler
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.CopyOnWriteArrayList
import com.qiuye.calendarkotlin.BaseUnitTest

class ReminderServiceTest : BaseUnitTest() {
    private lateinit var database: ReminderDatabase
    private lateinit var repository: ReminderRepository
    private lateinit var scheduler: RecordingScheduler
    private lateinit var deliveredReminders: MutableList<ReminderEntity>
    private lateinit var cancelledReminderIds: MutableList<Long>
    private lateinit var service: ReminderService

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, ReminderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ReminderRepository(database.reminderDao())
        scheduler = RecordingScheduler()
        deliveredReminders = CopyOnWriteArrayList()
        cancelledReminderIds = CopyOnWriteArrayList()
        service = ReminderService(
            repository = repository,
            scheduler = scheduler,
            context = context,
            notificationPermissionChecker = { false },
            notificationDeliverer = { _, reminder ->
                deliveredReminders += reminder
                true
            },
            notificationCanceller = { _, reminderId ->
                cancelledReminderIds += reminderId
            }
        )
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun saveReminder_newFutureReminder_returnsSuccessAndReschedules() = runBlocking {
        scheduler.canScheduleExactAlarmsResult = false
        val futureMillis = System.currentTimeMillis() + 60_000

        val result = service.saveReminder(
            reminderId = null,
            title = "Task",
            note = "note",
            scheduledAtMillis = futureMillis,
            allowPast = true
        )

        assertTrue(result is SaveReminderResult.Success)
        val success = result as SaveReminderResult.Success
        assertTrue(success.scheduledAlarm)
        assertTrue(success.needsNotificationWarning)
        assertTrue(success.needsExactAlarmWarning)
        assertEquals(1, scheduler.rescheduleCalls.size)
        assertEquals(futureMillis, scheduler.rescheduleCalls.single().scheduledAtMillis)
        assertEquals("Task", success.reminder.title)
    }

    @Test
    fun restoreSchedules_onlyRestoresFutureUncompletedReminders() = runBlocking {
        val now = System.currentTimeMillis()
        repository.insert(
            ReminderEntity(
                id = 0,
                title = "Future active",
                note = "",
                scheduledAtMillis = now + 120_000,
                isCompleted = false,
                createdAtMillis = now - 10_000,
                updatedAtMillis = now - 10_000
            )
        )
        repository.insert(
            ReminderEntity(
                id = 0,
                title = "Past active",
                note = "",
                scheduledAtMillis = now - 120_000,
                isCompleted = false,
                createdAtMillis = now - 10_000,
                updatedAtMillis = now - 10_000
            )
        )
        repository.insert(
            ReminderEntity(
                id = 0,
                title = "Future completed",
                note = "",
                scheduledAtMillis = now + 180_000,
                isCompleted = true,
                createdAtMillis = now - 10_000,
                updatedAtMillis = now - 10_000
            )
        )

        service.restoreSchedules()

        assertEquals(1, scheduler.scheduleCalls.size)
        assertEquals("Future active", scheduler.scheduleCalls.single().title)
        assertEquals(0, scheduler.cancelCalls.size)
    }

    @Test
    fun deliverReminder_onlyDeliversDueUncompletedReminder() = runBlocking {
        val now = System.currentTimeMillis()
        val futureId = repository.insert(
            ReminderEntity(
                id = 0,
                title = "Future reminder",
                note = "",
                scheduledAtMillis = now + 120_000,
                isCompleted = false,
                createdAtMillis = now - 10_000,
                updatedAtMillis = now - 10_000
            )
        )
        val completedId = repository.insert(
            ReminderEntity(
                id = 0,
                title = "Completed reminder",
                note = "",
                scheduledAtMillis = now - 60_000,
                isCompleted = true,
                createdAtMillis = now - 10_000,
                updatedAtMillis = now - 10_000
            )
        )
        val dueId = repository.insert(
            ReminderEntity(
                id = 0,
                title = "Due reminder",
                note = "",
                scheduledAtMillis = now - 1_000,
                isCompleted = false,
                createdAtMillis = now - 10_000,
                updatedAtMillis = now - 10_000
            )
        )

        service.deliverReminder(futureId)
        service.deliverReminder(completedId)
        service.deliverReminder(dueId)

        assertEquals(1, deliveredReminders.size)
        assertEquals("Due reminder", deliveredReminders.single().title)
    }

    @Test
    fun deleteReminder_removesRecord_cancelsSchedule_andCancelsNotification() = runBlocking {
        val reminderId = repository.insert(
            ReminderEntity(
                id = 0,
                title = "Delete me",
                note = "",
                scheduledAtMillis = System.currentTimeMillis() + 120_000,
                isCompleted = false,
                createdAtMillis = 1_700_000_000_000L,
                updatedAtMillis = 1_700_000_000_000L
            )
        )

        service.deleteReminder(reminderId)

        assertEquals(null, repository.getById(reminderId))
        assertTrue(scheduler.cancelCalls.contains(reminderId))
        assertTrue(cancelledReminderIds.contains(reminderId))
    }

    @Test
    fun deleteReminder_cancelsAlarmAndNotificationEvenWhenEntityNotFound() = runBlocking {
        val nonExistentId = 99999L

        service.deleteReminder(nonExistentId)

        assertTrue(scheduler.cancelCalls.contains(nonExistentId))
        assertTrue(cancelledReminderIds.contains(nonExistentId))
    }

    @Test
    fun setReminderCompleted_cancelsOrReschedulesAccordingToState() = runBlocking {
        val now = System.currentTimeMillis()
        val futureReminderId = repository.insert(
            ReminderEntity(
                id = 0,
                title = "Future reminder",
                note = "",
                scheduledAtMillis = now + 120_000,
                isCompleted = false,
                createdAtMillis = 1_700_000_000_000L,
                updatedAtMillis = 1_700_000_000_000L
            )
        )
        val completedReminderId = repository.insert(
            ReminderEntity(
                id = 0,
                title = "Completed reminder",
                note = "",
                scheduledAtMillis = now + 180_000,
                isCompleted = false,
                createdAtMillis = 1_700_000_000_000L,
                updatedAtMillis = 1_700_000_000_000L
            )
        )

        service.setReminderCompleted(futureReminderId, true)
        service.setReminderCompleted(completedReminderId, false)

        val completedUpdated = repository.getById(futureReminderId)
        val reopenedUpdated = repository.getById(completedReminderId)

        assertTrue(completedUpdated != null)
        assertTrue(completedUpdated!!.isCompleted)
        assertTrue(scheduler.cancelCalls.contains(futureReminderId))
        assertTrue(cancelledReminderIds.contains(futureReminderId))

        assertTrue(reopenedUpdated != null)
        assertFalse(reopenedUpdated!!.isCompleted)
        assertTrue(scheduler.rescheduleCalls.any { it.id == completedReminderId })
    }

    @Test
    fun saveReminder_rejectsBlankTitle() = runBlocking {
        val result = service.saveReminder(
            reminderId = null,
            title = "   ",
            note = "note",
            scheduledAtMillis = System.currentTimeMillis() + 60_000,
            allowPast = true
        )

        assertTrue(result is SaveReminderResult.ValidationError)
    }

    @Test
    fun saveReminder_requiresConfirmationForPastTime() = runBlocking {
        val result = service.saveReminder(
            reminderId = null,
            title = "Task",
            note = "note",
            scheduledAtMillis = System.currentTimeMillis() - 60_000,
            allowPast = false
        )

        assertTrue(result is SaveReminderResult.NeedsPastConfirmation)
    }

    @Test
    fun saveReminder_updatesExistingReminderWithoutChangingCreatedAtOrCompletion() = runBlocking {
        val createdAtMillis = 1_700_000_000_000L
        val existingId = repository.insert(
            ReminderEntity(
                id = 0,
                title = "Original",
                note = "old note",
                scheduledAtMillis = System.currentTimeMillis() - 120_000,
                isCompleted = true,
                createdAtMillis = createdAtMillis,
                updatedAtMillis = createdAtMillis
            )
        )

        val result = service.saveReminder(
            reminderId = existingId,
            title = "  Updated title  ",
            note = "  Updated note  ",
            scheduledAtMillis = System.currentTimeMillis() + 60_000,
            allowPast = true
        )

        assertTrue(result is SaveReminderResult.Success)
        val success = result as SaveReminderResult.Success
        assertEquals(createdAtMillis, success.reminder.createdAtMillis)
        assertTrue(success.reminder.isCompleted)
        assertEquals("Updated title", success.reminder.title)
        assertEquals("Updated note", success.reminder.note)
        assertFalse(success.scheduledAlarm)
        assertFalse(success.needsNotificationWarning)
        assertFalse(success.needsExactAlarmWarning)
        assertTrue(scheduler.cancelCalls.contains(existingId))
    }

    private class RecordingScheduler : ReminderScheduler {
        val scheduleCalls = CopyOnWriteArrayList<ReminderEntity>()
        val rescheduleCalls = CopyOnWriteArrayList<ReminderEntity>()
        val cancelCalls = CopyOnWriteArrayList<Long>()
        var canScheduleExactAlarmsResult = true

        override fun schedule(reminder: ReminderEntity) {
            scheduleCalls += reminder
        }

        override fun cancel(reminderId: Long) {
            cancelCalls += reminderId
        }

        override fun canScheduleExactAlarms(): Boolean = canScheduleExactAlarmsResult

        override fun reschedule(reminder: ReminderEntity) {
            rescheduleCalls += reminder
            cancel(reminder.id)
            schedule(reminder)
        }
    }
}

