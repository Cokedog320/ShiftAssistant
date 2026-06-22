package com.qiuye.calendarkotlin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qiuye.calendarkotlin.domain.ChineseCalendarInfo
import com.qiuye.calendarkotlin.ui.CalendarRoute
import com.qiuye.calendarkotlin.ui.theme.CalendarKotlinTheme
import com.qiuye.calendarkotlin.ui.theme.LanguageMode
import com.qiuye.calendarkotlin.ui.theme.LanguagePreferences
import com.qiuye.calendarkotlin.ui.theme.ThemeMode
import com.qiuye.calendarkotlin.ui.theme.ThemePreferences
import com.qiuye.calendarkotlin.ui.theme.resolve
import com.qiuye.calendarkotlin.ui.theme.toLanguageMode
import kotlinx.coroutines.launch
import com.qiuye.calendarkotlin.viewmodel.CalendarViewModel
import com.qiuye.calendarkotlin.tasks.TasksGraph
import com.qiuye.calendarkotlin.tasks.notification.ReminderNotifier
import com.qiuye.calendarkotlin.tasks.scheduler.ExactAlarmPermission
import com.qiuye.calendarkotlin.tasks.ui.navigation.ReminderRoutes
import com.qiuye.calendarkotlin.tasks.ui.screen.EditTaskScreen
import com.qiuye.calendarkotlin.tasks.ui.viewmodel.TasksViewModel
import com.qiuye.calendarkotlin.diary.ui.DiaryViewModel
import com.qiuye.calendarkotlin.diary.ui.DiaryEditScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

public class MainActivity : AppCompatActivity() {
    private val openReminderIdFlow = MutableStateFlow<Long?>(null)
    private val themePreferences by lazy { ThemePreferences(applicationContext) }
    private val languagePreferences by lazy { LanguagePreferences(applicationContext) }

    private val calendarViewModel by viewModels<CalendarViewModel> {
        CalendarViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChineseCalendarInfo.init(applicationContext)
        TasksGraph.initialize(applicationContext)
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )

        handleOpenReminderIntent(intent)

        setContent {
            val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val languageMode by languagePreferences.languageMode.collectAsState(
                initial = AppCompatDelegate.getApplicationLocales().toLanguageMode()
            )
            val isDark = themeMode.resolve(isSystemInDarkTheme())

            LaunchedEffect(languageMode) {
                val currentMode = AppCompatDelegate.getApplicationLocales().toLanguageMode()
                if (currentMode != languageMode) {
                    AppCompatDelegate.setApplicationLocales(languageMode.resolve())
                }
            }

            CalendarKotlinTheme(darkTheme = isDark) {
                MainNavigation(
                    calendarViewModel = calendarViewModel,
                    openReminderIdFlow = openReminderIdFlow.asStateFlow(),
                    onConsumeOpenReminder = { openReminderIdFlow.value = null },
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        lifecycleScope.launch {
                            themePreferences.setThemeMode(mode)
                        }
                    },
                    languageMode = languageMode,
                    onLanguageModeChange = { mode ->
                        lifecycleScope.launch {
                            languagePreferences.setLanguageMode(mode)
                        }
                    },
                    isDark = isDark,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenReminderIntent(intent)
    }

    private fun handleOpenReminderIntent(intent: Intent?) {
        val reminderId = intent?.getLongExtra(ReminderNotifier.EXTRA_OPEN_REMINDER_ID, -1L)
        if (reminderId != null && reminderId > 0L) {
            openReminderIdFlow.value = reminderId
        }
    }
}

@Composable
private fun MainNavigation(
    calendarViewModel: CalendarViewModel,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    languageMode: LanguageMode,
    onLanguageModeChange: (LanguageMode) -> Unit,
    isDark: Boolean,
    tasksViewModel: TasksViewModel = viewModel(factory = TasksViewModel.factory(LocalContext.current)),
    diaryViewModel: DiaryViewModel = viewModel(factory = DiaryViewModel.factory(LocalContext.current)),
    openReminderIdFlow: kotlinx.coroutines.flow.StateFlow<Long?>,
    onConsumeOpenReminder: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasNotificationPermission by remember {
        mutableStateOf(ReminderNotifier.hasNotificationPermission(context))
    }
    var hasExactAlarmPermission by remember {
        mutableStateOf(tasksViewModel.canScheduleExactAlarms())
    }
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }
    
