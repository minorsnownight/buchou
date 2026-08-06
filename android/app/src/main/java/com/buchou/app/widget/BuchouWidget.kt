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
import androidx.glance.layout.Box
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

private object WidgetTokens {
    val surface = ColorProvider(Color(0xFFFCFBF8))
    val primary = ColorProvider(Color(0xFF1C211E))
    val secondary = ColorProvider(Color(0xFF69706B))
    val smokeFree = ColorProvider(Color(0xFF3F6B50))
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
    val data = app.repository.data.first()
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
    color = WidgetTokens.secondary,
)

private fun primaryStyle(size: Int) = TextStyle(
    fontSize = size.sp,
    fontWeight = FontWeight.Bold,
    color = WidgetTokens.primary,
)

private fun secondaryStyle(size: Int) = TextStyle(
    fontSize = size.sp,
    color = WidgetTokens.secondary,
)

private fun timeText(context: Context, data: WidgetData) = "${data.hours}${context.getString(R.string.hour_unit_short)} ${data.minutes}${context.getString(R.string.minute_unit_short)}"

@Composable
private fun TimeDisplay(
    context: Context,
    data: WidgetData,
    daySize: Int,
    timeSize: Int,
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(data.days.toString(), style = primaryStyle(daySize))
        Text(context.getString(R.string.day_unit_short), style = secondaryStyle((daySize * 0.46f).toInt()))
        Spacer(GlanceModifier.width(7.dp))
        Text(timeText(context, data), style = secondaryStyle(timeSize))
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
                modifier = GlanceModifier.widgetCard().padding(20.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(text = "buchou", style = brandStyle(17))
                Spacer(GlanceModifier.height(12.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    TimeDisplay(context, data, daySize = 36, timeSize = 18)
                    Spacer(GlanceModifier.defaultWeight())
                    StatusText(data, size = 15)
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
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(text = "buchou", style = brandStyle(13))
                Spacer(GlanceModifier.width(10.dp))
                TimeDisplay(context, data, daySize = 25, timeSize = 11)
                Spacer(GlanceModifier.defaultWeight())
                StatusText(data, size = 10)
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
                Text(text = "buchou", style = brandStyle(21))
                Spacer(GlanceModifier.height(30.dp))
                TimeDisplay(context, data, daySize = 48, timeSize = 18)
                Spacer(GlanceModifier.defaultWeight())
                StatusText(data, size = 15)
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
