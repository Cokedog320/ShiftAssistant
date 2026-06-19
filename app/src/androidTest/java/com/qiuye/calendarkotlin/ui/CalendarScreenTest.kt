package com.qiuye.calendarkotlin.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.qiuye.calendarkotlin.model.DayCell
import com.qiuye.calendarkotlin.model.HolidayMarker
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class CalendarScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun calendarGrid_displaysDatesAndHandlesClicks() {
        val testDate = LocalDate.of(2026, 6, 15)
        var clickedDate: LocalDate? = null
        
        val cells = listOf(
            DayCell(
                date = testDate,
                inCurrentMonth = true,
                isToday = false,
                isSelected = false,
                lunarLabel = "初一",
                shift = null,
                holiday = HolidayMarker("端午节", "休", false),
                hasNote = true,
                hasReminder = true,
                hasDiary = false
            )
        )

        composeTestRule.setContent {
            CalendarGrid(
                dayCells = cells,
                seasonAccent = Color.Blue,
                onSelectDate = { clickedDate = it }
            )
        }

        // Verify the date is displayed
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
        
        // Verify the holiday label and lunar label
        composeTestRule.onNodeWithText("休").assertIsDisplayed()
        composeTestRule.onNodeWithText("初一").assertIsDisplayed()

        // Perform click
        composeTestRule.onNodeWithTag("day_cell_$testDate").performClick()

        // Assert click was registered
        assertEquals(testDate, clickedDate)
    }
}