    val openReminderId by openReminderIdFlow.collectAsState()

    LaunchedEffect(Unit) {
        hasNotificationPermission = ReminderNotifier.hasNotificationPermission(context)
        hasExactAlarmPermission = tasksViewModel.canScheduleExactAlarms()
    }

    LaunchedEffect(openReminderId) {
        val reminderId = openReminderId ?: return@LaunchedEffect
        // Navigate to edit screen directly when notification is clicked
        navController.navigate(ReminderRoutes.edit(reminderId)) {
            launchSingleTop = true
        }
        onConsumeOpenReminder()
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = ReminderNotifier.hasNotificationPermission(context)
                hasExactAlarmPermission = tasksViewModel.canScheduleExactAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    NavHost(
        navController = navController,
        startDestination = ReminderRoutes.CALENDAR
    ) {
        composable(ReminderRoutes.CALENDAR) {
            CalendarRoute(
                viewModel = calendarViewModel,
                tasksViewModel = tasksViewModel,
                diaryViewModel = diaryViewModel,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                languageMode = languageMode,
                onLanguageModeChange = onLanguageModeChange,
                isDark = isDark,
                onNavigateToEditTask = { reminderId ->
                    navController.navigate(ReminderRoutes.edit(reminderId))
                },
                onNavigateToNewTaskWithDate = { dateMillis ->
                    navController.navigate(ReminderRoutes.newWithDate(dateMillis))
                },
                onNavigateToNewTask = {
                    navController.navigate(ReminderRoutes.EDIT_NEW)
                },
                onNavigateToDiaryEdit = { dateKey ->
                    navController.navigate(ReminderRoutes.diaryEdit(dateKey))
                }
            )
        }

        composable(
            route = ReminderRoutes.EDIT_NEW,
            arguments = listOf(navArgument(ReminderRoutes.DATE_ARG) { 
                type = NavType.LongType
                defaultValue = -1L 
            })
        ) { backStackEntry ->
            val dateMillis = backStackEntry.arguments?.getLong(ReminderRoutes.DATE_ARG) ?: -1L
            EditTaskScreen(
                reminderId = null,
                initialDateMillis = if (dateMillis > 0L) dateMillis else null,
                isDark = isDark,
                hasNotificationPermission = hasNotificationPermission,
                hasExactAlarmPermission = hasExactAlarmPermission,
                onRequestNotificationPermission = {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                },
                onRequestExactAlarmPermission = {
                    ExactAlarmPermission.openSettings(context)
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSave = { reminderId, input, dateStartMillis, minutesOfDay, allowPast ->
                    tasksViewModel.saveReminder(
                        reminderId = reminderId,
                        input = input,
                        dateStartMillis = dateStartMillis,
                        minutesOfDay = minutesOfDay,
                        allowPast = allowPast
                    )
                },
                onDelete = null,
                onLoadReminder = { null }
            )
        }

        composable(
            route = ReminderRoutes.EDIT,
            arguments = listOf(navArgument(ReminderRoutes.REMINDER_ID_ARG) { type = NavType.LongType })
        ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getLong(ReminderRoutes.REMINDER_ID_ARG)
            EditTaskScreen(
                reminderId = reminderId,
                isDark = isDark,
                hasNotificationPermission = hasNotificationPermission,
                hasExactAlarmPermission = hasExactAlarmPermission,
                onRequestNotificationPermission = {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                },
                onRequestExactAlarmPermission = {
                    ExactAlarmPermission.openSettings(context)
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSave = { editedReminderId, input, dateStartMillis, minutesOfDay, allowPast ->
                    tasksViewModel.saveReminder(
                        reminderId = editedReminderId,
                        input = input,
                        dateStartMillis = dateStartMillis,
                        minutesOfDay = minutesOfDay,
                        allowPast = allowPast
                    )
                },
                onDelete = { editedReminderId ->
                    tasksViewModel.deleteReminder(editedReminderId)
                },
                onLoadReminder = { id ->
                    tasksViewModel.loadReminder(id)
                }
            )
        }

        composable(
            route = ReminderRoutes.DIARY_EDIT,
            arguments = listOf(navArgument(ReminderRoutes.DIARY_EDIT_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val dateKey = backStackEntry.arguments?.getString(ReminderRoutes.DIARY_EDIT_ARG) ?: ""
            DiaryEditScreen(
                dateKey = dateKey,
                viewModel = diaryViewModel,
                isDark = isDark,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
