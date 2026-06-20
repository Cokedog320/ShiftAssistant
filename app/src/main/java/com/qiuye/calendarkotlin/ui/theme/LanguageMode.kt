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
