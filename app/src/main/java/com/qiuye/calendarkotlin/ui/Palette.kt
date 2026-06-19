package com.qiuye.calendarkotlin.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.AcUnit
import com.qiuye.calendarkotlin.model.ShiftColorOption

data class SeasonPalette(val name: String, val icon: ImageVector, val accent: Color, val background: List<Color>)
data class ShiftPalette(val container: Color, val content: Color)

fun seasonPaletteFor(monthValue: Int, isDark: Boolean = false): SeasonPalette = seasonPalette(monthValue, isDark)

internal val darkSeasonBackground = listOf(Color(0xFF0B1118), Color(0xFF111827))

private fun seasonPalette(monthValue: Int, isDark: Boolean): SeasonPalette =
    when (monthValue) {
        in 3..5 -> SeasonPalette(
            "春",
            Icons.Rounded.LocalFlorist,
            if (isDark) Color(0xFF6FCFAD) else Color(0xFF247A5D),
            listOf(Color(0xFFF3FBF7), Color(0xFFDDF3E8)),
        )
        in 6..8 -> SeasonPalette(
            "夏",
            Icons.Rounded.WbSunny,
            if (isDark) Color(0xFF64B5F6) else Color(0xFF1769AA),
            listOf(Color(0xFFF2F8FF), Color(0xFFDCEEFF)),
        )
        in 9..11 -> SeasonPalette(
            "秋",
            Icons.Rounded.Eco,
            if (isDark) Color(0xFFFFB74D) else Color(0xFFB95C09),
            listOf(Color(0xFFFFF7F0), Color(0xFFFFE7CE)),
        )
        else -> SeasonPalette(
            "冬",
            Icons.Rounded.AcUnit,
            if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
            listOf(Color(0xFFF7F8FB), Color(0xFFE5EBF4)),
        )
    }

internal fun ShiftColorOption.palette(isDark: Boolean = false): ShiftPalette =
    when (this) {
        ShiftColorOption.BLUE -> if (isDark) ShiftPalette(Color(0xFF10253C), Color(0xFF64B5F6)) else ShiftPalette(Color(0xFFD9ECFF), Color(0xFF0A4B78))
        ShiftColorOption.INDIGO -> if (isDark) ShiftPalette(Color(0xFF1B1E3D), Color(0xFF7986CB)) else ShiftPalette(Color(0xFFE2E4FF), Color(0xFF353D95))
        ShiftColorOption.GREEN -> if (isDark) ShiftPalette(Color(0xFF0C2B1A), Color(0xFF81C784)) else ShiftPalette(Color(0xFFDDF5E7), Color(0xFF1B6B3B))
        ShiftColorOption.RED -> if (isDark) ShiftPalette(Color(0xFF3A1414), Color(0xFFE57373)) else ShiftPalette(Color(0xFFFFE1E1), Color(0xFF8D1F1F))
        ShiftColorOption.ORANGE -> if (isDark) ShiftPalette(Color(0xFF351C05), Color(0xFFFFB74D)) else ShiftPalette(Color(0xFFFFE8D1), Color(0xFF9A4D00))
        ShiftColorOption.GRAY -> if (isDark) ShiftPalette(Color(0xFF222831), Color(0xFF90A4AE)) else ShiftPalette(Color(0xFFE9EDF2), Color(0xFF415161))
        ShiftColorOption.PURPLE -> if (isDark) ShiftPalette(Color(0xFF2D124D), Color(0xFFBA68C8)) else ShiftPalette(Color(0xFFF0E0FF), Color(0xFF6F2DA8))
        ShiftColorOption.PINK -> if (isDark) ShiftPalette(Color(0xFF3A1025), Color(0xFFF06292)) else ShiftPalette(Color(0xFFFFE0EC), Color(0xFFA62D61))
    }

internal fun ShiftColorOption.label(): String =
    when (this) {
        ShiftColorOption.BLUE -> "蓝"
        ShiftColorOption.INDIGO -> "靛"
        ShiftColorOption.GREEN -> "绿"
        ShiftColorOption.RED -> "红"
        ShiftColorOption.ORANGE -> "橙"
        ShiftColorOption.GRAY -> "灰"
        ShiftColorOption.PURPLE -> "紫"
        ShiftColorOption.PINK -> "粉"
    }

