package com.qiuye.calendarkotlin.domain

import com.qiuye.calendarkotlin.model.CalendarData
import com.qiuye.calendarkotlin.model.DayCell
import com.qiuye.calendarkotlin.model.ShiftDefinition
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

object CalendarCalculator {
    fun buildMonthGrid(
        month: YearMonth,
        calendarData: CalendarData,
        selectedDate: LocalDate?,
        today: LocalDate = LocalDate.now(),
        reminderDates: Set<LocalDate> = emptySet(),
        diaryDates: Set<String> = emptySet(),
    ): List<DayCell> {
        val firstVisibleDay = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val lastVisibleDay = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        val days = buildList {
            var current = firstVisibleDay
            while (!current.isAfter(lastVisibleDay)) {
                add(current)
                current = current.plusDays(1)
            }
        }

        return days.map { date ->
            val lunarDisplay = ChineseCalendarInfo.getLunarDisplay(date)
            DayCell(
                date = date,
                inCurrentMonth = date.month == month.month,
                isToday = date == today,
                isSelected = date == selectedDate,
                shift = getShiftForDate(date, calendarData),
                hasNote = !calendarData.notes[date.toStorageKey()].isNullOrBlank(),
                hasReminder = date in reminderDates,
                hasDiary = date.toStorageKey() in diaryDates,
                lunarLabel = if (calendarData.showLunar) lunarDisplay.label else "",
                lunarFullText = if (calendarData.showLunar) lunarDisplay.fullText else "",
                holiday = ChineseCalendarInfo.getHolidayMarker(date),
            )
        }
    }

    fun getShiftForDate(date: LocalDate, calendarData: CalendarData): ShiftDefinition? {
        calendarData.overrides[date.toStorageKey()]?.let { return it }

        if (calendarData.pattern.isEmpty()) return null

        val isAllSameShift = calendarData.pattern.all { it.id == calendarData.pattern.first().id }
        if (isAllSameShift) {
            val firstShift = calendarData.pattern.first()
            val cycleStartDate = calendarData.cycleStartDate?.let(::parseStorageDate)
            val cycleEndDate = calendarData.cycleEndDate?.takeIf { it.isNotBlank() }?.let(::parseStorageDate)

            if (cycleStartDate != null && date.isBefore(cycleStartDate)) return null
            if (cycleEndDate != null && date.isAfter(cycleEndDate)) return null

            return firstShift
        }

        val cycleStartDate = calendarData.cycleStartDate?.let(::parseStorageDate) ?: return null
        val cycleEndDate = calendarData.cycleEndDate?.takeIf { it.isNotBlank() }?.let(::parseStorageDate)

        if (date.isBefore(cycleStartDate)) return null
        if (cycleEndDate != null && date.isAfter(cycleEndDate)) return null

        val index = (ChronoUnit.DAYS.between(cycleStartDate, date) % calendarData.pattern.size).toInt()
        return calendarData.pattern[index]
    }

    fun getDayCell(
        date: LocalDate,
        calendarData: CalendarData,
        reminderDates: Set<LocalDate> = emptySet(),
        diaryDates: Set<String> = emptySet(),
        today: LocalDate = LocalDate.now(),
    ): DayCell {
        val lunarDisplay = ChineseCalendarInfo.getLunarDisplay(date)
        return DayCell(
            date = date,
            inCurrentMonth = true,
            isToday = date == today,
            isSelected = true,
            shift = getShiftForDate(date, calendarData),
            hasNote = !calendarData.notes[date.toStorageKey()].isNullOrBlank(),
            hasReminder = date in reminderDates,
            hasDiary = date.toStorageKey() in diaryDates,
            lunarLabel = if (calendarData.showLunar) lunarDisplay.label else "",
            lunarFullText = if (calendarData.showLunar) lunarDisplay.fullText else "",
            holiday = ChineseCalendarInfo.getHolidayMarker(date),
        )
    }
}

fun LocalDate.toStorageKey(): String = toString()

fun parseStorageDate(value: String): LocalDate = LocalDate.parse(value)

