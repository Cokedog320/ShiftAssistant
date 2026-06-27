package com.qiuye.calendarkotlin.domain

// \s + explicit Unicode Space_Separator codepoints (Chinese IME may insert any of these around brackets)
private val bracketSpacingRegex = Regex("[\\s\u3000\u00A0\u2000-\u200A\u202F\u205F]*([()（）\\[\\]【】])[\\s\u3000\u00A0\u2000-\u200A\u202F\u205F]*")

fun normalizeProfileName(value: String): String =
    value
        .trim()
        .replace(bracketSpacingRegex, "$1")
