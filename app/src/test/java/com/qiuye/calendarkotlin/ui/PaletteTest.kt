package com.qiuye.calendarkotlin.ui

import androidx.compose.ui.graphics.toArgb
import com.qiuye.calendarkotlin.model.ShiftColorOption
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
}
