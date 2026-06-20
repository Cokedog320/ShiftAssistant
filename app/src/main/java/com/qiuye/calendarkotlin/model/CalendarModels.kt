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
data class ShiftProfile(
    val id: String,
    val name: String,
    val cycleStartDate: String? = null,
    val cycleEndDate: String? = null,
    val pattern: List<ShiftDefinition> = defaultPattern,
    val overrides: Map<String, ShiftDefinition> = emptyMap(),
)

@Serializable
data class CalendarData(
    val activeProfileId: String = "default",
    val profiles: List<ShiftProfile> = listOf(ShiftProfile(id = "default", name = "Default")),
    val showLunar: Boolean = true,
    val notes: Map<String, String> = emptyMap(),
) {
    // Secondary constructor for backward compatibility in tests and initialization
    constructor(
        cycleStartDate: String? = null,
        cycleEndDate: String? = null,
        pattern: List<ShiftDefinition> = defaultPattern,
        notes: Map<String, String> = emptyMap(),
        overrides: Map<String, ShiftDefinition> = emptyMap(),
        showLunar: Boolean = true
    ) : this(
        activeProfileId = "default",
        profiles = listOf(
            ShiftProfile(
                id = "default",
                name = "Default",
                cycleStartDate = cycleStartDate,
                cycleEndDate = cycleEndDate,
                pattern = pattern,
                overrides = overrides
            )
        ),
        showLunar = showLunar,
        notes = notes
    )

    val activeProfile: ShiftProfile
        get() = profiles.find { it.id == activeProfileId } ?: profiles.firstOrNull() ?: ShiftProfile(id = "default", name = "Default")

    val cycleStartDate: String? get() = activeProfile.cycleStartDate
    val cycleEndDate: String? get() = activeProfile.cycleEndDate
    val pattern: List<ShiftDefinition> get() = activeProfile.pattern
    val overrides: Map<String, ShiftDefinition> get() = activeProfile.overrides
}

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
    val type: HolidayType = HolidayType.HOLIDAY,
)

enum class HolidayType {
    HOLIDAY,
    OBSERVANCE,
}

val defaultPattern: List<ShiftDefinition> = listOf(
    ShiftDefinition(id = "1", name = "白班", color = ShiftColorOption.BLUE),
    ShiftDefinition(id = "2", name = "夜班", color = ShiftColorOption.INDIGO),
    ShiftDefinition(id = "3", name = "休息", color = ShiftColorOption.GREEN),
    ShiftDefinition(id = "3", name = "休息", color = ShiftColorOption.GREEN),
)

val vacationShift = ShiftDefinition(
    id = "vacation",
    name = "休假",
    color = ShiftColorOption.PINK,
)

val businessTripShift = ShiftDefinition(
    id = "business_trip",
    name = "出差",
    color = ShiftColorOption.ORANGE,
)
