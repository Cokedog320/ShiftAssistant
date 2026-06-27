package com.qiuye.calendarkotlin.domain

private val bracketSpacingRegex = Regex("[\\s\u3000]*([()（）\\[\\]【】])[\\s\u3000]*")

fun normalizeProfileName(value: String): String =
    value
        .trim()
        .replace(bracketSpacingRegex, "$1")
