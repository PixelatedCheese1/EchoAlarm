package com.pixelatedcheese1.echoclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        val snooze = intent.getBooleanExtra("snooze", false)
        val list = AlarmStore.load(ctx)
        val a = list.firstOrNull { it.id == id } ?: return
        if (!snooze) {
            if (a.days == 0) AlarmStore.save(ctx, list.map { if (it.id == id) it.copy(enabled = false) else it })
            else AlarmScheduler.schedule(ctx, a)   // repeating: queue the next occurrence
        }
        ContextCompat.startForegroundService(ctx, Intent(ctx, AlarmService::class.java).putExtra("id", id))
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) AlarmScheduler.rescheduleAll(ctx)
    }
}
