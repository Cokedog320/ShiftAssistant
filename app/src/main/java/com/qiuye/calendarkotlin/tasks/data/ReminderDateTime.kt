package com.qiuye.calendarkotlin.tasks.data

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal var zoneIdProvider: () -> ZoneId = { ZoneId.systemDefault() }
private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(zoneIdProvider()).toLocalDate().format(dateFormatter)

fun formatTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(zoneIdProvider()).toLocalTime().format(timeFormatter)

fun formatDateTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(zoneIdProvider()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault()))

fun startOfDayMillis(millis: Long, zoneId: ZoneId = zoneIdProvider()): Long {
    val localDate = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
    return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
}

fun minutesOfDay(millis: Long, zoneId: ZoneId = zoneIdProvider()): Int {
    val time = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalTime()
    return time.hour * 60 + time.minute
}

fun combineDateAndMinutes(dateStartOfDayMillis: Long, minutesOfDay: Int, zoneId: ZoneId = zoneIdProvider()): Long {
    val localDate = Instant.ofEpochMilli(dateStartOfDayMillis).atZone(zoneId).toLocalDate()
    val hour = minutesOfDay / 60
    val minute = minutesOfDay % 60
    return localDate.atTime(hour, minute).atZone(zoneId).toInstant().toEpochMilli()
}

fun roundedUpFiveMinuteSlot(nowMillis: Long = System.currentTimeMillis(), zoneId: ZoneId = zoneIdProvider()): Pair<Long, Int> {
    val current = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
    val totalMinutes = current.hour * 60 + current.minute
    val roundedMinutes = ((totalMinutes + 4) / 5) * 5
    return if (roundedMinutes >= 24 * 60) {
        current.toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() to 0
    } else {
        val adjustedDateTime = current.toLocalDate().atStartOfDay().plusMinutes(roundedMinutes.toLong())
        adjustedDateTime.atZone(zoneId).toInstant().toEpochMilli() to roundedMinutes
    }
}

fun reminderDateTimeLabel(scheduledAtMillis: Long): String =
    "${formatDate(scheduledAtMillis)} ${formatTime(scheduledAtMillis)}"

fun nowDateStartMillis(): Long = startOfDayMillis(System.currentTimeMillis())

fun localDateTimeFrom(dateStartOfDayMillis: Long, minutesOfDay: Int): LocalDateTime {
    val localDate = Instant.ofEpochMilli(dateStartOfDayMillis).atZone(zoneIdProvider()).toLocalDate()
    return localDate.atTime(minutesOfDay / 60, minutesOfDay % 60)
}


