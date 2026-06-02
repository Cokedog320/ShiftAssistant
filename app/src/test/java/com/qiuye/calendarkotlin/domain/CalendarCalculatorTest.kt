package com.qiuye.calendarkotlin.domain

import com.qiuye.calendarkotlin.BaseUnitTest
import com.qiuye.calendarkotlin.model.CalendarData
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
}
