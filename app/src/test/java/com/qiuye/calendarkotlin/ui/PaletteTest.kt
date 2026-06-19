package com.qiuye.calendarkotlin.ui

import androidx.compose.ui.graphics.toArgb
import com.qiuye.calendarkotlin.model.ShiftColorOption
import com.qiuye.calendarkotlin.model.businessTripShift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PaletteTest {

    @Test
    fun `dark palette should use dark container and content colors`() {
        val light = ShiftColorOption.BLUE.palette()
        val dark = ShiftColorOption.BLUE.palette(isDark = true)

        assertNotEquals(light.container, dark.container)
        assertNotEquals(light.content, dark.content)
        assertEquals(0xFF10253C.toInt(), dark.container.toArgb())
        assertEquals(0xFF64B5F6.toInt(), dark.content.toArgb())
    }

    @Test
    fun `ORANGE palette should use correct light colors for business trip shift`() {
        val palette = ShiftColorOption.ORANGE.palette(isDark = false)

        assertEquals(0xFFFFE8D1.toInt(), palette.container.toArgb())
        assertEquals(0xFF9A4D00.toInt(), palette.content.toArgb())
    }

    @Test
    fun `ORANGE palette should use correct dark colors for business trip shift`() {
        val palette = ShiftColorOption.ORANGE.palette(isDark = true)

        assertEquals(0xFF351C05.toInt(), palette.container.toArgb())
        assertEquals(0xFFFFB74D.toInt(), palette.content.toArgb())
    }

    @Test
    fun `businessTripShift color should use ORANGE palette`() {
        val palette = businessTripShift.color.palette(isDark = false)

        assertEquals(0xFFFFE8D1.toInt(), palette.container.toArgb())
    }

    @Test
    fun `ORANGE label should be correct`() {
        assertEquals("橙", ShiftColorOption.ORANGE.label())
    }
}
