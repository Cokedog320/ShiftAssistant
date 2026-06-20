package com.qiuye.calendarkotlin.ui.theme

import androidx.appcompat.app.AppCompatDelegate

fun isEnglishAppLocale(): Boolean {
    val firstTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        .split(",")
        .firstOrNull()
        .orEmpty()
    return firstTag.startsWith("en", ignoreCase = true)
}
