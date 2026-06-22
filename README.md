# Calendar | 智能排班备忘日历

[English Version](#english-version) | [中文版](#中文版)

---

## 中文版

一个功能完善的本地 Android 排班日历、日期备注与定时提醒应用。

本项目旨在提供一个真实可用、结构清晰、逻辑严密的本地时间与日程管理工具，优先保证本地可用、提醒可靠、数据稳定。它集成了轮班管理、每日备忘、提醒闹铃等核心日常功能。

### 最近更新 (🌟 Recent Updates)

- 🌐 **英文界面日期格式优化与六行布局适配**：
  - 顶栏月份由全称改为缩写（如 "September 2026" → "Sep 2026"），消除长月份名对右侧上/下月箭头的挤压。
  - 副标题月份标签保留全称（"September 2026"），兼顾视觉美观与紧凑性。
  - 详情页由数字格式改为美式短格式 + 中点分隔星期（如 "2026-09-21 Sunday" → "Sep 21, 2026 · Sunday"）。
  - 当月历恰好呈六行时，英文胶囊模式下自动收窄日期与胶囊间距（10dp → 4dp），避免底部提示点被挤压；五行及以下布局完全不变。
- 🚀 **冷启动 ANR 风险修复**：
  - 移除 `MainActivity.onCreate` 中的 `runBlocking` 同步阻塞读取，改用 `collectAsState` 以系统当前 locale 作初值异步加载，消除低端机 ANR 隐患。
  - 在 `LaunchedEffect` 内加入相等判断，避免重复调用 `AppCompatDelegate.setApplicationLocales`。
- 📌 **备忘录中心交互重构与置顶头部**：
  - **置顶头部标题栏**：固定了“备忘录中心”的标题与关闭按钮，无论列表如何滚动，用户随时可以方便地点击关闭或划走面板。
  - **卡片直观勾选与删除**：保留了经典的复选框选中机制。用户在勾选某条备注左侧的复选框后，卡片右侧会动态显示删除（垃圾桶）按钮，点击可直接进行二次确认删除，无需像以前一样繁琐地滑回屏幕最顶端点击删除。
- ⚡ **提醒保存瞬时返回（消除阻塞）**：
  - 移除了原有的同步等待 Compose Snackbar 消失（约 4 秒）与人为设定的延迟。
  - 引入了非阻塞的 Android 系统原生 `Toast` 提示反馈（在保存成功且存在权限异常时，自动调整为长提示）。
  - 实现点击“保存”后 **0 毫秒延迟** 即刻返回日历主界面，而提示信息仍会完美悬浮在主屏幕下方，提供了极佳的响应速度与交互连贯性。
- 🧪 **完整单元测试与集成测试套件**：
  - 构建了涵盖 Domain、Tasks、Data、ViewModel 和 UI 层的全面测试套件，测试总数已达到 60+。
  - 包括 Room 数据库多版本升迁移测试、DataStore Preferences 本地文件持久化验证、ViewModel 状态流响应式 Turbine 断言，以及利用 Jetpack Compose Test 框架对底部栏、列表和搜索流进行的端到端 UI 验证。

### 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose, Material Design 3
- **导航**: AndroidX Navigation
- **持久化**: Room Database (SQLite), DataStore Preferences
- **调度器**: AlarmManager (系统精确定时器)
- **构建工具**: Gradle Kotlin DSL

### 主要代码结构

主要代码位于 `app/src/main/java/com/qiuye/calendarkotlin/` 下：
- `ui/`：日历面板、设置页、备忘录中心等 Compose 界面组件
- `viewmodel/`：日历主状态、响应式交互流
- `data/`：日期备注、班次配置等 DataStore Preferences 数据处理
- `domain/`：日期网格计算、农历换算与节假日匹配算法
- `tasks/`：包含闹钟定时调度（AlarmManager）、系统状态栏通知（NotificationCompat）以及提醒的交互设计

### 测试与验证

项目包含完善的单元测试（在 JVM 运行）与仪器化 UI 测试（在真机或模拟器上运行）。

**运行单元测试（JVM）**:
```powershell
.\gradlew.bat testDebugUnitTest
```
**编译仪器化测试 APK**:
```powershell
.\gradlew.bat assembleDebugAndroidTest
```
**在连接的设备上运行仪器化测试**:
```powershell
.\gradlew.bat connectedDebugAndroidTest
```

---

## English Version

A fully-featured local Android Calendar application designed for shift rotation management, daily note taking, and scheduled reminder alerts.

This repository serves as a reliable, clean, and highly robust daily utility tool that prioritizes off-line usability, punctual alarm scheduling, and local data persistence.

### 🌟 Recent Updates

- 🌐 **English Locale Date Format & Six-Row Layout Adjustments**:
  - Top bar month uses short abbreviation (e.g., "September 2026" → "Sep 2026") to prevent long month names from compressing the prev/next arrow buttons.
  - Subtitle month label retains full name ("September 2026") for visual aesthetics.
  - Detail page switches from numeric format to American short format with a middot separator (e.g., "2026-09-21 Sunday" → "Sep 21, 2026 · Sunday").
  - When the month grid renders exactly six rows, the English capsule layout automatically tightens the spacing between date number and capsule (10dp → 4dp) so status dots at the bottom are not squeezed; five-row and shorter layouts remain unchanged.
- 🚀 **Cold-Start ANR Risk Fix**:
  - Removed the `runBlocking` synchronous blocking read from `MainActivity.onCreate`, switching to `collectAsState` with the current system locale as the initial value for asynchronous loading, eliminating ANR risk on low-end devices.
  - Added an equality check inside `LaunchedEffect` to avoid redundant `AppCompatDelegate.setApplicationLocales` calls.
- 📌 **Notes Center Layout & Deletion Interaction Refactoring**:
  - **Sticky Top Bar**: Extracted the "Notes Center" title and close buttons outside the scrollable `LazyColumn`. They are now pinned to the top of the BottomSheet, allowing users to close or dismiss the panel easily at any scroll offset.
  - **Localized Checked Deletion**: Kept the classic checkbox selection flow. Checking a note card's left-side selector now dynamically reveals a red delete (trash) button on the card itself (to the left of the forward arrow). Users can confirm deletion on-the-spot without scrolling all the way back to the top of the sheet.
- ⚡ **Instant Return on Save (Zero-Delay Navigation)**:
  - Eliminated the previous coroutine suspension block where the app had to wait for the Compose Snackbar to dismiss (~4 seconds) before navigating back.
  - Replaced it with Android's system-level `Toast` (which automatically uses `LENGTH_LONG` for permission warnings and `LENGTH_SHORT` for normal saves).
  - The edit screen now navigates back **with 0ms delay** as soon as the user taps "Save", while the Toast message floats smoothly on top of the destination screen.
- 🧪 **Comprehensive Test Suite**:
  - Created a robust testing infrastructure covering Domain, Tasks, Data, ViewModels, and UI layers with 60+ verified test cases.
  - Features multi-version Room migration testing, local DataStore Preferences serialization validation, Turbine-based `StateFlow` assertions, and end-to-end Compose UI test cases to guarantee zero regressions.

### Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose, Material Design 3
- **Navigation**: AndroidX Navigation
- **Persistence**: Room Database (SQLite), DataStore Preferences
- **Scheduling**: AlarmManager (System Exact Timer)
- **Build Tool**: Gradle Kotlin DSL

### Project Directory Structure

Core source files reside under `app/src/main/java/com/qiuye/calendarkotlin/`:
- `ui/`: Compose layout screens for main view, calendar details, settings, and notes bottom sheet.
- `viewmodel/`: Core state logic and user event mapping.
- `data/`: Local cache managers and DataStore Preference configuration files.
- `domain/`: Math calculators for monthly day grids, Lunar conversion tables, and statutory holiday rules.
- `tasks/`: Scheduled reminders package covering alarm setup, Notification builders, and receiver boot restorations.

### Running Tests

**Run local unit tests (JVM)**:
```powershell
.\gradlew.bat testDebugUnitTest
```
**Assemble android instrumented tests APK**:
```powershell
.\gradlew.bat assembleDebugAndroidTest
```
**Run instrumented tests on connected device**:
```powershell
.\gradlew.bat connectedDebugAndroidTest
```
