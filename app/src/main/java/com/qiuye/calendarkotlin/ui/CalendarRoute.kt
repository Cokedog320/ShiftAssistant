package com.qiuye.calendarkotlin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qiuye.calendarkotlin.domain.CalendarCalculator
import com.qiuye.calendarkotlin.viewmodel.CalendarUiState
import com.qiuye.calendarkotlin.viewmodel.CalendarViewModel
import java.time.YearMonth
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qiuye.calendarkotlin.tasks.ui.viewmodel.TasksViewModel
import kotlinx.coroutines.launch
import com.qiuye.calendarkotlin.diary.ui.DiaryViewModel
import com.qiuye.calendarkotlin.diary.ui.DiaryListBottomSheet

private val pagerStartMonth: YearMonth = YearMonth.of(1, 1)
private val pagerPageCount = 12 * 9999

@Composable
fun CalendarRoute(
    viewModel: CalendarViewModel, 
    tasksViewModel: TasksViewModel = viewModel(),
    diaryViewModel: DiaryViewModel = viewModel(),
    onNavigateToEditTask: (Long) -> Unit,
    onNavigateToNewTaskWithDate: (Long) -> Unit,
    onNavigateToNewTask: () -> Unit,
    onNavigateToDiaryEdit: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reminders by tasksViewModel.reminders.collectAsStateWithLifecycle()
    val diaryDateKeys by diaryViewModel.diaryDateKeys.collectAsStateWithLifecycle()
    val diaryEntries by diaryViewModel.allEntries.collectAsStateWithLifecycle()
    val diarySearchQuery by diaryViewModel.searchQuery.collectAsStateWithLifecycle()
    val diarySearchResults by diaryViewModel.searchResults.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    CalendarScreen(
        uiState = uiState,
        reminders = reminders,
        diaryDateKeys = diaryDateKeys,
        diaryEntries = diaryEntries,
        diarySearchQuery = diarySearchQuery,
        diarySearchResults = diarySearchResults,
        onMonthChanged = viewModel::setCurrentMonth,
        onToday = viewModel::showToday,
        onOpenSettings = viewModel::openSettings,
        onCloseSettings = viewModel::closeSettings,
        onOpenNotes = viewModel::openNotes,
        onCloseNotes = viewModel::closeNotes,
        onOpenRemindersCenter = viewModel::openReminders,
        onCloseRemindersCenter = viewModel::closeReminders,
        onOpenDiaryList = viewModel::openDiaryList,
        onCloseDiaryList = viewModel::closeDiaryList,
        onOpenDiaryEdit = {
            val selected = uiState.selectedDate
            if (selected != null) {
                onNavigateToDiaryEdit(selected.toString())
            }
        },
        onSearchDiary = diaryViewModel::setSearchQuery,
        onSaveDiary = { date, content, mood -> diaryViewModel.saveDiary(date.toString(), content, mood) },
        onDeleteDiary = { date -> diaryViewModel.deleteDiary(date.toString()) },
        onSelectDate = { date ->
            viewModel.selectDate(date)
            // Also auto-open if the date has reminders
            val hasReminders = reminders.any { reminder ->
                java.time.Instant.ofEpochMilli(reminder.scheduledAtMillis)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate() == date
            }
            if (hasReminders) {
                viewModel.openSelectedDayDetail()
            }
        },
        onOpenSelectedDayDetail = viewModel::openSelectedDayDetail,
        onCloseDaySheet = viewModel::closeDaySheet,
        onSaveDayDetail = viewModel::saveDayDetail,
        onSaveSettings = viewModel::saveSettings,
        onClearOverrides = viewModel::clearOverrides,
        onJumpToDate = viewModel::jumpToDate,
        onToggleTask = { reminder ->
            coroutineScope.launch {
                tasksViewModel.toggleCompletion(reminder.id, !reminder.isCompleted)
            }
        },
        onDeleteTask = { reminder ->
            coroutineScope.launch {
                tasksViewModel.deleteReminder(reminder.id)
            }
        },
        onNavigateToEditTask = { reminder ->
            onNavigateToEditTask(reminder.id)
        },
        onNavigateToNewTaskWithDate = { date ->
            val dateMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            onNavigateToNewTaskWithDate(dateMillis)
        },
        onAddNewReminder = onNavigateToNewTask,
        onNavigateToDiaryEdit = onNavigateToDiaryEdit
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarScreen(
    uiState: CalendarUiState,
    reminders: List<com.qiuye.calendarkotlin.tasks.data.ReminderEntity>,
    diaryDateKeys: Set<String>,
    diaryEntries: List<com.qiuye.calendarkotlin.diary.data.DiaryEntity>,
    diarySearchQuery: String,
    diarySearchResults: List<com.qiuye.calendarkotlin.diary.data.DiaryEntity>,
    onMonthChanged: (YearMonth) -> Unit,
    onToday: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onOpenNotes: () -> Unit,
    onCloseNotes: () -> Unit,
    onOpenRemindersCenter: () -> Unit,
    onCloseRemindersCenter: () -> Unit,
    onOpenDiaryList: () -> Unit,
    onCloseDiaryList: () -> Unit,
    onOpenDiaryEdit: () -> Unit,
    onSearchDiary: (String) -> Unit,
    onSaveDiary: (java.time.LocalDate, String, String) -> Unit,
    onDeleteDiary: (java.time.LocalDate) -> Unit,
    onSelectDate: (java.time.LocalDate) -> Unit,
    onOpenSelectedDayDetail: () -> Unit,
    onCloseDaySheet: () -> Unit,
    onSaveDayDetail: (java.time.LocalDate, String, com.qiuye.calendarkotlin.model.ShiftDefinition?) -> Unit,
    onSaveSettings: (String?, String?, List<com.qiuye.calendarkotlin.model.ShiftDefinition>, Boolean) -> Unit,
    onClearOverrides: () -> Unit,
    onJumpToDate: (java.time.LocalDate) -> Unit,
    onToggleTask: (com.qiuye.calendarkotlin.tasks.data.ReminderEntity) -> Unit,
    onDeleteTask: (com.qiuye.calendarkotlin.tasks.data.ReminderEntity) -> Unit,
    onNavigateToEditTask: (com.qiuye.calendarkotlin.tasks.data.ReminderEntity) -> Unit,
    onNavigateToNewTaskWithDate: (java.time.LocalDate) -> Unit,
    onAddNewReminder: () -> Unit,
    onNavigateToDiaryEdit: (String) -> Unit
) {
    val palette = seasonPaletteFor(uiState.currentMonth.monthValue)
    val coroutineScope = rememberCoroutineScope()
    val latestOnMonthChanged = rememberUpdatedState(onMonthChanged)
    val latestCurrentMonth = rememberUpdatedState(uiState.currentMonth)
    val pagerState = rememberPagerState(
        initialPage = monthToPage(uiState.currentMonth),
        pageCount = { pagerPageCount },
    )
    val handleToday = {
        onToday()
    }

    LaunchedEffect(uiState.currentMonth) {
        val targetPage = monthToPage(uiState.currentMonth)
        if (pagerState.currentPage == targetPage) return@LaunchedEffect

        val distance = kotlin.math.abs(pagerState.currentPage - targetPage)
        if (distance <= 3) {
            pagerState.animateScrollToPage(targetPage)
        } else {
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .map(::pageToMonth)
            .distinctUntilChanged()
            .collect { month ->
                if (month != latestCurrentMonth.value) {
                    latestOnMonthChanged.value(month)
                }
            }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "日历",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "${palette.name} · ${uiState.currentMonth}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.accent,
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onOpenRemindersCenter,
                        modifier = Modifier.testTag("btn_reminders"),
                    ) {
                        Text("任务提醒")
                    }
                    TextButton(
                        onClick = onOpenNotes,
                        modifier = Modifier.testTag("btn_notes"),
                    ) {
                        Text("备忘录")
                    }
                    TextButton(
                        onClick = onOpenDiaryList,
                        modifier = Modifier.testTag("btn_diary"),
                    ) {
                        Text("日记")
                    }
                    DateJumpButton(onDatePicked = onJumpToDate)
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("btn_settings"),
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = "设置")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(palette.background))
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                MonthHeader(
                    month = uiState.currentMonth,
                    accentColor = palette.accent,
                    onPreviousMonth = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                        }
                    },
                    onNextMonth = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                (pagerState.currentPage + 1).coerceAtMost(pagerPageCount - 1),
                            )
                        }
                    },
                    onToday = handleToday,
                    onOpenSelectedDayDetail = onOpenSelectedDayDetail,
                    isDayDetailEnabled = uiState.selectedDate != null,
                )
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calendar_pager"),
                ) { page ->
                    val month = remember(page) { pageToMonth(page) }
                    val reminderDates = remember(reminders) {
                        reminders.map { reminder ->
                            java.time.Instant.ofEpochMilli(reminder.scheduledAtMillis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        }.toSet()
                    }
                    val dayCells = remember(
                        month,
                        uiState.calendarData,
                        uiState.selectedDate,
                        reminderDates,
                        diaryDateKeys,
                    ) {
                        CalendarCalculator.buildMonthGrid(
                            month = month,
                            calendarData = uiState.calendarData,
                            selectedDate = uiState.selectedDate,
                            reminderDates = reminderDates,
                            diaryDates = diaryDateKeys,
                        )
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        CalendarGrid(
                            dayCells = dayCells,
                            seasonAccent = palette.accent,
                            onSelectDate = onSelectDate,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (uiState.isSettingsVisible) {
                SettingsBottomSheet(
                    calendarData = uiState.calendarData,
                    onDismiss = onCloseSettings,
                    onClearOverrides = onClearOverrides,
                    onSave = onSaveSettings,
                )
            } else if (uiState.isNotesVisible) {
                NotesBottomSheet(
                    noteEntries = uiState.noteEntries,
                    showLunar = uiState.calendarData.showLunar,
                    reminders = reminders,
                    onDismiss = onCloseNotes,
                    onSelectNoteDate = { date ->
                        onCloseNotes()
                        onJumpToDate(date)
                    },
                )
            } else if (uiState.isRemindersVisible) {
                RemindersBottomSheet(
                    reminders = reminders,
                    onDismiss = onCloseRemindersCenter,
                    onToggleReminder = onToggleTask,
                    onDeleteReminder = onDeleteTask,
                    onOpenReminder = { reminder ->
                        onCloseRemindersCenter()
                        onNavigateToEditTask(reminder)
                    },
                    onJumpToDate = { date ->
                        onCloseRemindersCenter()
                        onJumpToDate(date)
                    },
                    onAddReminder = {
                        onCloseRemindersCenter()
                        onAddNewReminder()
                    }
                )
            } else if (uiState.isDaySheetVisible) {
                uiState.selectedDate?.let { selected ->
                    val selectedCell = remember(selected, uiState.calendarData) {
                        CalendarCalculator.buildMonthGrid(
                            month = YearMonth.from(selected),
                            calendarData = uiState.calendarData,
                            selectedDate = selected,
                        ).firstOrNull { it.date == selected }
                    }
                    val dateReminders = remember(selected, reminders) {
                        reminders.filter { reminder ->
                            val reminderDate = java.time.Instant.ofEpochMilli(reminder.scheduledAtMillis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                            reminderDate == selected
                        }
                    }
                    DayDetailBottomSheet(
                        date = selected,
                        currentShift = CalendarCalculator.getShiftForDate(selected, uiState.calendarData),
                        note = uiState.calendarData.notes[selected.toString()].orEmpty(),
                        pattern = uiState.calendarData.pattern,
                        overrideShift = uiState.calendarData.overrides[selected.toString()],
                        lunarFullText = selectedCell?.lunarFullText.orEmpty(),
                        holidayName = selectedCell?.holiday?.name,
                        holidayLabel = selectedCell?.holiday?.label,
                        tasks = dateReminders,
                        onDismiss = onCloseDaySheet,
                        onSave = { note, shift -> onSaveDayDetail(selected, note, shift) },
                        onToggleTask = onToggleTask,
                        onDeleteTask = onDeleteTask,
                        onOpenReminder = { reminder ->
                            onCloseDaySheet()
                            onNavigateToEditTask(reminder)
                        },
                        onAddFullReminder = {
                            onCloseDaySheet()
                            onNavigateToNewTaskWithDate(selected)
                        }
                    )
                }
            } else if (uiState.isDiaryListVisible) {
                DiaryListBottomSheet(
                    entries = diaryEntries,
                    searchQuery = diarySearchQuery,
                    searchResults = diarySearchResults,
                    onSearchQueryChanged = onSearchDiary,
                    onDismiss = onCloseDiaryList,
                    onWriteDiaryClick = {
                        onCloseDiaryList()
                        onNavigateToDiaryEdit(java.time.LocalDate.now().toString())
                    },
                    onSelectDate = { date ->
                        onCloseDiaryList()
                        onNavigateToDiaryEdit(date.toString())
                    }
                )
            }
        }
    }
}

private fun monthToPage(month: YearMonth): Int {
    val monthIndex = month.year * 12 + (month.monthValue - 1)
    val startIndex = pagerStartMonth.year * 12 + (pagerStartMonth.monthValue - 1)
    return monthIndex - startIndex
}

private fun pageToMonth(page: Int): YearMonth = pagerStartMonth.plusMonths(page.toLong())


