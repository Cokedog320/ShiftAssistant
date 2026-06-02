package com.qiuye.calendarkotlin.tasks.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderDateTimeTest {

    @Test
    fun startOfDayMillis_andCombineDateAndMinutes_useFixedNonUtcZone() {
        val zoneId = ZoneId.of("Asia/Shanghai")
        val input = ZonedDateTime.of(2026, 4, 9, 15, 42, 11, 0, zoneId).toInstant().toEpochMilli()

        val dayStart = startOfDayMillis(input, zoneId)
        val expectedDayStart = ZonedDateTime.of(2026, 4, 9, 0, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        assertEquals(expectedDayStart, dayStart)

        val combined = combineDateAndMinutes(dayStart, 10 * 60 + 17, zoneId)
        val expectedCombined = ZonedDateTime.of(2026, 4, 9, 10, 17, 0, 0, zoneId).toInstant().toEpochMilli()
        assertEquals(expectedCombined, combined)
        assertEquals(10 * 60 + 17, minutesOfDay(combined, zoneId))
    }

    @Test
    fun combineDateAndMinutes_handlesDstSpringForwardGap() {
        val zoneId = ZoneId.of("America/Los_Angeles")
        val dayStart = ZonedDateTime.of(2026, 3, 8, 0, 0, 0, 0, zoneId).toInstant().toEpochMilli()

        val combined = combineDateAndMinutes(dayStart, 2 * 60 + 30, zoneId)

        val expected = ZonedDateTime.of(2026, 3, 8, 3, 30, 0, 0, zoneId).toInstant().toEpochMilli()
        assertEquals(expected, combined)
        assertEquals(3 * 60 + 30, minutesOfDay(combined, zoneId))
    }

    @Test
    fun roundedUpFiveMinuteSlot_rollsOverAtMidnight() {
        val zoneId = ZoneId.of("Asia/Shanghai")
        val now = ZonedDateTime.of(2026, 4, 9, 23, 58, 0, 0, zoneId).toInstant().toEpochMilli()

        val (millis, roundedMinutes) = roundedUpFiveMinuteSlot(now, zoneId)

        val expected = ZonedDateTime.of(2026, 4, 10, 0, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        assertEquals(expected, millis)
        assertEquals(0, roundedMinutes)
    }
}

