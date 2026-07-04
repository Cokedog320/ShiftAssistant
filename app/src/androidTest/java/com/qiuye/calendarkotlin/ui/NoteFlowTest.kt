package com.qiuye.calendarkotlin.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.qiuye.calendarkotlin.viewmodel.NoteEntry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import com.qiuye.calendarkotlin.ui.noteMonthFormatter

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
        composeTestRule.onNodeWithTag("note_item_$testDate1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("note_item_$testDate2").assertIsDisplayed()

        // Perform search
        composeTestRule.onNodeWithTag("input_notes_search").performTextInput("milk")
        
        // Verify only one entry is displayed
        composeTestRule.onNodeWithTag("note_item_$testDate1").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("note_item_$testDate2").assertCountEquals(0)
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

        // Click delete button on the note card
        composeTestRule.onNodeWithTag("btn_notes_delete_$testDate").performClick()

        // Confirm in dialog
        composeTestRule.onNodeWithTag("btn_dialog_confirm").performClick()

        assertEquals(testDate, deletedDate)
    }

    @Test
    fun notesSheet_displaysYearDrawerAndMonthDrawer() {
        val entry2027 = NoteEntry(
            LocalDate.of(2027, 1, 10),
            "next year",
            YearMonth.of(2027, 1),
            null,
            "",
        )
        val entry2026 = NoteEntry(
            LocalDate.of(2026, 12, 10),
            "this year",
            YearMonth.of(2026, 12),
            null,
            "",
        )

        composeTestRule.setContent {
            NotesBottomSheet(
                noteEntries = listOf(entry2027, entry2026),
                showLunar = false,
                reminders = emptyList(),
                accentColor = Color.Blue,
                onDismiss = {},
                onSelectNoteDate = {},
                onDeleteNote = {},
            )
        }

        val monthTag = "note_group_${entry2027.month.format(noteMonthFormatter())}"

        composeTestRule.onNodeWithTag("note_year_group_2027").assertIsDisplayed()
        composeTestRule.onNodeWithTag("note_year_group_2026").assertIsDisplayed()
        composeTestRule.onNodeWithTag("note_all_years").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(monthTag).assertCountEquals(0)

        composeTestRule.onNodeWithTag("note_year_group_2027").performClick()
        composeTestRule.onNodeWithTag(monthTag).assertIsDisplayed()

        composeTestRule.onNodeWithTag("note_year_group_2027").performClick()
        composeTestRule.onAllNodesWithTag(monthTag).assertCountEquals(0)
    }
}
