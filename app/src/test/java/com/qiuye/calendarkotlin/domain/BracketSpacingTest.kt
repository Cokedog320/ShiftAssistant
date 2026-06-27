package com.qiuye.calendarkotlin.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BracketSpacingTest {
    @Test
    fun `trims ascii space around brackets`() {
        assertEquals("班(夜)", normalizeProfileName("班 (夜)"))
        assertEquals("班(夜)", normalizeProfileName("班(夜) "))
        assertEquals("班(夜)", normalizeProfileName(" 班(夜)"))
    }

    @Test
    fun `trims full-width space around brackets`() {
        assertEquals("班(夜)", normalizeProfileName("班\u3000(夜)"))
        assertEquals("班(夜)", normalizeProfileName("班(夜)\u3000"))
        assertEquals("班(夜)", normalizeProfileName("\u3000班(夜)"))
    }

    @Test
    fun `trims full-width space before open bracket from chinese IME`() {
        assertEquals("本班班次(", normalizeProfileName("本班班次\u3000("))
        assertEquals("本班班次(二班)", normalizeProfileName("本班班次\u3000(二班)"))
    }

    @Test
    fun `trims invisible ime spacing around brackets`() {
        assertEquals("方案(一)", normalizeProfileName("方案\u00A0(一)\u00A0"))
        assertEquals("方案(一)", normalizeProfileName("方案\u202F(一)\u202F"))
        assertEquals("方案(一)", normalizeProfileName("方案\u200B(一)\u200B"))
    }

    @Test
    fun `checks profile name length after normalizing ime spacing`() {
        assertEquals("方案(一)", normalizeProfileNameInput("方案\u00A0(一)\u00A0", maxLength = 5))
        assertEquals(null, normalizeProfileNameInput("方案名称太长了(一)", maxLength = 5))
    }
}
