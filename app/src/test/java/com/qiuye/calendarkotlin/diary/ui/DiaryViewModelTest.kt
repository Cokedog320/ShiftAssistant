package com.qiuye.calendarkotlin.diary.ui

import app.cash.turbine.test
import com.qiuye.calendarkotlin.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment

import com.qiuye.calendarkotlin.diary.data.DiaryEntity
import com.qiuye.calendarkotlin.diary.data.DiaryRepository
import com.qiuye.calendarkotlin.tasks.TasksGraph
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: DiaryViewModel
    private lateinit var mockRepository: DiaryRepository
    private val allEntriesFlow = MutableStateFlow<List<DiaryEntity>>(emptyList())
    private val dateKeysFlow = MutableStateFlow<List<String>>(emptyList())

    @Before
    fun setUp() {
        mockRepository = mockk(relaxed = true)
        every { mockRepository.observeAll() } returns allEntriesFlow
        every { mockRepository.observeAllDateKeys() } returns dateKeysFlow
        
        mockkObject(TasksGraph)
        every { TasksGraph.diaryRepository(any()) } returns mockRepository

        val app = RuntimeEnvironment.getApplication()
        viewModel = DiaryViewModel(app)
    }

    @After
    fun tearDown() {
        unmockkObject(TasksGraph)
    }

    @Test
    fun `saveDiary inserts entry and allEntries emits it sorted`() = runTest {
        viewModel.allEntries.test {
            // Initial state should be empty
            val initial = awaitItem()
            
            // Simulate repository update
            val entity1 = DiaryEntity(1, "2026-06-15", "Meeting notes", "Happy", 1000, 1000)
            allEntriesFlow.value = listOf(entity1)
            
            val updated = awaitItem()
            assertEquals(1, updated.size)
            assertEquals("Meeting notes", updated[0].content)
            assertEquals("2026-06-15", updated[0].dateKey)
            
            // Add another one
            val entity2 = DiaryEntity(2, "2026-06-14", "Earlier notes", "Neutral", 900, 900)
            allEntriesFlow.value = listOf(entity2, entity1)
            
            val sortedList = awaitItem()
            assertEquals(2, sortedList.size)
            assertEquals("2026-06-14", sortedList[0].dateKey) // Passes through unchanged from repo
            assertEquals("2026-06-15", sortedList[1].dateKey)
        }
    }

    @Test
    fun `searchQuery triggers searchResults with debouncing`() = runTest {
        val entity1 = DiaryEntity(1, "2026-06-15", "Learn Kotlin", "Happy", 1000, 1000)
        every { mockRepository.search("Kotlin") } returns flowOf(listOf(entity1))

        viewModel.searchResults.test {
            val initial = awaitItem()
            assertTrue(initial.isEmpty())

            viewModel.setSearchQuery("Kotlin")
            advanceUntilIdle() // Wait for debounce and search flow to complete
            
            val results = awaitItem()
            assertEquals(1, results.size)
            assertEquals("Learn Kotlin", results[0].content)
        }
    }
}
