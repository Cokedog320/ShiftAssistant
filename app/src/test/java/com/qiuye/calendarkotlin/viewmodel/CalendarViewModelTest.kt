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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.qiuye.calendarkotlin.model.ShiftProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
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
    fun saveDayDetailWithMultipleDayDuration() = runTest {
        val targetDate = LocalDate.of(2026, 6, 15)
        val repository = FakeCalendarDataStore(CalendarData(showLunar = false))
        val viewModel = CalendarViewModel(repository)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        try {
            val overrideShift = defaultPattern.first()
            viewModel.saveDayDetail(targetDate, "集体修假", overrideShift, durationDays = 5)
            advanceUntilIdle()

            // Note should only be on the first day
            assertEquals("集体修假", repository.data.value.notes[targetDate.toString()])
            for (i in 1 until 5) {
                assertNull(repository.data.value.notes[targetDate.plusDays(i.toLong()).toString()])
            }

            // Override shift should be applied on all 5 days
            for (i in 0 until 5) {
                assertEquals(overrideShift, repository.data.value.overrides[targetDate.plusDays(i.toLong()).toString()])
            }
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun saveDayDetailClearsMultipleDayOverrides() = runTest {
        val targetDate = LocalDate.of(2026, 6, 15)
        val initialOverrides = (0 until 5).associate { i ->
            targetDate.plusDays(i.toLong()).toString() to defaultPattern.first()
        }
        val repository = FakeCalendarDataStore(CalendarData(
            showLunar = false,
            overrides = initialOverrides
        ))
        val viewModel = CalendarViewModel(repository)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        try {
            // Save with overrideShift = null to clear overrides for all 5 days
            viewModel.saveDayDetail(targetDate, "", null, durationDays = 5)
            advanceUntilIdle()

            for (i in 0 until 5) {
                assertNull(repository.data.value.overrides[targetDate.plusDays(i.toLong()).toString()])
            }
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

    @Test
    fun exportDataReturnsCorrectJson() = runTest {
        val data = CalendarData(
            cycleStartDate = "2026-06-01",
            pattern = defaultPattern,
            notes = mapOf("2026-06-15" to "Test Note"),
            showLunar = false,
        )
        val repository = FakeCalendarDataStore(data)
        val viewModel = CalendarViewModel(repository)

        val json = viewModel.exportData()
        val decoded = Json.decodeFromString<CalendarData>(json)
        assertEquals(data, decoded)
    }

    @Test
    fun importDataUpdatesStateWithValidJson() = runTest {
        val initialData = CalendarData(showLunar = true)
        val repository = FakeCalendarDataStore(initialData)
        val viewModel = CalendarViewModel(repository)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        try {
            val newData = CalendarData(
                cycleStartDate = "2026-01-01",
                notes = mapOf("2026-01-01" to "Imported Note"),
                showLunar = false,
            )
            val json = Json.encodeToString(newData)

            viewModel.importData(json)
            advanceUntilIdle()

            assertEquals(newData.cycleStartDate, repository.data.value.cycleStartDate)
            assertEquals(false, viewModel.uiState.value.calendarData.showLunar)
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun importDataDoesNotUpdateStateWithInvalidJson() = runTest {
        val initialData = CalendarData(showLunar = true)
        val repository = FakeCalendarDataStore(initialData)
        val viewModel = CalendarViewModel(repository)

        viewModel.importData("invalid json")
        advanceUntilIdle()

        assertEquals(initialData, repository.data.value)
    }

    @Test
    fun importDataSetsErrorMessageOnFailure() = runTest {
        val initialData = CalendarData(showLunar = true)
        val repository = FakeCalendarDataStore(initialData)
        val viewModel = CalendarViewModel(repository)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        try {
            viewModel.importData("invalid json")
            advanceUntilIdle()

            assertEquals("导入失败：文件格式不正确或已损坏", viewModel.uiState.value.errorMessage)
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun importDataSetsErrorMessageOnUnrelatedJson() = runTest {
        val initialData = CalendarData(showLunar = true)
        val repository = FakeCalendarDataStore(initialData)
        val viewModel = CalendarViewModel(repository)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        try {
            val unrelatedJson = "{\"someOtherField\": 1234, \"hello\": \"world\"}"
            viewModel.importData(unrelatedJson)
            advanceUntilIdle()

            assertEquals("导入失败：文件格式不正确或已损坏", viewModel.uiState.value.errorMessage)
            assertEquals(initialData, repository.data.value)
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun importDataClearsErrorMessageOnSuccess() = runTest {
        val initialData = CalendarData(showLunar = true)
        val repository = FakeCalendarDataStore(initialData)
        val viewModel = CalendarViewModel(repository)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        try {
            viewModel.importData("invalid json")
            advanceUntilIdle()
            assertEquals("导入失败：文件格式不正确或已损坏", viewModel.uiState.value.errorMessage)

            val newData = CalendarData(showLunar = false)
            viewModel.importData(Json.encodeToString(newData))
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.errorMessage)
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun clearErrorMessageResetsMessage() = runTest {
        val initialData = CalendarData(showLunar = true)
        val repository = FakeCalendarDataStore(initialData)
        val viewModel = CalendarViewModel(repository)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        try {
            viewModel.importData("invalid json")
            advanceUntilIdle()
            assertEquals("导入失败：文件格式不正确或已损坏", viewModel.uiState.value.errorMessage)

            viewModel.clearErrorMessage()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.errorMessage)
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun profileSelectDialogVisibilityState() = runTest {
        val repository = FakeCalendarDataStore(CalendarData())
        val viewModel = CalendarViewModel(repository)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        try {
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isProfileSelectVisible)

            viewModel.openProfileSelect()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isProfileSelectVisible)

            viewModel.closeProfileSelect()
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isProfileSelectVisible)
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun clearOverridesClearsTheOverrides() = runTest {
        val targetDate = LocalDate.of(2026, 6, 15)
        val initialData = CalendarData(
            overrides = mapOf(targetDate.toString() to defaultPattern.first())
        )
        val repository = FakeCalendarDataStore(initialData)
        val viewModel = CalendarViewModel(repository)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        try {
            advanceUntilIdle()
            assertEquals(defaultPattern.first(), viewModel.uiState.value.calendarData.overrides[targetDate.toString()])
            
            viewModel.openSettings()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isSettingsVisible)

            viewModel.clearOverrides()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.calendarData.overrides.isEmpty())
            assertFalse(viewModel.uiState.value.isSettingsVisible)
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun switchProfileUpdatesActiveProfileId() = runTest {
        val profile1 = ShiftProfile(id = "p1", name = "方案1")
        val profile2 = ShiftProfile(id = "p2", name = "方案2")
        val initialData = CalendarData(activeProfileId = "p1", profiles = listOf(profile1, profile2))
        val repository = FakeCalendarDataStore(initialData)
        val viewModel = CalendarViewModel(repository)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        try {
            advanceUntilIdle()
            assertEquals("p1", viewModel.uiState.value.calendarData.activeProfileId)

            viewModel.switchProfile("p2")
            advanceUntilIdle()

            assertEquals("p2", viewModel.uiState.value.calendarData.activeProfileId)
            assertEquals("方案2", viewModel.uiState.value.calendarData.activeProfile.name)
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun addNewProfileCreatesNewProfileAndSetsAsActive() = runTest {
        val initialData = CalendarData()
        val repository = FakeCalendarDataStore(initialData)
        val viewModel = CalendarViewModel(repository)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        try {
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.calendarData.profiles.size)
            assertEquals("default", viewModel.uiState.value.calendarData.activeProfileId)

            viewModel.addNewProfile("自定义方案")
            advanceUntilIdle()

            val profiles = viewModel.uiState.value.calendarData.profiles
            assertEquals(2, profiles.size)
            val newProfile = profiles.find { it.id != "default" }
            assertNotNull(newProfile)
            assertEquals("自定义方案", newProfile!!.name)
            assertEquals(newProfile.id, viewModel.uiState.value.calendarData.activeProfileId)
            
            // Settings sheet should be opened automatically
            assertTrue(viewModel.uiState.value.isSettingsVisible)
            assertFalse(viewModel.uiState.value.isProfileSelectVisible)
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun deleteProfileDeletesProfileAndUpdatesActiveIdIfNeeded() = runTest {
        val profile1 = ShiftProfile(id = "p1", name = "方案1")
        val profile2 = ShiftProfile(id = "p2", name = "方案2")
        val initialData = CalendarData(activeProfileId = "p2", profiles = listOf(profile1, profile2))
        val repository = FakeCalendarDataStore(initialData)
        val viewModel = CalendarViewModel(repository)
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        try {
            advanceUntilIdle()
            assertEquals(2, viewModel.uiState.value.calendarData.profiles.size)
            assertEquals("p2", viewModel.uiState.value.calendarData.activeProfileId)

            // Delete active profile (p2)
            viewModel.deleteProfile("p2")
            advanceUntilIdle()

            val profiles = viewModel.uiState.value.calendarData.profiles
            assertEquals(1, profiles.size)
            assertEquals("p1", profiles.first().id)
            assertEquals("p1", viewModel.uiState.value.calendarData.activeProfileId)
        } finally {
            collector.cancel()
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
        val profiles = current.profiles.toMutableList()
        val activeIndex = profiles.indexOfFirst { it.id == current.activeProfileId }.takeIf { it != -1 } ?: 0
        val nextNotes = current.notes.toMutableMap().apply {
            if (note.isBlank()) {
                remove(dateKey)
            } else {
                put(dateKey, note.trim())
            }
        }
        if (activeIndex < profiles.size) {
            val activeProfile = profiles[activeIndex]
            val nextOverrides = activeProfile.overrides.toMutableMap().apply {
                if (overrideShift == null) {
                    remove(dateKey)
                } else {
                    put(dateKey, overrideShift)
                }
            }
            profiles[activeIndex] = activeProfile.copy(overrides = nextOverrides)
        }
        data.value = current.copy(profiles = profiles, notes = nextNotes)
    }

    override suspend fun updateSettings(
        cycleStartDate: String?,
        cycleEndDate: String?,
        pattern: List<ShiftDefinition>,
        showLunar: Boolean,
    ) {
        val current = data.value
        val profiles = current.profiles.toMutableList()
        val activeIndex = profiles.indexOfFirst { it.id == current.activeProfileId }.takeIf { it != -1 } ?: 0
        if (activeIndex < profiles.size) {
            val activeProfile = profiles[activeIndex]
            profiles[activeIndex] = activeProfile.copy(
                cycleStartDate = cycleStartDate,
                cycleEndDate = cycleEndDate,
                pattern = pattern
            )
        }
        data.value = current.copy(
            profiles = profiles,
            showLunar = showLunar
        )
    }

    override suspend fun clearOverrides() {
        val current = data.value
        val profiles = current.profiles.toMutableList()
        val activeIndex = profiles.indexOfFirst { it.id == current.activeProfileId }.takeIf { it != -1 } ?: 0
        if (activeIndex < profiles.size) {
            profiles[activeIndex] = profiles[activeIndex].copy(overrides = emptyMap())
        }
        data.value = current.copy(profiles = profiles)
    }

    override suspend fun clearAll() {
        data.value = CalendarData()
    }

    override suspend fun replaceAllData(data: CalendarData) {
        this.data.value = data
    }

    override suspend fun getCurrentData(): CalendarData {
        return data.value
    }
}


