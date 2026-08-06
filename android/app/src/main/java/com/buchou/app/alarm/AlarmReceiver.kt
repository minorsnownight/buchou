package com.buchou.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.buchou.app.BuchouApplication
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val application = context.applicationContext as BuchouApplication
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val today = LocalDate.now()
                val data = application.repository.data.first()
                val alreadyCheckedIn = hasRecordForDate(
                    checkInDates = data.checkIns.map { it.localDate },
                    smokingDates = data.smokingEvents.map { it.localDate },
                    date = today.toString(),
                )
                application.alarmScheduler.skipToday()
                if (!alreadyCheckedIn) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, AlarmRingingService::class.java),
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

internal fun hasRecordForDate(
    checkInDates: Iterable<String>,
    smokingDates: Iterable<String>,
    date: String,
): Boolean = checkInDates.any { it == date } || smokingDates.any { it == date }
