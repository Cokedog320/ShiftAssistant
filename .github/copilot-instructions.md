# Copilot instructions for Calendar (Android)

## Build, test, and lint commands

Run from repository root on Windows:

- Build debug app APK: `.\gradlew.bat :app:assembleDebug`
- Full project checks/build: `.\gradlew.bat :app:build`
- Lint (debug): `.\gradlew.bat :app:lintDebug`
- JVM unit tests: `.\gradlew.bat :app:testDebugUnitTest`
- Instrumentation tests on connected device/emulator: `.\gradlew.bat :app:connectedDebugAndroidTest`

Single-test commands:

- Single JVM test class: `.\gradlew.bat :app:testDebugUnitTest --tests "com.qiuye.calendarkotlin.viewmodel.CalendarViewModelTest"`
- Single JVM test method: `.\gradlew.bat :app:testDebugUnitTest --tests "com.qiuye.calendarkotlin.viewmodel.CalendarViewModelTest.selectDateOpensSheetOnlyIfHasNoteAndSaveClosesIt"`
- Single instrumentation test class: `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.qiuye.calendarkotlin.ui.ReminderFlowTest`
- Single instrumentation test method: `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.qiuye.calendarkotlin.ui.ReminderFlowTest#remindersSheet_displaysRemindersAndHandlesInteractions`

## High-level architecture

The app has three persistence/scheduling domains that are wired together in the UI:

1. **Calendar/profile/notes domain (DataStore + JSON)**
   - `CalendarRepository` (`data/CalendarRepository.kt`) stores calendar settings, profiles, notes, and overrides in Preferences DataStore JSON.
   - `CalendarViewModel` (`viewmodel/CalendarViewModel.kt`) is the state coordinator for month selection, sheet visibility, import/export, profile switching, and note editing.
   - `CalendarRoute` (`ui/CalendarRoute.kt`) binds `CalendarViewModel` state into Compose UI and bottom sheets.

2. **Reminder domain (Room + AlarmManager + notifications)**
   - Room entities/DAO/repo live under `tasks/data/`.
   - `ReminderService` (`tasks/service/ReminderService.kt`) owns reminder validation, save/delete/toggle behavior, schedule restore, and delivery rules.
   - `AlarmReminderScheduler` (`tasks/scheduler/AlarmReminderScheduler.kt`) is the AlarmManager adapter.
   - Delivery path: AlarmManager -> `ReminderAlertReceiver` -> `ReminderService.deliverReminder()` -> `ReminderNotifier`.
   - Restore path: app launch (`CalendarApplication`) and boot/permission changes (`ReminderBootReceiver`) call `ReminderService.restoreSchedules()`.

3. **Diary domain (Room)**
   - Diary entries share the same Room DB (`ReminderDatabase`) via `DiaryEntity/DiaryDao/DiaryRepository`.
   - `DiaryViewModel` exposes all entries, date-key index, and debounced search.

Cross-domain wiring is done through **`TasksGraph`** (manual service locator singleton), which provides lazy singletons for `CalendarRepository`, Room database/repositories, scheduler, and reminder service. `MainActivity` initializes `TasksGraph` and creates ViewModels via explicit factories.

## Key repository-specific conventions

- **Date key format is canonicalized as `yyyy-MM-dd`** for notes/diary/overrides (`toStorageKey`, `parseStorageDateOrNull`). Manual input parsing additionally accepts `yyyyMM` and `yyyyMMdd`.
- **Reminder data is profile-scoped** (`ReminderEntity.profileId`, DAO queries by profile), and profile switches/imports must reschedule alarms via `ReminderService.rescheduleAlarmsForProfileSwitch`.
- **Bottom-sheet visibility is mutually exclusive** in `CalendarViewModel`: call `closeAllSheets()` before opening another center/sheet; day sheet is managed separately with `dismissDaySheet(...)`.
- **`CalendarData` compatibility model is intentional**: main model is profile-based, but secondary constructor/properties (`cycleStartDate`, `pattern`, `overrides`) are retained for backward compatibility and tests.
- **Reminder save contract uses `SaveReminderResult`** (success / past-time confirmation / validation error). Callers should branch on result type instead of inferring from side effects.
- **Room migration coverage is expected** when schema changes: update migrations in `ReminderDatabase`, keep `exportSchema = true`, and keep migration tests passing (`tasks/data/RoomMigrationTest.kt`, androidTest migration coverage).
- **Compose UI tests depend on stable `testTag`/content descriptions** (e.g., `btn_reminders`, `calendar_pager`, button descriptions like “添加提醒”). Avoid breaking these without updating tests.

## MCP servers to configure for this repository

- **GitHub MCP**: use for PRs/issues/reviews/check-runs and release workflow visibility tied to this Android app.
- **Android/ADB MCP (if available in your Copilot environment)**: use for emulator/device install, launching activities, logcat capture, and instrumentation-test execution.
- **Gradle/Build MCP (if available)**: use for structured task execution and build/test diagnostics (`assembleDebug`, `testDebugUnitTest`, `connectedDebugAndroidTest`, `lintDebug`).
