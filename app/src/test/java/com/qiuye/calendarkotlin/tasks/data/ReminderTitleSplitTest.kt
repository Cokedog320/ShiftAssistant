package com.qiuye.calendarkotlin.tasks.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTitleSplitTest {

    private fun splitInput(input: String): Pair<String, String> {
        val lines = input.lines()
        val titleToSave = lines.firstOrNull()?.trim().orEmpty()
        val noteToSave = lines.drop(1).joinToString("\n").trim()
        return Pair(titleToSave, noteToSave)
    }

    @Test
    fun testSingleLineInput() {
        val input = "去超市买牛奶"
        val (title, note) = splitInput(input)
        assertEquals("去超市买牛奶", title)
        assertEquals("", note)
    }

    @Test
    fun testMultiLineInput() {
        val input = "去超市买牛奶\n买脱脂的\n顺便带包盐"
        val (title, note) = splitInput(input)
        assertEquals("去超市买牛奶", title)
        assertEquals("买脱脂的\n顺便带包盐", note)
    }

    @Test
    fun testEmptyInput() {
        val input = ""
        val (title, note) = splitInput(input)
        assertEquals("", title)
        assertEquals("", note)
    }

    @Test
    fun testWhitespaceInput() {
        val input = "   \n  some note  "
        val (title, note) = splitInput(input)
        assertEquals("", title)
        assertEquals("some note", note)
    }
}
