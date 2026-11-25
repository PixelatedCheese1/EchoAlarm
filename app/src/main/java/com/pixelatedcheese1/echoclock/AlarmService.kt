package com.pixelatedcheese1.echoclock

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class AlarmService : Service() {
    companion object {
        @Volatile var running = false
        const val ACTION_DISMISS = "echoclock.DISMISS"
        const val ACTION_SNOOZE = "echoclock.SNOOZE"
        private const val CH = "alarm_ring"
        private const val NID = 42
        private const val MAX_RING_MS = 5 * 60_000L
    }

    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var alarmId = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getIntExtra("id", alarmId) ?: alarmId
        when (intent?.action) {
            ACTION_DISMISS -> { stopAll(); return START_NOT_STICKY }
            ACTION_SNOOZE -> { AlarmScheduler.snooze(this, id); stopAll(); return START_NOT_STICKY }
        }
        alarmId = id
        val alarm = AlarmStore.load(this).firstOrNull { it.id == id }

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "Alarms", NotificationManager.IMPORTANCE_HIGH))
        val full = PendingIntent.getActivity(
            this, 0, Intent(this, AlarmRingActivity::class.java).putExtra("id", id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        fun action(a: String, code: Int) = PendingIntent.getService(
            this, code, Intent(this, AlarmService::class.java).setAction(a).putExtra("id", id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(this, CH)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(alarm?.label?.ifBlank { null } ?: "Alarm")
            .setContentText("%02d:%02d".format(alarm?.hour ?: 0, alarm?.minute ?: 0))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(full, true)
            .setContentIntent(full)
            .setOngoing(true)
            .addAction(0, "Snooze", action(ACTION_SNOOZE, 1))
            .addAction(0, "Dismiss", action(ACTION_DISMISS, 2))
            .build()
        ServiceCompat.startForeground(
            this, NID, n,
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        )

        player?.release()
        player = try { Sounds.player(this, alarm?.sound.orEmpty(), true) } catch (e: Exception) { null }
        running = true
        handler.postDelayed({ stopAll() }, MAX_RING_MS)
        return START_NOT_STICKY
    }

    private fun stopAll() {
        handler.removeCallbacksAndMessages(null)
        try { player?.release() } catch (_: Exception) {}
        player = null
        running = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() { running = false; player?.release(); super.onDestroy() }
}
