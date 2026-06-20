package com.qiuye.calendarkotlin.domain

import com.qiuye.calendarkotlin.model.ShiftDefinition
import com.qiuye.calendarkotlin.ui.theme.isEnglishAppLocale

fun ShiftDefinition.displayName(): String =
    when (id) {
        "1" -> if (isEnglishAppLocale()) "Day Shift" else "白班"
        "2" -> if (isEnglishAppLocale()) "Night Shift" else "夜班"
        "3" -> if (isEnglishAppLocale()) "Off" else "休息"
        "vacation" -> if (isEnglishAppLocale()) "😎 Leave" else "😎 休假"
        "business_trip" -> if (isEnglishAppLocale()) "Trip" else "出差"
        else -> name
    }
