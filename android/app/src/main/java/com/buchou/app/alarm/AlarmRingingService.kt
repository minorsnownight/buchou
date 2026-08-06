package com.buchou.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.buchou.app.AlarmActivity
import com.buchou.app.BuchouApplication
import com.buchou.app.R

class AlarmRingingService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        startForeground(NOTIFICATION_ID, createNotification())
        startSound()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        wakeLock?.takeIf(PowerManager.WakeLock::isHeld)?.release()
        wakeLock = null
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            REQUEST_FULL_SCREEN,
            Intent(this, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle(getString(R.string.alarm_notification_title))
            .setContentText(getString(R.string.alarm_notification_text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.alarm_channel_description)
            setSound(null, null)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun startSound() {
        val application = applicationContext as BuchouApplication
        val configuredUri = application.reminderPreferences.current().soundUri?.let(Uri::parse)
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        mediaPlayer = createPlayer(configuredUri)
            ?: createPlayer(defaultUri)
            ?: return
        mediaPlayer?.start()
    }

    private fun createPlayer(uri: Uri?): MediaPlayer? {
        if (uri == null) return null
        val player = MediaPlayer()
        return try {
            player.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(this@AlarmRingingService, uri)
                isLooping = true
                prepare()
            }
        } catch (_: Exception) {
            player.release()
            null
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "buchou:alarm",
        ).apply { acquire(MAX_WAKE_LOCK_MILLIS) }
    }

    companion object {
        const val ACTION_STOP = "com.buchou.app.action.STOP_ALARM"
        private const val CHANNEL_ID = "check_in_alarm"
        private const val NOTIFICATION_ID = 4201
        private const val REQUEST_FULL_SCREEN = 4202
        private const val MAX_WAKE_LOCK_MILLIS = 30 * 60 * 1000L

        fun stop(context: Context) {
            context.startService(
                Intent(context, AlarmRingingService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
