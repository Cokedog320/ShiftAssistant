package com.qiuye.calendarkotlin.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qiuye.calendarkotlin.data.CalendarDataStore
import com.qiuye.calendarkotlin.data.CalendarRepository
import com.qiuye.calendarkotlin.domain.CalendarCalculator
import com.qiuye.calendarkotlin.domain.ChineseCalendarInfo
import com.qiuye.calendarkotlin.domain.parseStorageDateOrNull
import com.qiuye.calendarkotlin.domain.toStorageKey
import com.qiuye.calendarkotlin.model.CalendarData
import com.qiuye.calendarkotlin.model.ShiftDefinition
import com.qiuye.calendarkotlin.model.defaultPattern
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = null,
    val noteEntries: List<NoteEntry> = emptyList(),
    val calendarData: CalendarData = CalendarData(),
    val isSettingsVisible: Boolean = false,
    val isNotesVisible: Boolean = false,
    val isDaySheetVisible: Boolean = false,
    val isRemindersVisible: Boolean = false,
    val isDiaryListVisible: Boolean = false,
)

private data class SheetVisibility(
    val settings: Boolean,
    val notes: Boolean,
    val daySheet: Boolean,
    val reminders: Boolean,
    val diaryList: Boolean,
)

data class NoteEntry(
    val date: LocalDate,
    val text: String,
    val month: YearMonth,
    val shift: ShiftDefinition?,
    val lunarLabel: String,
)

private data class CalendarDataWithNotes(
    val calendarData: CalendarData,
    val noteEntries: List<NoteEntry>,
)

