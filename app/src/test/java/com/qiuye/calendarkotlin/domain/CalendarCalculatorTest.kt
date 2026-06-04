package com.qiuye.calendarkotlin.domain

import com.qiuye.calendarkotlin.BaseUnitTest
import com.qiuye.calendarkotlin.model.CalendarData
import com.qiuye.calendarkotlin.model.ShiftColorOption
import com.qiuye.calendarkotlin.model.ShiftDefinition
import com.qiuye.calendarkotlin.model.defaultPattern
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarCalculatorTest : BaseUnitTest() {

    @Test
    fun `buildMonthGrid should return correct number of days for a month view`() {
        // February 2024 (Leap year, 2024-02-01 is Thursday)
        // Grid needs to start from previous Monday: Jan 29, 2024
        // Grid ends at next Sunday: Mar 3, 2024
        // Total 35 days
        val month = YearMonth.of(2024, 2)
        val data = CalendarData()
        
        val grid = CalendarCalculator.buildMonthGrid(month, data, null)
        
        assertTrue("Grid size should be multiple of 7", grid.size % 7 == 0)
        assertEquals(35, grid.size)
        assertEquals(LocalDate.of(2024, 1, 29), grid.first().date)
        assertEquals(LocalDate.of(2024, 3, 3), grid.last().date)
    }

    @Test
    fun `buildMonthGrid should correctly mark days inside and outside current month`() {
        val month = YearMonth.of(2024, 2)
        val grid = CalendarCalculator.buildMonthGrid(month, CalendarData(), null)
        
        val janDays = grid.filter { it.date.monthValue == 1 }
        val febDays = grid.filter { it.date.monthValue == 2 }
        
        assertTrue(janDays.all { !it.inCurrentMonth })
        assertTrue(febDays.all { it.inCurrentMonth })
    }

    @Test
    fun `buildMonthGrid should correctly identify today and selected dates`() {
        val month = YearMonth.of(2024, 5)
        val today = LocalDate.of(2024, 5, 10)
        val selected = LocalDate.of(2024, 5, 15)
        
        val grid = CalendarCalculator.buildMonthGrid(
            month = month, 
            calendarData = CalendarData(), 
            selectedDate = selected, 
            today = today
        )
        
        val todayCell = grid.find { it.date == today }
        val selectedCell = grid.find { it.date == selected }
        
        assertTrue(todayCell?.isToday == true)
        assertTrue(selectedCell?.isSelected == true)
    }

    @Test
    fun `getDayCell should return correct cell for a single date`() {
        val date = LocalDate.of(2024, 5, 10)
        val reminderDates = setOf(LocalDate.of(2024, 5, 10))
        val diaryDates = setOf("2024-05-10")
        val data = CalendarData(
            notes = mapOf("2024-05-10" to "meeting"),
            showLunar = false,
        )

        val cell = CalendarCalculator.getDayCell(
            date = date,
            calendarData = data,
            reminderDates = reminderDates,
            diaryDates = diaryDates,
        )

        assertEquals(date, cell.date)
        assertTrue(cell.isSelected)
        assertTrue(cell.inCurrentMonth)
        assertTrue(cell.hasNote)
        assertTrue(cell.hasReminder)
        assertTrue(cell.hasDiary)
    }

    @Test
    fun `getDayCell should return cell with shift`() {
        val date = LocalDate.of(2024, 1, 5)
        val data = CalendarData(
            cycleStartDate = "2024-01-01",
            pattern = defaultPattern,
            showLunar = false,
        )

        val cell = CalendarCalculator.getDayCell(date, data)

        assertNotNull(cell.shift)
        assertEquals("白班", cell.shift!!.name)
    }

    @Test
    fun `getDayCell should indicate no note or reminder when absent`() {
        val date = LocalDate.of(2024, 3, 1)
        val data = CalendarData(showLunar = false)

        val cell = CalendarCalculator.getDayCell(date, data)

        assertFalse(cell.hasNote)
        assertFalse(cell.hasReminder)
        assertFalse(cell.hasDiary)
    }

    @Test
    fun `getDayCell should reflect override shift`() {
        val date = LocalDate.of(2024, 1, 2)
        val override = ShiftDefinition("vacation", "休假", ShiftColorOption.GREEN)
        val data = CalendarData(
            cycleStartDate = "2024-01-01",
            pattern = defaultPattern,
            overrides = mapOf("2024-01-02" to override),
            showLunar = false,
        )

        val cell = CalendarCalculator.getDayCell(date, data)

        assertEquals("休假", cell.shift!!.name)
    }
}
