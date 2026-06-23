package com.qiuye.calendarkotlin.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FestivalEmojiTest {

    @Test
    fun `all 8 holidays_en json festivals map to their confirmed emoji`() {
        assertEquals("🎉", festivalEmoji("New Year's Day"))
        assertEquals("💝", festivalEmoji("Valentine's Day"))
        assertEquals("👩", festivalEmoji("Mother's Day"))
        assertEquals("👔", festivalEmoji("Father's Day"))
        assertEquals("🎃", festivalEmoji("Halloween"))
        assertEquals("🍗", festivalEmoji("Thanksgiving"))
        assertEquals("🎄", festivalEmoji("Christmas Eve"))
        assertEquals("🎅", festivalEmoji("Christmas Day"))
    }

    @Test
    fun `unrecognized festival name returns null for fallback rendering`() {
        assertNull(festivalEmoji("Earth Day"))
        assertNull(festivalEmoji("Easter"))
        assertNull(festivalEmoji(""))
    }

    @Test
    fun `festival name matching is case-sensitive to avoid accidental matches`() {
        assertNull(festivalEmoji("new year's day"))
        assertNull(festivalEmoji("CHRISTMAS DAY"))
    }
}
