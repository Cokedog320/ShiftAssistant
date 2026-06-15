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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.decodeFromJsonElement

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
    val isProfileSelectVisible: Boolean = false,
    val errorMessage: String? = null,
)

private data class SheetVisibility(
    val settings: Boolean,
    val notes: Boolean,
    val daySheet: Boolean,
    val reminders: Boolean,
    val diaryList: Boolean,
    val profileSelect: Boolean,
    val error: String?,
)

private data class SecondSheetVisibility(
    val reminders: Boolean,
    val diaryList: Boolean,
    val profileSelect: Boolean,
    val error: String?,
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
    private val reminderService: com.qiuye.calendarkotlin.tasks.service.ReminderService? = null,
) : ViewModel() {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val currentMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val settingsVisible = MutableStateFlow(false)
    private val notesVisible = MutableStateFlow(false)
    private val daySheetVisible = MutableStateFlow(false)
    private val remindersVisible = MutableStateFlow(false)
    private val diaryListVisible = MutableStateFlow(false)
    private val profileSelectVisible = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val initialCalendarDataWithNotes = CalendarData().toCalendarDataWithNotes()
    private val calendarDataWithNotes: StateFlow<CalendarDataWithNotes> =
        repository.calendarData
            .map { data -> data.toCalendarDataWithNotes() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = initialCalendarDataWithNotes,
            )

    val uiState: StateFlow<CalendarUiState> =
        combine(
            calendarDataWithNotes,
            currentMonth,
            selectedDate,
            combine(
                combine(settingsVisible, notesVisible, daySheetVisible) { s, n, d -> Triple(s, n, d) },
                combine(remindersVisible, diaryListVisible, profileSelectVisible, errorMessage) { r, dl, ps, e ->
                    SecondSheetVisibility(r, dl, ps, e)
                }
            ) { a, b ->
                SheetVisibility(a.first, a.second, a.third, b.reminders, b.diaryList, b.profileSelect, b.error)
            }
        ) { dataWithNotes, month, selected, visibility ->
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
                isProfileSelectVisible = visibility.profileSelect,
                errorMessage = visibility.error,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
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
                isProfileSelectVisible = profileSelectVisible.value,
                errorMessage = errorMessage.value,
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
        val noteKey = date.toStorageKey()
        val currentNotes = calendarDataWithNotes.value.calendarData.notes
        if (!currentNotes[noteKey].isNullOrBlank()) {
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

    suspend fun exportData(): String {
        val data = repository.getCurrentData()
        return json.encodeToString(data)
    }

    fun importData(jsonString: String) {
        viewModelScope.launch {
            runCatching {
                val element = json.parseToJsonElement(jsonString)
                val jsonObject = element.jsonObject
                val hasExpectedKeys = jsonObject.containsKey("pattern") ||
                        jsonObject.containsKey("notes") ||
                        jsonObject.containsKey("overrides") ||
                        jsonObject.containsKey("cycleStartDate") ||
                        jsonObject.containsKey("cycleEndDate") ||
                        jsonObject.containsKey("showLunar") ||
                        jsonObject.containsKey("profiles")
                if (!hasExpectedKeys) {
                    throw IllegalArgumentException("Not a calendar backup file")
                }

                if (jsonObject.containsKey("profiles")) {
                    json.decodeFromString<CalendarData>(jsonString)
                } else {
                    val cycleStartDate = jsonObject["cycleStartDate"]?.let { json.decodeFromJsonElement<String?>(it) }
                    val cycleEndDate = jsonObject["cycleEndDate"]?.let { json.decodeFromJsonElement<String?>(it) }
                    val pattern = jsonObject["pattern"]?.let { json.decodeFromJsonElement<List<ShiftDefinition>>(it) } ?: defaultPattern
                    val notes = jsonObject["notes"]?.let { json.decodeFromJsonElement<Map<String, String>>(it) } ?: emptyMap()
                    val overrides = jsonObject["overrides"]?.let { json.decodeFromJsonElement<Map<String, ShiftDefinition>>(it) } ?: emptyMap()
                    val showLunar = jsonObject["showLunar"]?.let { json.decodeFromJsonElement<Boolean>(it) } ?: true

                    val importedProfile = com.qiuye.calendarkotlin.model.ShiftProfile(
                        id = java.util.UUID.randomUUID().toString(),
                        name = "导入的方案",
                        cycleStartDate = cycleStartDate,
                        cycleEndDate = cycleEndDate,
                        pattern = pattern,
                        overrides = overrides,
                        notes = emptyMap()
                    )

                    val current = repository.getCurrentData()
                    val oldActiveId = current.activeProfileId
                    val mergedNotes = current.notes.toMutableMap().apply {
                        putAll(notes)
                    }
                    val updated = current.copy(
                        profiles = current.profiles + importedProfile,
                        activeProfileId = importedProfile.id,
                        showLunar = showLunar,
                        notes = mergedNotes
                    )

                    reminderService?.rescheduleAlarmsForProfileSwitch(oldActiveId, importedProfile.id)
                    updated
                }
            }.onSuccess { data ->
                repository.replaceAllData(data)
                errorMessage.value = null
            }.onFailure {
                errorMessage.value = "导入失败：文件格式不正确或已损坏"
            }
        }
    }

    fun clearErrorMessage() {
        errorMessage.value = null
    }

    fun switchProfile(newProfileId: String) {
        viewModelScope.launch {
            val current = repository.getCurrentData()
            val oldId = current.activeProfileId
            if (oldId == newProfileId) return@launch
            val updated = current.copy(activeProfileId = newProfileId)
            repository.replaceAllData(updated)

            reminderService?.rescheduleAlarmsForProfileSwitch(oldId, newProfileId)
        }
    }

    fun addNewProfile(name: String) {
        viewModelScope.launch {
            val current = repository.getCurrentData()
            val newId = java.util.UUID.randomUUID().toString()
            val newProfile = com.qiuye.calendarkotlin.model.ShiftProfile(
                id = newId,
                name = name.trim().ifBlank { "新排班方案" },
                pattern = emptyList() // start empty as requested
            )
            val updatedProfiles = current.profiles + newProfile
            val updated = current.copy(
                activeProfileId = newId,
                profiles = updatedProfiles
            )
            repository.replaceAllData(updated)

            reminderService?.rescheduleAlarmsForProfileSwitch(current.activeProfileId, newId)

            profileSelectVisible.value = false
            settingsVisible.value = true
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            val current = repository.getCurrentData()
            if (current.profiles.size <= 1) return@launch
            val activeId = current.activeProfileId
            val updatedProfiles = current.profiles.filter { it.id != profileId }
            val newActiveId = if (activeId == profileId) {
                updatedProfiles.first().id
            } else {
                activeId
            }
            val updated = current.copy(
                activeProfileId = newActiveId,
                profiles = updatedProfiles
            )
            repository.replaceAllData(updated)

            if (activeId != newActiveId) {
                reminderService?.rescheduleAlarmsForProfileSwitch(activeId, newActiveId)
            }
        }
    }

    fun saveDayDetail(date: LocalDate, note: String, overrideShift: ShiftDefinition?, durationDays: Int = 1) {
        viewModelScope.launch {
            val current = repository.getCurrentData()
            val updatedNotes = current.notes.toMutableMap()
            val updatedOverrides = current.overrides.toMutableMap()

            val dateKey = date.toStorageKey()
            if (note.isBlank()) {
                updatedNotes.remove(dateKey)
            } else {
                updatedNotes[dateKey] = note.trim()
            }

            for (i in 0 until durationDays) {
                val targetDateKey = date.plusDays(i.toLong()).toStorageKey()
                if (overrideShift == null) {
                    updatedOverrides.remove(targetDateKey)
                } else {
                    updatedOverrides[targetDateKey] = overrideShift
                }
            }

            val updatedProfiles = current.profiles.map { profile ->
                if (profile.id == current.activeProfileId) {
                    profile.copy(
                        overrides = updatedOverrides
                    )
                } else {
                    profile
                }
            }
            repository.replaceAllData(
                current.copy(
                    profiles = updatedProfiles,
                    notes = updatedNotes
                )
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
        profileName: String,
        cycleStartDate: String?,
        cycleEndDate: String?,
        pattern: List<ShiftDefinition>,
        showLunar: Boolean,
    ) {
        val normalizedCycleStartDate = cycleStartDate?.takeIf { it.isNotBlank() }
        val normalizedCycleEndDate = cycleEndDate?.takeIf { it.isNotBlank() }
        val normalizedPattern = pattern.ifEmpty { defaultPattern }
        viewModelScope.launch {
            val current = repository.getCurrentData()
            val updatedProfiles = current.profiles.map { profile ->
                if (profile.id == current.activeProfileId) {
                    profile.copy(
                        name = profileName.trim().ifBlank { profile.name },
                        cycleStartDate = normalizedCycleStartDate,
                        cycleEndDate = normalizedCycleEndDate,
                        pattern = normalizedPattern
                    )
                } else {
                    profile
                }
            }
            repository.replaceAllData(
                current.copy(
                    profiles = updatedProfiles,
                    showLunar = showLunar
                )
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
            settingsVisible.value = false
        }
    }

    private fun dismissDaySheet(clearSelection: Boolean = false) {
        daySheetVisible.value = false
        if (clearSelection) {
            selectedDate.value = null
        }
    }

    fun openProfileSelect() {
        closeAllSheets()
        dismissDaySheet(clearSelection = true)
        profileSelectVisible.value = true
    }

    fun closeProfileSelect() {
        profileSelectVisible.value = false
    }

    private fun closeAllSheets() {
        settingsVisible.value = false
        notesVisible.value = false
        remindersVisible.value = false
        diaryListVisible.value = false
        profileSelectVisible.value = false
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
                val appContext = context.applicationContext
                CalendarViewModel(
                    repository = CalendarRepository(appContext),
                    reminderService = com.qiuye.calendarkotlin.tasks.TasksGraph.reminderService(appContext),
                )
            }
        }
    }
}
