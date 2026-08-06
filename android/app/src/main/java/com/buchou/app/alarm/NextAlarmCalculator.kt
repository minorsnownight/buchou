package com.buchou.app.alarm

import java.time.LocalDate
import java.time.ZonedDateTime

object NextAlarmCalculator {
    fun next(
        now: ZonedDateTime,
        settings: ReminderSettings,
        earliestDate: LocalDate = now.toLocalDate(),
    ): ZonedDateTime? {
        if (!settings.enabled || settings.weekdays.isEmpty()) return null
        val firstDate = maxOf(now.toLocalDate(), earliestDate)
        for (offset in 0L..7L) {
            val date = firstDate.plusDays(offset)
            if (date.dayOfWeek !in settings.weekdays) continue
            val candidate = date
                .atTime(settings.hour, settings.minute)
                .atZone(now.zone)
            if (candidate.isAfter(now)) return candidate
        }
        return null
    }
}