class CalendarViewModel internal constructor(
    private val repository: CalendarDataStore,
) : ViewModel() {
    private val currentMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val settingsVisible = MutableStateFlow(false)
    private val notesVisible = MutableStateFlow(false)
    private val daySheetVisible = MutableStateFlow(false)
    private val remindersVisible = MutableStateFlow(false)
    private val diaryListVisible = MutableStateFlow(false)
    private val initialCalendarDataWithNotes = CalendarData().toCalendarDataWithNotes()
    private val calendarDataWithNotes: StateFlow<CalendarDataWithNotes> =
        repository.calendarData
            .map { data -> data.toCalendarDataWithNotes() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = initialCalendarDataWithNotes,
            )

    val uiState: StateFlow<CalendarUiState> =
        combine(
            calendarDataWithNotes,
            combine(currentMonth, selectedDate, ::Pair),
            combine(
                listOf(settingsVisible, notesVisible, daySheetVisible, remindersVisible, diaryListVisible)
            ) { array ->
                SheetVisibility(
                    settings = array[0],
                    notes = array[1],
                    daySheet = array[2],
                    reminders = array[3],
                    diaryList = array[4]
                )
            }
        ) { dataWithNotes, (month, selected), visibility ->
            CalendarUiState(
                currentMonth = month,
                selectedDate = selected,
                noteEntries = dataWithNotes.noteEntries,
                calendarData = dataWithNotes.calendarData,
                isSettingsVisible = visibility.settings,
                isNotesVisible = visibility.notes,
                isDaySheetVisible = visibility.daySheet,
                isRemindersVisible = visibility.reminders,
                isDiaryListVisible = visibility.diaryList,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = CalendarUiState(
                currentMonth = currentMonth.value,
                selectedDate = selectedDate.value,
                noteEntries = initialCalendarDataWithNotes.noteEntries,
                calendarData = initialCalendarDataWithNotes.calendarData,
                isSettingsVisible = settingsVisible.value,
                isNotesVisible = notesVisible.value,
                isDaySheetVisible = daySheetVisible.value,
                isRemindersVisible = remindersVisible.value,
                isDiaryListVisible = diaryListVisible.value,
            ),
        )

    fun setCurrentMonth(month: YearMonth) {
        currentMonth.value = month
    }

    fun showToday() {
        currentMonth.value = YearMonth.now()
        selectedDate.value = LocalDate.now()
        closeAllSheets()
        dismissDaySheet()
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        currentMonth.value = YearMonth.from(date)
        closeAllSheets()
        if (hasNote(date)) {
            daySheetVisible.value = true
        }
    }

    fun openSelectedDayDetail() {
        if (selectedDate.value == null) return
        closeAllSheets()
        daySheetVisible.value = true
    }

    fun closeDaySheet() {
        dismissDaySheet()
    }

    fun openSettings() {
        closeAllSheets()
        dismissDaySheet(clearSelection = true)
        settingsVisible.value = true
    }

    fun closeSettings() {
        settingsVisible.value = false
    }

    fun openNotes() {
        closeAllSheets()
        dismissDaySheet(clearSelection = true)
        notesVisible.value = true
    }

    fun closeNotes() {
        notesVisible.value = false
    }

    fun openReminders() {
        closeAllSheets()
        dismissDaySheet(clearSelection = true)
        remindersVisible.value = true
    }

    fun closeReminders() {
        remindersVisible.value = false
    }

    fun openDiaryList() {
        closeAllSheets()
        dismissDaySheet(clearSelection = true)
        diaryListVisible.value = true
    }

    fun closeDiaryList() {
        diaryListVisible.value = false
    }



    fun jumpToDate(date: LocalDate) {
        closeAllSheets()
        currentMonth.value = YearMonth.from(date)
        selectedDate.value = date
        dismissDaySheet()
    }

    fun saveDayDetail(date: LocalDate, note: String, overrideShift: ShiftDefinition?) {
        viewModelScope.launch {
            val dateKey = date.toStorageKey()
            repository.updateDetail(
                dateKey = dateKey,
                note = note,
                overrideShift = overrideShift,
            )
            dismissDaySheet(clearSelection = true)
        }
    }

    fun deleteNote(date: LocalDate) {
        viewModelScope.launch {
            val dateKey = date.toStorageKey()
            val currentOverride = calendarDataWithNotes.value.calendarData.overrides[dateKey]
            repository.updateDetail(
                dateKey = dateKey,
                note = "",
                overrideShift = currentOverride,
            )
        }
    }

    fun saveSettings(
        cycleStartDate: String?,
        cycleEndDate: String?,
        pattern: List<ShiftDefinition>,
        showLunar: Boolean,
    ) {
        val normalizedCycleStartDate = cycleStartDate?.takeIf { it.isNotBlank() }
        val normalizedCycleEndDate = cycleEndDate?.takeIf { it.isNotBlank() }
        val normalizedPattern = pattern.ifEmpty { defaultPattern() }
        viewModelScope.launch {
            repository.updateSettings(
                cycleStartDate = normalizedCycleStartDate,
                cycleEndDate = normalizedCycleEndDate,
                pattern = normalizedPattern,
                showLunar = showLunar,
            )

            normalizedCycleStartDate
                ?.let(::parseStorageDateOrNull)
                ?.let { currentMonth.value = YearMonth.from(it) }

            dismissDaySheet(clearSelection = true)
            settingsVisible.value = false
        }
    }

    fun clearOverrides() {
        viewModelScope.launch {
            repository.clearOverrides()
        }
    }

    private fun dismissDaySheet(clearSelection: Boolean = false) {
        daySheetVisible.value = false
        if (clearSelection) {
            selectedDate.value = null
        }
    }

    private fun hasNote(date: LocalDate): Boolean =
        !calendarDataWithNotes.value.calendarData.notes[date.toStorageKey()].isNullOrBlank()

    private fun closeAllSheets() {
        settingsVisible.value = false
        notesVisible.value = false
        remindersVisible.value = false
        diaryListVisible.value = false
    }

    private fun CalendarData.toCalendarDataWithNotes(): CalendarDataWithNotes =
        CalendarDataWithNotes(
            calendarData = this,
            noteEntries = buildNoteEntries(this),
        )

    private fun buildNoteEntries(calendarData: CalendarData): List<NoteEntry> {
        return calendarData.notes.entries.mapNotNull { (dateKey, text) ->
            val date = parseStorageDateOrNull(dateKey) ?: return@mapNotNull null
            val normalizedText = text.trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
            NoteEntry(
                date = date,
                text = normalizedText,
                month = YearMonth.from(date),
                shift = CalendarCalculator.getShiftForDate(date, calendarData),
                lunarLabel = if (calendarData.showLunar) {
                    ChineseCalendarInfo.getCleanLunarLabel(date)
                } else {
                    ""
                },
            )
        }.sortedByDescending { it.date }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CalendarViewModel(
                    repository = CalendarRepository(context.applicationContext),
                )
            }
        }
    }
}


