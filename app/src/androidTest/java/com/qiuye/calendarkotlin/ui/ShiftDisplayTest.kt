package com.qiuye.calendarkotlin.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.qiuye.calendarkotlin.model.DayCell
import com.qiuye.calendarkotlin.model.ShiftColorOption
import com.qiuye.calendarkotlin.model.ShiftDefinition
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ShiftDisplayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shiftLabels_areCorrectlyDisplayedAndTruncated() {
        val testDate = LocalDate.of(2026, 6, 15)
        val cells = listOf(
            DayCell(
                date = testDate,
                inCurrentMonth = true,
                isToday = false,
                isSelected = false,
                lunarLabel = "",
                shift = ShiftDefinition("1", "白班", ShiftColorOption.RED), // Will render as "白" due to monthGridLabel()
                holiday = null,
                hasNote = false,
                hasReminder = false,
                hasDiary = false
            ),
            DayCell(
                date = testDate.plusDays(1),
                inCurrentMonth = true,
                isToday = false,
                isSelected = false,
                lunarLabel = "",
                shift = ShiftDefinition("2", "夜班", ShiftColorOption.BLUE), // "夜"
                holiday = null,
                hasNote = false,
                hasReminder = false,
                hasDiary = false
            )
        )

        composeTestRule.setContent {
            CalendarGrid(
                dayCells = cells,
                seasonAccent = Color.Blue,
                onSelectDate = { },
                modifier = Modifier.padding(16.dp)
            )
        }

        // Verify shift labels
        composeTestRule.onNodeWithText("白").assertIsDisplayed()
        composeTestRule.onNodeWithText("夜").assertIsDisplayed()
    }
}
