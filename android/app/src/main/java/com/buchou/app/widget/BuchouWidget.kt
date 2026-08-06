package com.buchou.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
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

private data class WidgetData(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val statusText: String,
    val statusColor: Int,
)

private suspend fun loadWidgetData(context: Context): WidgetData {
    val app = context.applicationContext as BuchouApplication
    val data = app.repository.widgetSnapshot()
    val duration = data.currentStreakStartedAtEpochMillis()?.let { startedAt ->
        Duration.between(Instant.ofEpochMilli(startedAt), Instant.now())
    } ?: Duration.ZERO

    val res = context.resources
    val (statusText, statusColor) = when (data.statusForDate(LocalDate.now())) {
        DailyStatus.SMOKE_FREE -> res.getString(R.string.widget_status_smoke_free) to ContextCompat.getColor(context, R.color.widget_smoke_free)
        DailyStatus.SMOKED -> res.getString(R.string.widget_status_smoked) to ContextCompat.getColor(context, R.color.widget_smoked)
        DailyStatus.UNRECORDED -> res.getString(R.string.widget_status_not_checked_in) to ContextCompat.getColor(context, R.color.widget_unrecorded)
    }

    return WidgetData(
        days = duration.toDays().coerceAtLeast(0),
        hours = (duration.toHours() % 24).coerceAtLeast(0),
        minutes = (duration.toMinutes() % 60).coerceAtLeast(0),
        statusText = statusText,
        statusColor = statusColor,
    )
}

/**
 * 推送一个 widget 实例的 RemoteViews。同步执行，不经过 JobScheduler/WorkManager，
 * 前台调用立即生效，不受厂商后台冻结影响。
 */
private suspend fun pushWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    layoutRes: Int,
) {
    val data = loadWidgetData(context)
    android.util.Log.i("BuchouWidget", "pushWidget id=$appWidgetId days=${data.days} status=${data.statusText}")
    val views = RemoteViews(context.packageName, layoutRes).apply {
        setTextViewText(R.id.widget_days, data.days.toString())
        setTextViewText(R.id.widget_time, "${data.hours}h ${data.minutes}m")
        setTextViewText(R.id.widget_status, data.statusText)
        setTextColor(R.id.widget_status, data.statusColor)
        setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(
                context,
                appWidgetId,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
    }
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

abstract class BuchouWidgetProvider(private val layoutRes: Int) : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        android.util.Log.i("BuchouWidget", "onUpdate ${javaClass.simpleName} ids=${appWidgetIds.toList()} layoutRes=$layoutRes")
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                appWidgetIds.forEach { id ->
                    pushWidget(context, appWidgetManager, id, layoutRes)
                }
            } catch (t: Throwable) {
                android.util.Log.e("BuchouWidget", "onUpdate failed", t)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class BuchouWidgetWideReceiver : BuchouWidgetProvider(R.layout.buchou_widget_preview_wide)
class BuchouWidgetCompactReceiver : BuchouWidgetProvider(R.layout.buchou_widget_preview_compact)
class BuchouWidgetTallReceiver : BuchouWidgetProvider(R.layout.buchou_widget_preview_tall)

object WidgetUpdater {
    private val providers = listOf(
        BuchouWidgetWideReceiver::class.java,
        BuchouWidgetCompactReceiver::class.java,
        BuchouWidgetTallReceiver::class.java,
    )

    /**
     * 同步刷新所有已添加的 widget 实例。供数据变化流、onResume 等前台路径调用。
     */
    suspend fun updateAll(context: Context) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        for (provider in providers) {
            val ids = manager.getAppWidgetIds(ComponentName(app, provider))
            val layoutRes = when (provider) {
                BuchouWidgetWideReceiver::class.java -> R.layout.buchou_widget_preview_wide
                BuchouWidgetCompactReceiver::class.java -> R.layout.buchou_widget_preview_compact
                else -> R.layout.buchou_widget_preview_tall
            }
            ids.forEach { id -> pushWidget(app, manager, id, layoutRes) }
        }
    }
}
