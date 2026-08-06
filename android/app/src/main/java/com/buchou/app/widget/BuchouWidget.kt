package com.buchou.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.buchou.app.BuchouApplication
import com.buchou.app.MainActivity
import com.buchou.app.R
import com.buchou.app.domain.model.DailyStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

private object WidgetTokens {
    val surface = ColorProvider(Color(0xFFFCFBF8))
    val time = ColorProvider(Color(0xFF3F6B50))
    val smokeFree = time
    val smoked = ColorProvider(Color(0xFFA24C43))
    val unrecorded = ColorProvider(Color(0xFF929792))
    val cardRadius = 24.dp
}

private data class WidgetData(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val statusText: String,
    val statusColor: ColorProvider,
)

private suspend fun loadWidgetData(context: Context): WidgetData {
    val app = context.applicationContext as BuchouApplication
    val data = app.repository.widgetSnapshot()
    val duration = data.currentStreakStartedAtEpochMillis()?.let { startedAt ->
        Duration.between(Instant.ofEpochMilli(startedAt), Instant.now())
    } ?: Duration.ZERO

    val status = when (data.statusForDate(LocalDate.now())) {
        DailyStatus.SMOKE_FREE -> context.getString(R.string.widget_status_smoke_free) to WidgetTokens.smokeFree
        DailyStatus.SMOKED -> context.getString(R.string.widget_status_smoked) to WidgetTokens.smoked
        DailyStatus.UNRECORDED -> context.getString(R.string.widget_status_not_checked_in) to WidgetTokens.unrecorded
    }

    return WidgetData(
        days = duration.toDays().coerceAtLeast(0),
        hours = (duration.toHours() % 24).coerceAtLeast(0),
        minutes = (duration.toMinutes() % 60).coerceAtLeast(0),
        statusText = status.first,
        statusColor = status.second,
    )
}

private fun GlanceModifier.widgetCard() = fillMaxSize()
    .background(WidgetTokens.surface)
    .cornerRadius(WidgetTokens.cardRadius)
    .clickable(actionStartActivity<MainActivity>())

private fun brandStyle(size: Int) = TextStyle(
    fontSize = size.sp,
    fontStyle = FontStyle.Italic,
    fontFamily = FontFamily.Serif,
    color = WidgetTokens.time,
)

private fun timeNumberStyle(size: Int) = TextStyle(
    fontSize = size.sp,
    fontFamily = FontFamily.Serif,
    color = WidgetTokens.time,
)

private fun timeUnitStyle(size: Int) = TextStyle(
    fontSize = size.sp,
    fontFamily = FontFamily.Serif,
    color = WidgetTokens.time,
)

@Composable
private fun DayMetric(
    data: WidgetData,
    numberSize: Int,
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(data.days.toString(), style = timeNumberStyle(numberSize))
        Spacer(GlanceModifier.width(2.dp))
        Text("d", style = timeUnitStyle((numberSize * 0.42f).toInt()))
    }
}

@Composable
private fun ClockMetric(
    data: WidgetData,
    numberSize: Int,
) {
    val unitSize = (numberSize * 0.58f).toInt()
    Row(verticalAlignment = Alignment.Bottom) {
        Text(data.hours.toString(), style = timeNumberStyle(numberSize))
        Spacer(GlanceModifier.width(1.dp))
        Text("h", style = timeUnitStyle(unitSize))
        Spacer(GlanceModifier.width(5.dp))
        Text(data.minutes.toString(), style = timeNumberStyle(numberSize))
        Spacer(GlanceModifier.width(1.dp))
        Text("m", style = timeUnitStyle(unitSize))
    }
}

@Composable
private fun StatusText(data: WidgetData, size: Int) {
    Text(
        text = data.statusText,
        style = TextStyle(
            fontSize = size.sp,
            fontWeight = FontWeight.Medium,
            color = data.statusColor,
        ),
    )
}

class BuchouWidgetWide : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent {
            Column(
                modifier = GlanceModifier.widgetCard().padding(18.dp),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "buchou", style = brandStyle(20))
                Spacer(GlanceModifier.height(8.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        DayMetric(data, numberSize = 40)
                        Spacer(GlanceModifier.width(8.dp))
                        ClockMetric(data, numberSize = 18)
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    StatusText(data, size = 18)
                }
            }
        }
    }
}

class BuchouWidgetCompact : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent {
            Row(
                modifier = GlanceModifier.widgetCard().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "buchou", style = brandStyle(14))
                Spacer(GlanceModifier.width(9.dp))
                DayMetric(data, numberSize = 28)
                Spacer(GlanceModifier.width(6.dp))
                ClockMetric(data, numberSize = 12)
                Spacer(GlanceModifier.defaultWeight())
                StatusText(data, size = 12)
            }
        }
    }
}

class BuchouWidgetTall : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent {
            Column(
                modifier = GlanceModifier.widgetCard().padding(18.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(text = "buchou", style = brandStyle(26))
                Spacer(GlanceModifier.height(20.dp))
                DayMetric(data, numberSize = 50)
                Spacer(GlanceModifier.height(4.dp))
                ClockMetric(data, numberSize = 18)
                Spacer(GlanceModifier.defaultWeight())
                StatusText(data, size = 16)
            }
        }
    }
}

class BuchouWidgetWideReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = BuchouWidgetWide()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetTickReceiver.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetTickReceiver.cancelIfUnused(context)
    }
}

class BuchouWidgetCompactReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = BuchouWidgetCompact()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetTickReceiver.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetTickReceiver.cancelIfUnused(context)
    }
}

class BuchouWidgetTallReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = BuchouWidgetTall()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetTickReceiver.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetTickReceiver.cancelIfUnused(context)
    }
}

object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        BuchouWidgetWide().updateAll(context)
        BuchouWidgetCompact().updateAll(context)
        BuchouWidgetTall().updateAll(context)
    }
}

class WidgetTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_WIDGET_TICK) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                WidgetUpdater.updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val ACTION_WIDGET_TICK = "com.buchou.app.WIDGET_TICK"
        private const val WIDGET_UPDATE_REQUEST_CODE = 10001
        private const val INTERVAL_ONE_MINUTE = 60_000L

        fun schedule(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + INTERVAL_ONE_MINUTE,
                INTERVAL_ONE_MINUTE,
                pendingIntent(context),
            )
        }

        fun cancelIfUnused(context: Context) {
            if (hasAnyWidget(context)) return
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent(context))
        }

        private fun hasAnyWidget(context: Context): Boolean {
            val manager = android.appwidget.AppWidgetManager.getInstance(context)
            val providers = listOf(
                BuchouWidgetWideReceiver::class.java,
                BuchouWidgetCompactReceiver::class.java,
                BuchouWidgetTallReceiver::class.java,
            )
            return providers.any { receiver ->
                manager.getAppWidgetIds(ComponentName(context, receiver)).isNotEmpty()
            }
        }

        private fun pendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, WidgetTickReceiver::class.java).apply {
                action = ACTION_WIDGET_TICK
            }
            return PendingIntent.getBroadcast(
                context,
                WIDGET_UPDATE_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
