package com.qiuye.calendarkotlin.ui

/**
 * 英文界面节日 Emoji 映射。
 *
 * 仅覆盖 [holidays_en.json] 中现有的 8 个节日。未识别的节日返回 null,
 * 由调用方回退到原右上角 holiday 小标签渲染,不丢信息。
 */
fun festivalEmoji(name: String): String? = when (name) {
    "New Year's Day" -> "🎉"
    "Valentine's Day" -> "💝"
    "Mother's Day" -> "👩"
    "Father's Day" -> "👔"
    "Halloween" -> "🎃"
    "Thanksgiving" -> "🍗"
    "Christmas Eve" -> "🎄"
    "Christmas Day" -> "🎅"
    else -> null
}
