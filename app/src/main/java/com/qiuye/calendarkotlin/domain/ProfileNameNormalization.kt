package com.qiuye.calendarkotlin.domain

private val bracketSpacingRegex = Regex("\\s*([()（）\\[\\]【】])\\s*")

fun normalizeProfileName(value: String): String =
    value
        .trim()
        .replace(bracketSpacingRegex, "$1")
