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

import com.qiuye.calendarkotlin.data.CalendarDataStore
import com.qiuye.calendarkotlin.model.CalendarData
import com.qiuye.calendarkotlin.model.ShiftDefinition

class ReminderServiceTest : BaseUnitTest() {
    private lateinit var database: ReminderDatabase
    private lateinit var repository: ReminderRepository
    private lateinit var scheduler: RecordingScheduler
    private lateinit var deliveredReminders: MutableList<ReminderEntity>
    private lateinit var cancelledReminderIds: MutableList<Long>
    private lateinit var service: ReminderService

    private val fakeCalendarRepository = object : CalendarDataStore {
        override val calendarData = kotlinx.coroutines.flow.MutableStateFlow(CalendarData())
        override suspend fun getCurrentData() = CalendarData()
        override suspend fun updateDetail(dateKey: String, note: String, overrideShift: ShiftDefinition?) {}
        override suspend fun updateSettings(
            cycleStartDate: String?,
            cycleEndDate: String?,
            pattern: List<ShiftDefinition>,
            showLunar: Boolean
        ) {}
        override suspend fun clearOverrides() {}
        override suspend fun clearAll() {}
        override suspend fun replaceAllData(data: CalendarData) {}
    }

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
            calendarRepository = fakeCalendarRepository,
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
            input = "Task\nnote",
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
            input = "   \nnote",
            scheduledAtMillis = System.currentTimeMillis() + 60_000,
            allowPast = true
        )

        assertTrue(result is SaveReminderResult.ValidationError)
    }

    @Test
    fun saveReminder_requiresConfirmationForPastTime() = runBlocking {
        val result = service.saveReminder(
            reminderId = null,
            input = "Task\nnote",
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
            input = "  Updated title  \n  Updated note  ",
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

    
    @Test
    fun saveReminder_splitsSingleLineInputToTitleAndEmptyNote() = runBlocking {
        scheduler.canScheduleExactAlarmsResult = false
        val result = service.saveReminder(
            reminderId = null,
            input = "去超市买牛奶",
            scheduledAtMillis = System.currentTimeMillis() + 60_000,
            allowPast = true
        )
        assertTrue(result is SaveReminderResult.Success)
        val success = result as SaveReminderResult.Success
        assertEquals("去超市买牛奶", success.reminder.title)
        assertEquals("", success.reminder.note)
    }

    @Test
    fun saveReminder_splitsMultiLineInputToTitleAndNote() = runBlocking {
        scheduler.canScheduleExactAlarmsResult = false
        val result = service.saveReminder(
            reminderId = null,
            input = "去超市买牛奶\n买脱脂的\n顺便带包盐",
            scheduledAtMillis = System.currentTimeMillis() + 60_000,
            allowPast = true
        )
        assertTrue(result is SaveReminderResult.Success)
        val success = result as SaveReminderResult.Success
        assertEquals("去超市买牛奶", success.reminder.title)
        assertEquals("买脱脂的\n顺便带包盐", success.reminder.note)
    }

    @Test
    fun saveReminder_handlesWhitespaceInInput() = runBlocking {
        scheduler.canScheduleExactAlarmsResult = false
        val result = service.saveReminder(
            reminderId = null,
            input = "   \n  some note  ",
            scheduledAtMillis = System.currentTimeMillis() + 60_000,
            allowPast = true
        )
        assertTrue(result is SaveReminderResult.ValidationError)
    }

    @Test
    fun runAutoArchiveSweep_archivesCompletedRemindersOlderThanRetention() = runBlocking {
        val now = System.currentTimeMillis()
        val retentionMillis = 30 * 24 * 60 * 60 * 1000L
        val oldCompletedId = repository.insert(
            ReminderEntity(
                id = 0,
                title = "Old completed",
                note = "",
                scheduledAtMillis = now - retentionMillis - 60_000,
                isCompleted = true,
                createdAtMillis = now - retentionMillis - 120_000,
                updatedAtMillis = now - retentionMillis - 60_000
            )
        )
        val recentCompletedId = repository.insert(
            ReminderEntity(
                id = 0,
                title = "Recent completed",
                note = "",
                scheduledAtMillis = now - 60_000,
                isCompleted = true,
                createdAtMillis = now - 120_000,
                updatedAtMillis = now - 60_000
            )
        )

        service.runAutoArchiveSweep("default", retentionDays = 30)

        assertTrue(repository.getById(oldCompletedId)!!.isArchived)
        assertFalse(repository.getById(recentCompletedId)!!.isArchived)
    }

    @Test
    fun runAutoArchiveSweep_archivesOverdueUncompletedRemindersOlderThanRetention() = runBlocking {
        val now = System.currentTimeMillis()
        val retentionMillis = 30 * 24 * 60 * 60 * 1000L
        val oldOverdueId = repository.insert(
            ReminderEntity(
                id = 0,
                title = "Old overdue",
                note = "",
                scheduledAtMillis = now - retentionMillis - 60_000,
                isCompleted = false,
                createdAtMillis = now - retentionMillis - 120_000,
                updatedAtMillis = now - retentionMillis - 120_000
            )
        )
        val recentOverdueId = repository.insert(
            ReminderEntity(
                id = 0,
                title = "Recent overdue",
                note = "",
                scheduledAtMillis = now - 60_000,
                isCompleted = false,
                createdAtMillis = now - 120_000,
                updatedAtMillis = now - 120_000
            )
        )

        service.runAutoArchiveSweep("default", retentionDays = 30)

        assertTrue(repository.getById(oldOverdueId)!!.isArchived)
        assertFalse(repository.getById(recentOverdueId)!!.isArchived)
    }

    @Test
    fun runAutoArchiveSweep_doesNotArchiveAlreadyArchivedItems() = runBlocking {
        val now = System.currentTimeMillis()
        val retentionMillis = 30 * 24 * 60 * 60 * 1000L
        val archivedId = repository.insert(
            ReminderEntity(
                id = 0,
                title = "Already archived",
                note = "",
                scheduledAtMillis = now - retentionMillis - 60_000,
                isCompleted = true,
                createdAtMillis = now - retentionMillis - 120_000,
                updatedAtMillis = now - retentionMillis - 60_000,
                isArchived = true,
                archivedAtMillis = now - retentionMillis
            )
        )

        service.runAutoArchiveSweep("default", retentionDays = 30)

        val archived = repository.getById(archivedId)!!
        assertTrue(archived.isArchived)
        assertEquals(now - retentionMillis, archived.archivedAtMillis)
    }

    @Test
    fun runAutoArchiveSweep_respectsProfileScope() = runBlocking {
        val now = System.currentTimeMillis()
        val retentionMillis = 30 * 24 * 60 * 60 * 1000L
        val otherProfileId = "other"
        val otherProfileOldCompletedId = repository.insert(
            ReminderEntity(
                id = 0,
                title = "Other profile old completed",
                note = "",
                scheduledAtMillis = now - retentionMillis - 60_000,
                isCompleted = true,
                createdAtMillis = now - retentionMillis - 120_000,
                updatedAtMillis = now - retentionMillis - 60_000,
                profileId = otherProfileId
            )
        )

        service.runAutoArchiveSweep("default", retentionDays = 30)

        assertFalse(repository.getById(otherProfileOldCompletedId)!!.isArchived)

        service.runAutoArchiveSweep(otherProfileId, retentionDays = 30)

        assertTrue(repository.getById(otherProfileOldCompletedId)!!.isArchived)
    }

    @Test
    fun unarchiveReminder_restoresArchivedItem() = runBlocking {
        val now = System.currentTimeMillis()
        val archivedId = repository.insert(
            ReminderEntity(
                id = 0,
                title = "Archived",
                note = "",
                scheduledAtMillis = now - 60_000,
                isCompleted = true,
                createdAtMillis = now - 120_000,
                updatedAtMillis = now - 60_000,
                isArchived = true,
                archivedAtMillis = now
            )
        )

        service.unarchiveReminder(archivedId)

        val restored = repository.getById(archivedId)!!
        assertFalse(restored.isArchived)
        assertEquals(null, restored.archivedAtMillis)
        assertTrue(restored.isCompleted)
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

