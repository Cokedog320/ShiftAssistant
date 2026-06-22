package com.qiuye.calendarkotlin.ui.theme

import androidx.core.os.LocaleListCompat

enum class LanguageMode {
    SYSTEM,
    ZH,
    EN,
}

fun LanguageMode.resolve(): LocaleListCompat = when (this) {
    LanguageMode.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
    LanguageMode.ZH -> LocaleListCompat.forLanguageTags("zh-CN")
    LanguageMode.EN -> LocaleListCompat.forLanguageTags("en")
}

fun LocaleListCompat.toLanguageMode(): LanguageMode {
    if (isEmpty) return LanguageMode.SYSTEM
    val language = get(0)?.language ?: return LanguageMode.SYSTEM
    return when (language.lowercase()) {
        "zh" -> LanguageMode.ZH
        "en" -> LanguageMode.EN
        else -> LanguageMode.SYSTEM
    }
}
