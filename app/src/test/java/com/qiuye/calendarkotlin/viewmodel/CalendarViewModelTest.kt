package com.qiuye.calendarkotlin.viewmodel

import com.qiuye.calendarkotlin.data.CalendarDataStore
import com.qiuye.calendarkotlin.model.CalendarData
import com.qiuye.calendarkotlin.model.ShiftDefinition
import com.qiuye.calendarkotlin.model.defaultPattern
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun calendarDataAndNoteEntriesStayInSyncWithinEachUiStateEmission() = runTest {
        val repository = FakeCalendarDataStore(
            CalendarData(
                cycleStartDate = "2026-06-01",
                pattern = defaultPattern,
                notes = mapOf("2026-06-15" to "值班交接"),
                showLunar = false,
            )
        )
        val viewModel = CalendarViewModel(repository)
        val emissions = mutableListOf<CalendarUiState>()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect(emissions::add)
        }

        try {
            advanceUntilIdle()

            repository.updateDetail(
                dateKey = "2026-06-20",
                note = "复盘",
                overrideShift = null,
            )
            advanceUntilIdle()

            assertTrue(emissions.isNotEmpty())
            emissions.forEach(::assertCalendarDataMatchesNoteEntries)

            val latestState = emissions.last()
            assertEquals("复盘", latestState.calendarData.notes["2026-06-20"])
            assertTrue(
                latestState.noteEntries.any { entry ->
                    entry.date == LocalDate.of(2026, 6, 20) && entry.text == "复盘"
                }
            )
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun uiStateValueUpdatesWithActiveCollector() = runTest {
        val repository = FakeCalendarDataStore(
            CalendarData(
                cycleStartDate = "2026-06-01",
                pattern = defaultPattern,
                notes = mapOf("2026-06-15" to "值班交接"),
                showLunar = false,
            )
        )
        val viewModel = CalendarViewModel(repository)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        try {
            advanceUntilIdle()
            assertEquals("值班交接", viewModel.uiState.value.calendarData.notes["2026-06-15"])

            repository.updateDetail(
                dateKey = "2026-06-20",
                note = "复盘",
                overrideShift = null,
            )
            advanceUntilIdle()

            val latestState = viewModel.uiState.value
            assertEquals("复盘", latestState.calendarData.notes["2026-06-20"])
            assertTrue(
                latestState.noteEntries.any { entry ->
                    entry.date == LocalDate.of(2026, 6, 20) && entry.text == "复盘"
                }
            )
            assertCalendarDataMatchesNoteEntries(latestState)
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun noteEntriesStayStableWhenOnlyMonthOrSheetStateChanges() = runTest {
        val repository = FakeCalendarDataStore(
            CalendarData(
                cycleStartDate = "2026-06-01",
                pattern = defaultPattern,
                notes = mapOf(
                    "2026-06-15" to "值班交接",
                    "2026-06-20" to "复盘",
                ),
                showLunar = false,
            )
        )
        val viewModel = CalendarViewModel(repository)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        try {
            advanceUntilIdle()
            val initialEntries = viewModel.uiState.value.noteEntries

            viewModel.setCurrentMonth(YearMonth.of(2026, 7))
            advanceUntilIdle()
            val entriesAfterMonthChange = viewModel.uiState.value.noteEntries

            viewModel.openNotes()
            advanceUntilIdle()
            val entriesAfterSheetChange = viewModel.uiState.value.noteEntries

            assertEquals(2, initialEntries.size)
            assertSame(initialEntries, entriesAfterMonthChange)
            assertSame(initialEntries, entriesAfterSheetChange)
            assertTrue(viewModel.uiState.value.isNotesVisible)
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun selectDateOpensSheetOnlyIfHasNoteAndSaveClosesIt() = runTest {
        val targetDateNoNote = LocalDate.of(2026, 6, 15)
        val targetDateWithNote = LocalDate.of(2026, 6, 16)
        val repository = FakeCalendarDataStore(CalendarData(
            showLunar = false,
            notes = mapOf(targetDateWithNote.toString() to "已有备注")
        ))
        val viewModel = CalendarViewModel(repository)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        try {
            advanceUntilIdle()
            
            // 无备注日期：selectDate() 后 isDaySheetVisible == false
            viewModel.selectDate(targetDateNoNote)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isDaySheetVisible)
            assertEquals(targetDateNoNote, viewModel.uiState.value.selectedDate)

            // 有备注日期：selectDate() 后 isDaySheetVisible == true
            viewModel.selectDate(targetDateWithNote)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isDaySheetVisible)
            assertEquals(targetDateWithNote, viewModel.uiState.value.selectedDate)

            // 保存后关闭 Sheet 且清除选中状态
            viewModel.saveDayDetail(targetDateWithNote, "保存后的备注", defaultPattern.first())
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isDaySheetVisible)
            assertNull(viewModel.uiState.value.selectedDate)
            assertEquals("保存后的备注", repository.data.value.notes[targetDateWithNote.toString()])
            assertEquals(defaultPattern.first(), repository.data.value.overrides[targetDateWithNote.toString()])
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun uiStateUsesWhileSubscribedAndResumesCorrectly() = runTest {
        val repository = FakeCalendarDataStore(CalendarData(showLunar = false))
        val viewModel = CalendarViewModel(repository)

        // Collect, get initial value, then stop collecting
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()
        collector.cancel()
        advanceUntilIdle()

        // Make a change while no collector is active
        repository.updateDetail("2026-06-01", "test note", overrideShift = null)
        advanceUntilIdle()

        // Start collecting again — WhileSubscribed should replay the latest data
        val secondCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        try {
            val state = viewModel.uiState.value
            assertEquals("test note", state.calendarData.notes["2026-06-01"])
        } finally {
            secondCollector.cancel()
        }
    }
}

private fun assertCalendarDataMatchesNoteEntries(state: CalendarUiState) {
    val noteEntriesAsMap = state.noteEntries.associate { entry -> entry.date.toString() to entry.text }
    assertEquals(state.calendarData.notes, noteEntriesAsMap)
}

private class FakeCalendarDataStore(initialData: CalendarData) : CalendarDataStore {
    val data = MutableStateFlow(initialData)

    override val calendarData: Flow<CalendarData> = data

    override suspend fun updateDetail(dateKey: String, note: String, overrideShift: ShiftDefinition?) {
        val current = data.value
        val nextNotes = current.notes.toMutableMap().apply {
            if (note.isBlank()) {
                remove(dateKey)
            } else {
                put(dateKey, note.trim())
            }
        }
        val nextOverrides = current.overrides.toMutableMap().apply {
            if (overrideShift == null) {
                remove(dateKey)
            } else {
                put(dateKey, overrideShift)
            }
        }
        data.value = current.copy(notes = nextNotes, overrides = nextOverrides)
    }

    override suspend fun updateSettings(
        cycleStartDate: String?,
        cycleEndDate: String?,
        pattern: List<ShiftDefinition>,
        showLunar: Boolean,
    ) {
        data.value = data.value.copy(
            cycleStartDate = cycleStartDate,
            cycleEndDate = cycleEndDate,
            pattern = pattern,
            showLunar = showLunar,
        )
    }

    override suspend fun clearOverrides() {
        data.value = data.value.copy(overrides = emptyMap())
    }

    override suspend fun clearAll() {
        data.value = CalendarData()
    }
}


