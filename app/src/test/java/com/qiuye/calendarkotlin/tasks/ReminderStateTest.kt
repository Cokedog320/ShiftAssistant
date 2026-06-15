package com.qiuye.calendarkotlin.tasks

import android.content.Context
import com.qiuye.calendarkotlin.BaseUnitTest
import com.qiuye.calendarkotlin.tasks.data.ReminderEntity
import com.qiuye.calendarkotlin.tasks.data.ReminderRepository
import com.qiuye.calendarkotlin.tasks.scheduler.ReminderScheduler
import com.qiuye.calendarkotlin.tasks.service.ReminderService
import com.qiuye.calendarkotlin.utils.MockHelpers
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ReminderStateTest : BaseUnitTest() {

    @MockK
    lateinit var repository: ReminderRepository

    @MockK
    lateinit var scheduler: ReminderScheduler
    
    @MockK
    lateinit var context: Context

    private lateinit var service: ReminderService

    @Before
    fun setUp() {
        MockHelpers.initMocks(this)
        every { scheduler.canScheduleExactAlarms() } returns true
        every { context.applicationContext } returns context
        val notificationPermissionChecker: (Context) -> Boolean = { true }
        val notificationDeliverer: (Context, ReminderEntity) -> Boolean = { _, _ -> true }
        val notificationCanceller: (Context, Long) -> Unit = { _, _ -> }
        service = ReminderService(repository, scheduler, context, notificationPermissionChecker, notificationDeliverer, notificationCanceller)
    }

    @Test
    fun `setReminderCompleted should update state to completed and cancel schedule`() = runBlocking {
        // Given
        val id = 1L
        val reminder = ReminderEntity(id = id, title = "Task", note = "", scheduledAtMillis = 100L, isCompleted = false, createdAtMillis = 0L, updatedAtMillis = 0L)
        coEvery { repository.getById(id) } returns reminder
        coEvery { repository.update(any()) } returns Unit

        // When
        service.setReminderCompleted(id, true)

        // Then: State is updated to true
        coVerify { 
            repository.update(match { it.id == id && it.isCompleted }) 
        }
        // Then: Schedule is cancelled
        verify { scheduler.cancel(id) }
    }

    @Test
    fun `saveReminder should schedule if future and incomplete`() = runBlocking {
        // Given
        val futureTime = System.currentTimeMillis() + 10000
        coEvery { repository.getById(any()) } returns null
        coEvery { repository.insert(any()) } returns 2L

        // When
        val result = service.saveReminder(null, "Task", "", futureTime, false)

        // Then
        verify { scheduler.reschedule(match { it.id == 2L && it.title == "Task" }) }
        assertEquals(true, result is com.qiuye.calendarkotlin.tasks.service.SaveReminderResult.Success)
    }
    
    @Test
    fun `deleteReminder should remove from repository and cancel schedule`() = runBlocking {
        // Given
        val id = 3L
        val reminder = ReminderEntity(id = id, title = "Task", note = "", scheduledAtMillis = 100L, isCompleted = false, createdAtMillis = 0L, updatedAtMillis = 0L)
        coEvery { repository.getById(id) } returns reminder
        coEvery { repository.delete(reminder) } returns Unit
        
        // When
        service.deleteReminder(id)
        
        // Then
        coVerify { repository.delete(reminder) }
        verify { scheduler.cancel(id) }
    }
}
