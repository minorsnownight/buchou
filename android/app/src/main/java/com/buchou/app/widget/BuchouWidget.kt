package com.buchou.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.buchou.app.BuchouApplication
import com.buchou.app.MainActivity
import com.buchou.app.R

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

// ── Shared data ──────────────────────────────────────────────

private data class WidgetData(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val statusText: String,
    val statusColorRes: Int,
)

private suspend fun loadWidgetData(context: Context): WidgetData {
    val app = context.applicationContext as BuchouApplication
    val data = app.repository.data.first()
    val todayStatus = data.statusForDate(LocalDate.now())
    val streakStart = data.currentStreakStartedAtEpochMillis()
    val now = Instant.now()
    val duration = streakStart?.let {
        Duration.between(Instant.ofEpochMilli(it), now)
    } ?: Duration.ZERO

    val days = duration.toDays().coerceAtLeast(0)
    val hours = (duration.toHours() % 24).coerceAtLeast(0)
    val minutes = (duration.toMinutes() % 60).coerceAtLeast(0)

    val (statusText, statusColorRes) = when (todayStatus) {
        com.buchou.app.domain.model.DailyStatus.SMOKE_FREE ->
            context.getString(R.string.btn_smoke_free) to R.color.widget_smoke_free
        com.buchou.app.domain.model.DailyStatus.SMOKED ->
            context.getString(R.string.btn_smoked) to R.color.widget_smoked
        com.buchou.app.domain.model.DailyStatus.UNRECORDED ->
            context.getString(R.string.not_checked_in) to R.color.widget_unrecorded
    }

    return WidgetData(days, hours, minutes, statusText, statusColorRes)
}

private fun buildTimeString(context: Context, hours: Long, minutes: Long): String {
    val hourUnit = context.getString(R.string.hour_unit_short)
    val minuteUnit = context.getString(R.string.minute_unit_short)
    return "$hours$hourUnit$minutes$minuteUnit"
}

// ── 4×2 Wide Widget ───────────────────────────────────────────

class BuchouWidgetWide : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val wd = loadWidgetData(context)
        val dayUnit = context.getString(R.string.day_unit_short)
        val timeStr = buildTimeString(context, wd.hours, wd.minutes)
        val statusColor = ColorProvider(wd.statusColorRes)

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(20.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            text = context.getString(R.string.app_name),
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = GlanceTheme.colors.onSurfaceVariant,
                            ),
                        )
                        Spacer(GlanceModifier.width(12.dp))
                        Text(
                            text = wd.days.toString(),
                            style = TextStyle(
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface,
                            ),
                        )
                        Text(
                            text = dayUnit,
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = GlanceTheme.colors.onSurfaceVariant,
                            ),
                        )
                        Spacer(GlanceModifier.width(6.dp))
                        Text(
                            text = timeStr,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = GlanceTheme.colors.onSurfaceVariant,
                            ),
                        )
                        Spacer(GlanceModifier.width(12.dp))
                        Text(
                            text = wd.statusText,
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = statusColor,
                            ),
                        )
                    }
                }
            }
        }
    }
}

// ── 4×1 Compact Widget ──────────────────────────────────────

class BuchouWidgetCompact : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val wd = loadWidgetData(context)
        val dayUnit = context.getString(R.string.day_unit_short)
        val hourUnit = context.getString(R.string.hour_unit_short)
        val minuteUnit = context.getString(R.string.minute_unit_short)
        val statusColor = ColorProvider(wd.statusColorRes)

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(20.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = context.getString(R.string.app_name),
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = GlanceTheme.colors.onSurfaceVariant,
                            ),
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        Text(
                            text = "${wd.days}$dayUnit",
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface,
                            ),
                        )
                        Spacer(GlanceModifier.width(4.dp))
                        Text(
                            text = "${wd.hours}$hourUnit${wd.minutes}$minuteUnit",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = GlanceTheme.colors.onSurfaceVariant,
                            ),
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        Text(
                            text = wd.statusText,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = statusColor,
                            ),
                        )
                    }
                }
            }
        }
    }
}

// ── 2×4 Tall Widget ──────────────────────────────────────────

class BuchouWidgetTall : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val wd = loadWidgetData(context)
        val dayUnit = context.getString(R.string.day_unit_short)
        val hourUnit = context.getString(R.string.hour_unit_short)
        val minuteUnit = context.getString(R.string.minute_unit_short)
        val statusColor = ColorProvider(wd.statusColorRes)

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(20.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = context.getString(R.string.app_name),
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = GlanceTheme.colors.onSurfaceVariant,
                            ),
                        )
                        Spacer(GlanceModifier.height(16.dp))
                        Text(
                            text = wd.days.toString(),
                            style = TextStyle(
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface,
                            ),
                        )
                        Text(
                            text = dayUnit,
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = GlanceTheme.colors.onSurfaceVariant,
                            ),
                        )
                        Spacer(GlanceModifier.height(12.dp))
                        Text(
                            text = "${wd.hours}$hourUnit",
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = GlanceTheme.colors.onSurfaceVariant,
                            ),
                        )
                        Spacer(GlanceModifier.height(2.dp))
                        Text(
                            text = "${wd.minutes}$minuteUnit",
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = GlanceTheme.colors.onSurfaceVariant,
                            ),
                        )
                        Spacer(GlanceModifier.height(16.dp))
                        Text(
                            text = wd.statusText,
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = statusColor,
                            ),
                        )
                    }
                }
            }
        }
    }
}

// ── Receivers ────────────────────────────────────────────────

class BuchouWidgetWideReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = BuchouWidgetWide()
}

class BuchouWidgetCompactReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = BuchouWidgetCompact()
}

class BuchouWidgetTallReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = BuchouWidgetTall()
}

// ── Unified updater + periodic refresh ───────────────────────

object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        BuchouWidgetWide().updateAll(context)
        BuchouWidgetCompact().updateAll(context)
        BuchouWidgetTall().updateAll(context)
    }
}

class WidgetTickReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = BuchouWidgetWide() // unused but required

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleWidgetUpdates(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelWidgetUpdates(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_TICK) {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    WidgetUpdater.updateAll(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val ACTION_WIDGET_TICK = "com.buchou.app.WIDGET_TICK"
        private const val WIDGET_UPDATE_REQUEST_CODE = 10001
        private const val INTERVAL_ONE_MINUTE = 60_000L

        fun scheduleWidgetUpdates(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WidgetTickReceiver::class.java).apply {
                action = ACTION_WIDGET_TICK
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                WIDGET_UPDATE_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + INTERVAL_ONE_MINUTE,
                INTERVAL_ONE_MINUTE,
                pendingIntent,
            )
        }

        fun cancelWidgetUpdates(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WidgetTickReceiver::class.java).apply {
                action = ACTION_WIDGET_TICK
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                WIDGET_UPDATE_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
