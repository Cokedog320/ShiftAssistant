package com.qiuye.calendarkotlin.model

import java.time.LocalDate
import kotlinx.serialization.Serializable

@Serializable
enum class ShiftColorOption {
    BLUE,
    INDIGO,
    GREEN,
    RED,
    ORANGE,
    GRAY,
    PURPLE,
    PINK,
}

@Serializable
data class ShiftDefinition(
    val id: String,
    val name: String,
    val color: ShiftColorOption,
)

@Serializable
data class CalendarData(
    val cycleStartDate: String? = null,
    val cycleEndDate: String? = null,
    val pattern: List<ShiftDefinition> = defaultPattern,
    val notes: Map<String, String> = emptyMap(),
    val overrides: Map<String, ShiftDefinition> = emptyMap(),
    val showLunar: Boolean = true,
)

data class DayCell(
    val date: LocalDate,
    val inCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val shift: ShiftDefinition?,
    val hasNote: Boolean,
    val hasReminder: Boolean = false,
    val hasDiary: Boolean = false,
    val lunarLabel: String = "",
    val lunarFullText: String = "",
    val holiday: HolidayMarker? = null,
)

data class HolidayMarker(
    val name: String,
    val label: String,
    val isWorkday: Boolean,
)

val defaultPattern: List<ShiftDefinition> = listOf(
    ShiftDefinition(id = "1", name = "白班", color = ShiftColorOption.BLUE),
    ShiftDefinition(id = "2", name = "夜班", color = ShiftColorOption.INDIGO),
    ShiftDefinition(id = "3", name = "休息", color = ShiftColorOption.GREEN),
    ShiftDefinition(id = "4", name = "休息", color = ShiftColorOption.GREEN),
)

val vacationShift = ShiftDefinition(
    id = "vacation",
    name = "休假",
    color = ShiftColorOption.GREEN,
)


