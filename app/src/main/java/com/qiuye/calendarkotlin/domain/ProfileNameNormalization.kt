package com.qiuye.calendarkotlin.domain

private val bracketSpacingRegex = Regex("[\\s\\p{Zs}]*([()（）\\[\\]【】])[\\s\\p{Zs}]*")

fun normalizeProfileName(value: String): String =
    value
        .trim()
        .replace(bracketSpacingRegex, "$1")
