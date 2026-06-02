package com.qiuye.calendarkotlin.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.qiuye.calendarkotlin.viewmodel.NoteEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class NoteFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun notesSheet_displaysEntriesAndFiltersBySearch() {
        val testDate1 = LocalDate.of(2026, 6, 15)
        val testDate2 = LocalDate.of(2026, 6, 16)
        
        val entries = listOf(
            NoteEntry(testDate1, "Buy milk", YearMonth.of(2026, 6), null, ""),
            NoteEntry(testDate2, "Learn Compose", YearMonth.of(2026, 6), null, "")
        )

        composeTestRule.setContent {
            NotesBottomSheet(
                noteEntries = entries,
                showLunar = false,
                reminders = emptyList(),
                accentColor = Color.Blue,
                onDismiss = {},
                onSelectNoteDate = {},
                onDeleteNote = {}
            )
        }

        // Verify both entries are displayed
        composeTestRule.onNodeWithText("Buy milk").assertIsDisplayed()
        composeTestRule.onNodeWithText("Learn Compose").assertIsDisplayed()

        // Perform search
        composeTestRule.onNodeWithTag("input_notes_search").performTextInput("milk")
        
        // Verify only one entry is displayed
        composeTestRule.onNodeWithText("Buy milk").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Learn Compose").assertCountEquals(0)
    }

    @Test
    fun notesSheet_deleteActionTriggersCallback() {
        val testDate = LocalDate.of(2026, 6, 15)
        var deletedDate: LocalDate? = null

        val entries = listOf(
            NoteEntry(testDate, "Delete me", YearMonth.of(2026, 6), null, "")
        )

        composeTestRule.setContent {
            NotesBottomSheet(
                noteEntries = entries,
                showLunar = false,
                reminders = emptyList(),
                accentColor = Color.Blue,
                onDismiss = {},
                onSelectNoteDate = {},
                onDeleteNote = { deletedDate = it }
            )
        }

        // Click checkbox to select the note
        composeTestRule.onNodeWithTag("note_checkbox_$testDate").performClick()

        // Click delete button (which appears after selection)
        composeTestRule.onNodeWithTag("btn_notes_delete").performClick()

        // Confirm in dialog
        composeTestRule.onNodeWithTag("btn_dialog_confirm").performClick()

        assertEquals(testDate, deletedDate)
    }
}
