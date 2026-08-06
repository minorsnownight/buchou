package com.buchou.app.alarm

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextAlarmCalculatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun `uses today when reminder time is still in the future`() {
        val now = ZonedDateTime.of(2026, 8, 4, 20, 0, 0, 0, zone)
        val next = NextAlarmCalculator.next(now, ReminderSettings(enabled = true, hour = 21))

        assertEquals(ZonedDateTime.of(2026, 8, 4, 21, 0, 0, 0, zone), next)
    }

    @Test
    fun `moves to tomorrow when today's time has passed`() {
        val now = ZonedDateTime.of(2026, 8, 4, 22, 0, 0, 0, zone)
        val next = NextAlarmCalculator.next(now, ReminderSettings(enabled = true, hour = 21))

        assertEquals(ZonedDateTime.of(2026, 8, 5, 21, 0, 0, 0, zone), next)
    }

    @Test
    fun `skips disabled weekdays`() {
        val monday = ZonedDateTime.of(2026, 8, 3, 8, 0, 0, 0, zone)
        val settings = ReminderSettings(
            enabled = true,
            hour = 21,
            weekdays = setOf(DayOfWeek.WEDNESDAY),
        )

        assertEquals(
            ZonedDateTime.of(2026, 8, 5, 21, 0, 0, 0, zone),
            NextAlarmCalculator.next(monday, settings),
        )
    }

    @Test
    fun `check in skips the rest of today`() {
        val now = ZonedDateTime.of(2026, 8, 4, 8, 0, 0, 0, zone)
        val next = NextAlarmCalculator.next(
            now = now,
            settings = ReminderSettings(enabled = true, hour = 21),
            earliestDate = LocalDate.of(2026, 8, 5),
        )

        assertEquals(ZonedDateTime.of(2026, 8, 5, 21, 0, 0, 0, zone), next)
    }

    @Test
    fun `disabled reminder has no next alarm`() {
        val now = ZonedDateTime.of(2026, 8, 4, 8, 0, 0, 0, zone)

        assertNull(NextAlarmCalculator.next(now, ReminderSettings()))
    }
}
