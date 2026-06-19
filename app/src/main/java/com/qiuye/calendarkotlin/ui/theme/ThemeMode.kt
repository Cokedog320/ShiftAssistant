package com.qiuye.calendarkotlin.ui.theme

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

fun ThemeMode.resolve(isSystemDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
