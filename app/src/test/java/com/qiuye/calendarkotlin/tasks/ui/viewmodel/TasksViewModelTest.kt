package com.qiuye.calendarkotlin.tasks.ui.viewmodel

import app.cash.turbine.test
import com.qiuye.calendarkotlin.BaseUnitTest
import com.qiuye.calendarkotlin.tasks.service.SaveReminderResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment

import com.qiuye.calendarkotlin.tasks.TasksGraph
import com.qiuye.calendarkotlin.tasks.data.ReminderEntity
import com.qiuye.calendarkotlin.tasks.service.ReminderService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: TasksViewModel
    private lateinit var mockService: ReminderService
    private val remindersFlow = MutableStateFlow<List<ReminderEntity>>(emptyList())

    @Before
    fun setUp() {
        mockService = mockk(relaxed = true)
        every { mockService.observeReminders() } returns remindersFlow
        
        mockkObject(TasksGraph)
        every { TasksGraph.reminderService(any()) } returns mockService

        val app = RuntimeEnvironment.getApplication()
        viewModel = TasksViewModel(app)
    }

    @After
    fun tearDown() {
        unmockkObject(TasksGraph)
    }

    @Test
    fun `reminders list sorts items correctly`() = runTest {
        viewModel.reminders.test {
            val initial = awaitItem()
            
            // Flow emits items
            val now = System.currentTimeMillis()
            val entityFuture = ReminderEntity(1, "Future", "", now + 3600_000, false, 0, 0)
            val entityCompleted = ReminderEntity(2, "Completed", "", now + 3600_000, true, 0, 0)
            val entityPast = ReminderEntity(3, "Past", "", now - 3600_000, false, 0, 0)
            
            remindersFlow.value = listOf(entityFuture, entityCompleted, entityPast)
            
            val sortedList = awaitItem()
            assertEquals(3, sortedList.size)
            // Sorting order: Uncompleted past <= now (1), Uncompleted future (0), Completed (2)
            // Within same rank: by scheduledAtMillis
            assertEquals("Future", sortedList[0].title) // Rank 0
            assertEquals("Past", sortedList[1].title) // Rank 1
            assertEquals("Completed", sortedList[2].title) // Rank 2
        }
    }

    @Test
    fun `reminders list updates sorting when data changes without clock`() = runTest {
        viewModel.reminders.test {
            awaitItem() // initial empty

            val now = System.currentTimeMillis()
            val upcoming = ReminderEntity(1, "Upcoming", "", now + 3600_000, false, 0, 0)
            val completed = ReminderEntity(2, "Done", "", now + 7200_000, true, 0, 0)

            remindersFlow.value = listOf(upcoming, completed)
            val list = awaitItem()

            assertEquals(2, list.size)
            assertEquals("Upcoming", list[0].title)
            assertEquals("Done", list[1].title)
        }
    }
}
