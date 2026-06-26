package com.qiuye.calendarkotlin.ui

import java.time.format.DateTimeFormatter
import java.util.Locale
import com.qiuye.calendarkotlin.ui.theme.isEnglishAppLocale

internal fun monthFormatter(): DateTimeFormatter = when {
    isEnglishAppLocale() -> DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
    else -> DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)
}

internal fun fullDateFormatter(): DateTimeFormatter = when {
    isEnglishAppLocale() -> DateTimeFormatter.ofPattern("MMM d, yyyy · EEEE", Locale.ENGLISH)
    else -> DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE", Locale.CHINA)
}

internal fun shortDateFormatter(): DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

internal fun noteMonthFormatter(): DateTimeFormatter = when {
    isEnglishAppLocale() -> DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    else -> DateTimeFormatter.ofPattern("yyyy年MM月", Locale.CHINA)
}

internal fun noteDateFormatter(): DateTimeFormatter = when {
    isEnglishAppLocale() -> DateTimeFormatter.ofPattern("MMM d, EEEE", Locale.ENGLISH)
    else -> DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)
}

internal fun pickerMonthFormatter(): DateTimeFormatter = when {
    isEnglishAppLocale() -> DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    else -> DateTimeFormatter.ofPattern("yyyy年MM月", Locale.CHINA)
}

internal fun shortMonthFormatter(): DateTimeFormatter = when {
    isEnglishAppLocale() -> DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    else -> DateTimeFormatter.ofPattern("yyyy-MM", Locale.CHINA)
}

internal fun weekdayLabels(): List<String> = when {
    isEnglishAppLocale() -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    else -> listOf("一", "二", "三", "四", "五", "六", "日")
}

