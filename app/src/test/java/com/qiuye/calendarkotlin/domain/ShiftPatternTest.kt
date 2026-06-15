package com.qiuye.calendarkotlin.domain

import com.qiuye.calendarkotlin.BaseUnitTest
import com.qiuye.calendarkotlin.model.CalendarData
import com.qiuye.calendarkotlin.model.ShiftColorOption
import com.qiuye.calendarkotlin.model.ShiftDefinition
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ShiftPatternTest : BaseUnitTest() {

    private val pattern = listOf(
        ShiftDefinition("1", "白班", ShiftColorOption.BLUE),
        ShiftDefinition("2", "夜班", ShiftColorOption.INDIGO),
        ShiftDefinition("3", "休息", ShiftColorOption.GREEN),
        ShiftDefinition("4", "休息", ShiftColorOption.GREEN)
    )

    @Test
    fun `getShiftForDate should calculate repeating pattern correctly`() {
        val cycleStart = "2024-01-01"
        val data = CalendarData(
            pattern = pattern,
            cycleStartDate = cycleStart
        )
        
        // Day 0: 2024-01-01 -> 白班 (Index 0)
        var shift = CalendarCalculator.getShiftForDate(LocalDate.of(2024, 1, 1), data)
        assertEquals("白班", shift?.name)

        // Day 1: 2024-01-02 -> 夜班 (Index 1)
        shift = CalendarCalculator.getShiftForDate(LocalDate.of(2024, 1, 2), data)
        assertEquals("夜班", shift?.name)
        
        // Day 4: 2024-01-05 -> repeats to 白班 (Index 0)
        shift = CalendarCalculator.getShiftForDate(LocalDate.of(2024, 1, 5), data)
        assertEquals("白班", shift?.name)
    }

    @Test
    fun `getShiftForDate should respect override over pattern`() {
        val overrideShift = ShiftDefinition("99", "请假", ShiftColorOption.PURPLE)
        val data = CalendarData(
            pattern = pattern,
            cycleStartDate = "2024-01-01",
            overrides = mapOf("2024-01-02" to overrideShift)
        )
        
        // normally index 1 (夜班), but overridden
        val shift = CalendarCalculator.getShiftForDate(LocalDate.of(2024, 1, 2), data)
        assertEquals("请假", shift?.name)
    }

    @Test
    fun `getShiftForDate should return null before cycleStartDate`() {
        val data = CalendarData(
            pattern = pattern,
            cycleStartDate = "2024-01-05"
        )
        
        // Date is before cycle start
        val shift = CalendarCalculator.getShiftForDate(LocalDate.of(2024, 1, 1), data)
        assertNull(shift)
    }

    @Test
    fun `getShiftForDate should return null if pattern is empty`() {
        val data = CalendarData(
            pattern = emptyList(),
            cycleStartDate = "2024-01-01"
        )
        
        val shift = CalendarCalculator.getShiftForDate(LocalDate.of(2024, 1, 1), data)
        assertNull(shift)
    }

    @Test
    fun `getShiftForDate should return shift when pattern is all same and cycleStartDate is null`() {
        val singleShiftPattern = listOf(ShiftDefinition("1", "白班", ShiftColorOption.BLUE))
        val data = CalendarData(
            pattern = singleShiftPattern,
            cycleStartDate = null
        )
        val shift = CalendarCalculator.getShiftForDate(LocalDate.of(2024, 6, 15), data)
        assertNotNull(shift)
        assertEquals("白班", shift?.name)
    }

    @Test
    fun `getShiftForDate should return shift when pattern is all same and cycleStartDate is set and date is valid`() {
        val singleShiftPattern = listOf(ShiftDefinition("1", "白班", ShiftColorOption.BLUE))
        val data = CalendarData(
            pattern = singleShiftPattern,
            cycleStartDate = "2024-06-10"
        )
        
        // Date on or after cycleStartDate
        val shiftOn = CalendarCalculator.getShiftForDate(LocalDate.of(2024, 6, 10), data)
        assertEquals("白班", shiftOn?.name)

        val shiftAfter = CalendarCalculator.getShiftForDate(LocalDate.of(2024, 6, 15), data)
        assertEquals("白班", shiftAfter?.name)

        // Date before cycleStartDate should return null
        val shiftBefore = CalendarCalculator.getShiftForDate(LocalDate.of(2024, 6, 9), data)
        assertNull(shiftBefore)
    }
}
