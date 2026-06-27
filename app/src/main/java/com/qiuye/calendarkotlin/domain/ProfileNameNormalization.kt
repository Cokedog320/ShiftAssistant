package com.qiuye.calendarkotlin.domain

private const val profileNameSpacingChars = "\\s\\u00A0\\u1680\\u180E\\u2000-\\u200D\\u202F\\u205F\\u2060\\u3000\\uFEFF"

private val edgeSpacingRegex = Regex("^[$profileNameSpacingChars]+|[$profileNameSpacingChars]+$")
private val bracketSpacingRegex = Regex("[$profileNameSpacingChars]*([()（）\\[\\]【】])[$profileNameSpacingChars]*")

fun normalizeProfileName(value: String): String =
    value
        .replace(edgeSpacingRegex, "")
        .replace(bracketSpacingRegex, "$1")

fun normalizeProfileNameInput(value: String, maxLength: Int): String? {
    val normalized = normalizeProfileName(value)
    return normalized.takeIf { it.length <= maxLength }
}
