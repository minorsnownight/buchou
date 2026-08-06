package com.buchou.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.buchou.app.MainActivity
import java.time.LocalDate
import java.time.ZonedDateTime

class AlarmScheduler(
    context: Context,
    private val preferences: ReminderPreferences,
) {
    private val applicationContext = context.applicationContext
    private val alarmManager = applicationContext.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun scheduleNext(earliestDate: LocalDate = LocalDate.now()) {
        cancel()
        val settings = preferences.current()
        if (!settings.enabled || !canScheduleExactAlarms()) return
        val next = NextAlarmCalculator.next(
            now = ZonedDateTime.now(),
            settings = settings,
            earliestDate = earliestDate,
        ) ?: return
        val showIntent = PendingIntent.getActivity(
            applicationContext,
            REQUEST_SHOW,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(next.toInstant().toEpochMilli(), showIntent),
            alarmPendingIntent(),
        )
    }

    fun skipToday() {
        scheduleNext(LocalDate.now().plusDays(1))
    }

    fun cancel() {
        alarmManager.cancel(alarmPendingIntent())
    }

    private fun alarmPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        applicationContext,
        REQUEST_ALARM,
        Intent(applicationContext, AlarmReceiver::class.java).setAction(ACTION_FIRE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val ACTION_FIRE = "com.buchou.app.action.FIRE_ALARM"
        const val REQUEST_ALARM = 4101
        const val REQUEST_SHOW = 4102
    }
}

