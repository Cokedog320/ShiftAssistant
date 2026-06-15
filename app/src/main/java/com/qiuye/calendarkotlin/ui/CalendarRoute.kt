package com.qiuye.calendarkotlin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
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

private val pagerStartMonth: YearMonth = YearMonth.of(1900, 1)
private val pagerPageCount = (2100 - 1900) * 12

@Composable
fun CalendarRoute(
    viewModel: CalendarViewModel, 
    tasksViewModel: TasksViewModel = viewModel(factory = TasksViewModel.factory(LocalContext.current)),
    diaryViewModel: DiaryViewModel = viewModel(factory = DiaryViewModel.factory(LocalContext.current)),
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
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let { targetUri ->
            coroutineScope.launch {
                try {
                    val json = viewModel.exportData()
                    context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(json)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { targetUri ->
            coroutineScope.launch {
                try {
                    val json = context.contentResolver.openInputStream(targetUri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            reader.readText()
                        }
                    }
                    if (!json.isNullOrBlank()) {
                        viewModel.importData(json)
                    } else {
                        viewModel.importData("INVALID_JSON")
                    }
                } catch (e: Exception) {
                    viewModel.importData("INVALID_JSON")
                }
            }
        }
    }

    CalendarScreen(
        viewModel = viewModel,
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
        onNavigateToDiaryEdit = onNavigateToDiaryEdit,
        onDeleteNote = viewModel::deleteNote,
        onExport = { exportLauncher.launch("calendar_backup.json") },
        onImport = { importLauncher.launch(arrayOf("*/*")) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarScreen(
    viewModel: CalendarViewModel,
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
    onSaveDayDetail: (java.time.LocalDate, String, com.qiuye.calendarkotlin.model.ShiftDefinition?, Int) -> Unit,
    onSaveSettings: (String?, String?, List<com.qiuye.calendarkotlin.model.ShiftDefinition>, Boolean) -> Unit,
    onClearOverrides: () -> Unit,
    onJumpToDate: (java.time.LocalDate) -> Unit,
    onToggleTask: (com.qiuye.calendarkotlin.tasks.data.ReminderEntity) -> Unit,
    onDeleteTask: (com.qiuye.calendarkotlin.tasks.data.ReminderEntity) -> Unit,
    onNavigateToEditTask: (com.qiuye.calendarkotlin.tasks.data.ReminderEntity) -> Unit,
    onNavigateToNewTaskWithDate: (java.time.LocalDate) -> Unit,
    onAddNewReminder: () -> Unit,
    onNavigateToDiaryEdit: (String) -> Unit,
    onDeleteNote: (java.time.LocalDate) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    val palette = remember(uiState.currentMonth.monthValue) { seasonPaletteFor(uiState.currentMonth.monthValue) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val latestOnMonthChanged = rememberUpdatedState(onMonthChanged)
    val latestCurrentMonth = rememberUpdatedState(uiState.currentMonth)
    val pagerState = rememberPagerState(
        initialPage = monthToPage(uiState.currentMonth),
        pageCount = { pagerPageCount },
    )
    val reminderDates = remember(reminders) {
        reminders.map { reminder ->
            java.time.Instant.ofEpochMilli(reminder.scheduledAtMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }.toSet()
    }

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

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "倒班助手",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Row(
                            modifier = Modifier.offset(x = (-4).dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = palette.icon,
                                contentDescription = palette.name,
                                tint = palette.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = uiState.currentMonth.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accent,
                            )
                        }
                    }
                },
                actions = {
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
        bottomBar = {
            val isCalendarActive = !uiState.isRemindersVisible && !uiState.isNotesVisible && !uiState.isDiaryListVisible
            NavigationBar(
                containerColor = palette.background.last().copy(alpha = 0.95f),
            ) {
                NavigationBarItem(
                    selected = isCalendarActive,
                    onClick = {
                        if (uiState.isRemindersVisible) onCloseRemindersCenter()
                        if (uiState.isNotesVisible) onCloseNotes()
                        if (uiState.isDiaryListVisible) onCloseDiaryList()
                    },
                    icon = { Icon(Icons.Rounded.DateRange, contentDescription = "日历") },
                    label = { Text("日历") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = palette.accent,
                        selectedTextColor = palette.accent,
                        indicatorColor = palette.accent.copy(alpha = 0.15f),
                        unselectedIconColor = palette.accent.copy(alpha = 0.6f),
                        unselectedTextColor = palette.accent.copy(alpha = 0.6f),
                    )
                )
                NavigationBarItem(
                    selected = uiState.isRemindersVisible,
                    onClick = {
                        if (!uiState.isRemindersVisible) {
                            onOpenRemindersCenter()
                        }
                    },
                    icon = { Icon(Icons.Rounded.Notifications, contentDescription = "任务提醒") },
                    label = { Text("任务") },
                    modifier = Modifier.testTag("btn_reminders"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = palette.accent,
                        selectedTextColor = palette.accent,
                        indicatorColor = palette.accent.copy(alpha = 0.15f),
                        unselectedIconColor = palette.accent.copy(alpha = 0.6f),
                        unselectedTextColor = palette.accent.copy(alpha = 0.6f),
                    )
                )
                NavigationBarItem(
                    selected = uiState.isDiaryListVisible,
                    onClick = {
                        if (!uiState.isDiaryListVisible) {
                            onOpenDiaryList()
                        }
                    },
                    icon = { Icon(Icons.Rounded.EditNote, contentDescription = "日记") },
                    label = { Text("日记") },
                    modifier = Modifier.testTag("btn_diary"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = palette.accent,
                        selectedTextColor = palette.accent,
                        indicatorColor = palette.accent.copy(alpha = 0.15f),
                        unselectedIconColor = palette.accent.copy(alpha = 0.6f),
                        unselectedTextColor = palette.accent.copy(alpha = 0.6f),
                    )
                )
                NavigationBarItem(
                    selected = uiState.isNotesVisible,
                    onClick = {
                        if (!uiState.isNotesVisible) {
                            onOpenNotes()
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Rounded.List, contentDescription = "备忘录") },
                    label = { Text("备忘录") },
                    modifier = Modifier.testTag("btn_notes"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = palette.accent,
                        selectedTextColor = palette.accent,
                        indicatorColor = palette.accent.copy(alpha = 0.15f),
                        unselectedIconColor = palette.accent.copy(alpha = 0.6f),
                        unselectedTextColor = palette.accent.copy(alpha = 0.6f),
                    )
                )
            }
        }
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
                    onExport = onExport,
                    onImport = onImport,
                )
            } else if (uiState.isNotesVisible) {
                NotesBottomSheet(
                    noteEntries = uiState.noteEntries,
                    showLunar = uiState.calendarData.showLunar,
                    reminders = reminders,
                    accentColor = palette.accent,
                    onDismiss = onCloseNotes,
                    onSelectNoteDate = { date ->
                        onCloseNotes()
                        onJumpToDate(date)
                    },
                    onDeleteNote = onDeleteNote,
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
                    val selectedCell = remember(selected, uiState.calendarData, reminderDates, diaryDateKeys) {
                        CalendarCalculator.getDayCell(
                            date = selected,
                            calendarData = uiState.calendarData,
                            reminderDates = reminderDates,
                            diaryDates = diaryDateKeys,
                        )
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
                        lunarFullText = selectedCell.lunarFullText,
                        holidayName = selectedCell.holiday?.name,
                        holidayLabel = selectedCell.holiday?.label,
                        tasks = dateReminders,
                        onDismiss = onCloseDaySheet,
                        onSave = { note, shift, duration -> onSaveDayDetail(selected, note, shift, duration) },
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


