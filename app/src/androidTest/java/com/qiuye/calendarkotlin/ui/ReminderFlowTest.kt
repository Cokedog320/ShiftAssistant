package com.qiuye.calendarkotlin.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.qiuye.calendarkotlin.tasks.data.ReminderEntity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReminderFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun remindersSheet_displaysRemindersAndHandlesInteractions() {
        var addClicked = false
        var deleteClicked = false
        var toggleClicked = false
        
        val reminders = listOf(
            ReminderEntity(1, "Task 1", "Note 1", System.currentTimeMillis() + 3600_000, false, 0, 0)
        )

        composeTestRule.setContent {
            RemindersBottomSheet(
                reminders = reminders,
                onDismiss = {},
                onToggleReminder = { toggleClicked = true },
                onDeleteReminder = { deleteClicked = true },
                onOpenReminder = {},
                onJumpToDate = {},
                onAddReminder = { addClicked = true }
            )
        }

        // Verify reminder is displayed
        composeTestRule.onNodeWithText("Task 1").assertIsDisplayed()

        // Click Add
        composeTestRule.onNodeWithContentDescription("添加提醒").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { addClicked }
        assertTrue(addClicked)

        // Click Delete
        composeTestRule.onNodeWithContentDescription("删除").performClick()
        assertTrue(deleteClicked)
    }
}
