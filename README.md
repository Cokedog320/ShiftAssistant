# ShiftAssistant | 智能排班备忘日历

[English Version](#english-version) | [中文版](#中文版)

---

## 中文版

一个功能完善的本地 Android 排班日历、日期备注、日记与定时提醒应用。

本项目旨在提供一个真实可用、结构清晰、逻辑严密的本地时间与日程管理工具，优先保证本地可用、提醒可靠、数据稳定。它集成了轮班管理、每日备忘、心情日记、提醒闹铃等核心日常功能，完全离线运行，数据仅保存在本机。

### 核心功能

- **📅 排班日历**：自定义轮班周期与班次配色，支持多套排班方案（Profile）切换，一目了然地查看每日班次。
- **🈺 农历与节假日**：内置农历换算、节气与法定节假日（休 / 班）标记，中文界面下自动显示。
- **📝 日期备忘**：为任意日期添加备注，备忘录中心支持按年月分组浏览与快速删除。
- **📖 心情日记**：记录每日心情与点滴，支持按日期查看、搜索与管理。
- **⏰ 定时提醒**：基于系统精确闹钟（AlarmManager）的可靠提醒，支持开机 / 权限变更后自动恢复，并新增提醒归档与自动归档能力。
- **🌗 主题与语言**：支持浅色 / 深色 / 跟随系统主题，以及中英文双语切换。

### 最近更新 (🌟 Recent Updates)

#### v1.1
- 📅 **补全 2027 年法定节假日与调休安排**：
  - 依据国务院办公厅 2027 年部分节假日安排通知，内置 2027 年元旦、春节、清明节、劳动节、端午节（6 月 9 日单休不调休）、中秋节（9 月 15 日单休不调休）及国庆节假期与全部 5 个调休上班日（`休` / `班`）标记。
  - 同步更新英文模式（`holidays_en.json`）下 2027 年西方主要节日日期（New Year's Day、Valentine's Day、Mother's Day、Father's Day、Halloween、Thanksgiving、Christmas Eve、Christmas Day）。
  - 补充真实假日资源文件（`holidays.json` / `holidays_en.json`）的解析与调休标记单元测试，全套 137 项 JVM 单元测试通过；支持从 v1.0 无缝覆盖升级并完整保留本地数据。

#### v1.0
- 🚀 **冷启动零闪烁 / 零空白（SplashScreen 就绪门）**：
  - 新增统一的启动屏“就绪门”，在主题、语言、排班数据、节假日、提醒、日记六类数据首次真实加载完成前保持启动屏（并带 800ms 超时兜底，绝不卡死）。
  - 彻底消除了“浅色模式下先闪一下深色再切回”以及“主页面先空白、稍后才显示排班规律”的问题，冷启动第一帧即为完整、正确的界面。
  - 农历 / 节假日 JSON 解析移出主线程，配合 `holidaysReady` 状态流在数据就绪后自动重组，避免首帧卡顿。
- 📖 **日记保存交互优化**：
  - 底部大号“保存日记”按钮改为右上角紧凑图标按钮；查看已有日记时与“删除”并排。
  - 输入完成后保存对号变为实心圆底 + 白色对号，启用状态一眼可见；书写区域随之显著增大。
- ⏰ **提醒归档功能**：
  - 新增提醒归档 / 取消归档，以及基于保留期的自动归档清扫（Room 数据库迁移至 v5，新增 `isArchived` / `archivedAtMillis` 字段）。
- 🧪 **测试与稳定性**：
  - 修复并补充单元 / 仪器测试，全套 JVM 单元测试通过；覆盖 Room 多版本迁移、DataStore 持久化、ViewModel 状态流与 Compose UI 端到端验证。

### 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose, Material Design 3
- **导航**: AndroidX Navigation
- **启动屏**: AndroidX Core SplashScreen
- **持久化**: Room Database (SQLite), DataStore Preferences
- **调度器**: AlarmManager (系统精确定时器)
- **构建工具**: Gradle Kotlin DSL

### 主要代码结构

主要代码位于 `app/src/main/java/com/qiuye/calendarkotlin/` 下：
- `ui/`：日历面板、设置页、备忘录中心等 Compose 界面组件
- `viewmodel/`：日历主状态、响应式交互流
- `data/`：日期备注、班次配置等 DataStore Preferences 数据处理
- `domain/`：日期网格计算、农历换算与节假日匹配算法
- `diary/`：心情日记的界面、ViewModel 与 Room 数据层
- `tasks/`：闹钟定时调度（AlarmManager）、系统通知（NotificationCompat）、提醒归档以及提醒交互设计

### 构建与打包

**编译 Debug 包**:
```powershell
.\gradlew.bat :app:assembleDebug
```
**编译正式签名 Release 包**（需在项目根目录配置 `keystore.properties`）:
```powershell
.\gradlew.bat :app:assembleRelease
```
> 签名配置从项目根目录的 `keystore.properties` 读取（`storeFile` / `storePassword` / `keyAlias` / `keyPassword`），该文件与密钥库 `.jks` 均不应提交到版本库。

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

A fully-featured local Android app for shift rotation management, daily notes, mood journaling, and scheduled reminder alerts.

This repository serves as a reliable, clean, and highly robust daily utility that prioritizes off-line usability, punctual alarm scheduling, and local data persistence. It runs entirely offline, with all data stored only on the device.

### Core Features

- **📅 Shift Calendar**: Customize rotation cycles and shift colors, switch between multiple shift profiles, and see each day's shift at a glance.
- **🈺 Lunar Calendar & Holidays**: Built-in lunar conversion, solar terms, and statutory holiday (Off / Work) markers, shown automatically in the Chinese locale.
- **📝 Date Notes**: Attach notes to any date; the Notes Center supports year/month grouping and quick deletion.
- **📖 Mood Journal**: Record your daily mood and thoughts, with view-by-date, search, and management.
- **⏰ Scheduled Reminders**: Reliable reminders backed by exact system alarms (AlarmManager), with automatic restore after boot / permission changes, plus new reminder archiving and auto-archiving.
- **🌗 Theme & Language**: Light / Dark / Follow-System themes and Chinese ⇄ English switching.

### 🌟 Recent Updates

#### v1.1
- 📅 **2027 Statutory Holidays & Makeup Workdays**:
  - Added all 2027 Chinese statutory holidays and 5 makeup workdays (`holidays.json`), including New Year's Day, Spring Festival, Qingming Festival, Labor Day, Dragon Boat Festival (June 9, single day off with no makeup workday), Mid-Autumn Festival (September 15, single day off with no makeup workday), and National Day.
  - Updated `holidays_en.json` with 2027 Western holidays for the English locale (New Year's Day, Valentine's Day, Mother's Day, Father's Day, Halloween, Thanksgiving, Christmas Eve, and Christmas Day).
  - Added asset-backed unit tests verifying 2027 holiday and makeup workday markers (all 137 JVM unit tests pass); supports seamless in-place upgrade over v1.0 while preserving all local data.

#### v1.0
- 🚀 **Flicker-Free, Blank-Free Cold Start (SplashScreen Ready-Gate)**:
  - Added a unified splash "ready-gate" that keeps the splash screen up until theme, language, shift/calendar data, holidays, reminders, and diary data have all had their first real load (with an 800ms fallback so it never hangs).
  - Completely eliminates the "dark flashes first then switches to light" issue and the "empty home screen, shift pattern appears a moment later" delay — the very first frame on cold start is complete and correct.
  - Lunar/holiday JSON parsing moved off the main thread, with a `holidaysReady` state flow that recomposes once data is ready, avoiding first-frame jank.
- 📖 **Diary Save Interaction Refinement**:
  - The large bottom "Save Diary" button is replaced with a compact top-right icon button; when viewing an existing entry it sits next to "Delete".
  - Once text is entered the save check becomes a filled accent circle with a white check for clear affordance, and the writing area grows noticeably larger.
- ⏰ **Reminder Archiving**:
  - Added archive / unarchive and a retention-based auto-archive sweep (Room database migrated to v5 with new `isArchived` / `archivedAtMillis` columns).
- 🧪 **Testing & Stability**:
  - Fixed and expanded unit / instrumentation tests; the full JVM unit test suite passes, covering multi-version Room migrations, DataStore persistence, ViewModel state flows, and end-to-end Compose UI verification.

### Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose, Material Design 3
- **Navigation**: AndroidX Navigation
- **Splash**: AndroidX Core SplashScreen
- **Persistence**: Room Database (SQLite), DataStore Preferences
- **Scheduling**: AlarmManager (System Exact Timer)
- **Build Tool**: Gradle Kotlin DSL

### Project Directory Structure

Core source files reside under `app/src/main/java/com/qiuye/calendarkotlin/`:
- `ui/`: Compose layout screens for main view, calendar details, settings, and notes bottom sheet.
- `viewmodel/`: Core state logic and user event mapping.
- `data/`: Local cache managers and DataStore Preference configuration files.
- `domain/`: Math calculators for monthly day grids, Lunar conversion tables, and statutory holiday rules.
- `diary/`: Mood journal UI, ViewModel, and Room data layer.
- `tasks/`: Scheduled reminders package covering alarm setup, Notification builders, reminder archiving, and receiver boot restorations.

### Building & Packaging

**Build a debug APK**:
```powershell
.\gradlew.bat :app:assembleDebug
```
**Build a signed release APK** (requires `keystore.properties` in the project root):
```powershell
.\gradlew.bat :app:assembleRelease
```
> Signing config is read from `keystore.properties` in the project root (`storeFile` / `storePassword` / `keyAlias` / `keyPassword`). Neither this file nor the `.jks` keystore should be committed to version control.

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
