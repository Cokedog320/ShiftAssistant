package com.qiuye.calendarkotlin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF0A0A0A),
    primaryContainer = Color(0xFF1B2A3A),
    onPrimaryContainer = Color(0xFFD8E6FF),
    secondary = Color(0xFFB0BEC5),
    secondaryContainer = Color(0xFF25313A),
    onSecondaryContainer = Color(0xFFD8E1E8),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF3F3F3),
    surface = Color(0xFF111111),
    onSurface = Color(0xFFF3F3F3),
    surfaceVariant = Color(0xFF1B1B1B),
    onSurfaceVariant = Color(0xFFB7B7B7),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF4A4A4A),
    outlineVariant = Color(0xFF323232),
)

@Composable
fun CalendarKotlinTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
