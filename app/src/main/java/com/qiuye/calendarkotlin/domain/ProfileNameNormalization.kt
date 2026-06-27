package com.qiuye.calendarkotlin.domain

private const val profileNameSpacingChars = "\\s\\u00A0\\u1680\\u180E\\u2000-\\u200D\\u202F\\u205F\\u2060\\u3000\\uFEFF"

private val edgeSpacingRegex = Regex("^[$profileNameSpacingChars]+|[$profileNameSpacingChars]+$")
private val bracketSpacingRegex = Regex("[$profileNameSpacingChars]*([()（）\\[\\]【】])[$profileNameSpacingChars]*")

// Convert full-width brackets to half-width so Chinese IME won't insert spaces around them.
// IME spacing rules treat full-width brackets as "spaced enough" but half-width as "too tight",
// so by converting after IME commits we dodge the automatic space insertion entirely.
private val fullWidthBracketMap = mapOf(
    '\uff08' to '(',   // （→ (
    '\uff09' to ')',   // ）→ )
    '\u3010' to '[',   // 【→ [
    '\u3011' to ']',   // 】→ ]
)

fun normalizeProfileName(value: String): String =
    value.map { fullWidthBracketMap[it] ?: it }.joinToString("")
        .replace(edgeSpacingRegex, "")
        .replace(bracketSpacingRegex, "$1")

fun normalizeProfileNameInput(value: String, maxLength: Int): String? {
    val normalized = normalizeProfileName(value)
    return normalized.takeIf { it.length <= maxLength }
}
