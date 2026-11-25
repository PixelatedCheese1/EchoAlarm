package com.pixelatedcheese1.echoclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object AlarmScheduler {
    private fun pending(ctx: Context, id: Int, snooze: Boolean = false): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java).putExtra("id", id).putExtra("snooze", snooze)
        return PendingIntent.getBroadcast(
            ctx, if (snooze) id + 100000 else id, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setAt(ctx: Context, at: Long, pi: PendingIntent) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val show = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        try { am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi) }
        catch (e: SecurityException) { am.set(AlarmManager.RTC_WAKEUP, at, pi) }
    }

    fun schedule(ctx: Context, a: Alarm) {
        cancel(ctx, a.id)
        if (!a.enabled) return
        setAt(ctx, AlarmStore.nextTrigger(a).toInstant().toEpochMilli(), pending(ctx, a.id))
    }

    fun cancel(ctx: Context, id: Int) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pending(ctx, id))
    }

    fun snooze(ctx: Context, id: Int, minutes: Int = 10) {
        setAt(ctx, System.currentTimeMillis() + minutes * 60_000L, pending(ctx, id, true))
    }

    fun rescheduleAll(ctx: Context) {
        AlarmStore.load(ctx).forEach { if (it.enabled) schedule(ctx, it) else cancel(ctx, it.id) }
    }
}
