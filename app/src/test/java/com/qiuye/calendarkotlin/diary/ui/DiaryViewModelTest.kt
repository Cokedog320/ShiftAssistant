package com.qiuye.calendarkotlin.diary.ui

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import com.qiuye.calendarkotlin.diary.data.DiaryEntity
import com.qiuye.calendarkotlin.diary.data.DiaryRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModelTest {
    private lateinit var viewModel: DiaryViewModel
    private lateinit var mockRepository: DiaryRepository
    private val allEntriesFlow = MutableStateFlow<List<DiaryEntity>>(emptyList())
    private val dateKeysFlow = MutableStateFlow<List<String>>(emptyList())

    @Before
    fun setUp() {
        mockRepository = mockk(relaxed = true)
        every { mockRepository.observeAll() } returns allEntriesFlow
        every { mockRepository.observeAllDateKeys() } returns dateKeysFlow
        viewModel = DiaryViewModel(mockRepository)
    }

    @Test
    fun `saveDiary inserts entry and allEntries emits it sorted`() = runTest {
        viewModel.allEntries.test {
            val initial = awaitItem()

            val entity1 = DiaryEntity(1, "2026-06-15", "Meeting notes", "Happy", 1000, 1000)
            allEntriesFlow.value = listOf(entity1)

            val updated = awaitItem()
            assertEquals(1, updated.size)
            assertEquals("Meeting notes", updated[0].content)
            assertEquals("2026-06-15", updated[0].dateKey)

            val entity2 = DiaryEntity(2, "2026-06-14", "Earlier notes", "Neutral", 900, 900)
            allEntriesFlow.value = listOf(entity2, entity1)

            val sortedList = awaitItem()
            assertEquals(2, sortedList.size)
            assertEquals("2026-06-14", sortedList[0].dateKey)
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
            advanceUntilIdle()

            val results = awaitItem()
            assertEquals(1, results.size)
            assertEquals("Learn Kotlin", results[0].content)
        }
    }
}