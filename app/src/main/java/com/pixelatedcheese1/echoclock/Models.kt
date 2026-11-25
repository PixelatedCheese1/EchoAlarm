package com.pixelatedcheese1.echoclock

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZoneId
import java.time.ZonedDateTime

/** days: bitmask, bit0 = Monday ... bit6 = Sunday. 0 = one-shot (next occurrence). */
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val days: Int = 0,
    val enabled: Boolean = true,
    val sound: String = "",
    val label: String = ""
)

object AlarmStore {
    private const val KEY = "alarms"
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("alarms", Context.MODE_PRIVATE)

    fun load(ctx: Context): List<Alarm> {
        val arr = JSONArray(prefs(ctx).getString(KEY, "[]"))
        return List(arr.length()) {
            val o = arr.getJSONObject(it)
            Alarm(o.getInt("id"), o.getInt("h"), o.getInt("m"), o.getInt("d"),
                o.getBoolean("e"), o.getString("s"), o.optString("l", ""))
        }
    }

    fun save(ctx: Context, list: List<Alarm>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("h", it.hour).put("m", it.minute)
                .put("d", it.days).put("e", it.enabled).put("s", it.sound).put("l", it.label))
        }
        prefs(ctx).edit().putString(KEY, arr.toString()).apply()
    }

    fun nextId(list: List<Alarm>) = (list.maxOfOrNull { it.id } ?: 0) + 1

    fun nextTrigger(a: Alarm, from: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        val zone = ZoneId.systemDefault()
        for (i in 0..7) {
            val d = from.toLocalDate().plusDays(i.toLong())
            val dt = d.atTime(a.hour, a.minute).atZone(zone)
            val dayOk = a.days == 0 || (a.days and (1 shl (d.dayOfWeek.value - 1))) != 0
            if (dayOk && dt.isAfter(from)) return dt
        }
        return from.plusDays(1)
    }
}

/**
 * Alarm sounds = every file you drop into app/src/main/res/raw/ (mp3/ogg/wav).
 * They are discovered automatically at runtime, no code changes needed.
 * File names must be lowercase letters, digits and underscores, e.g. "morning_birds.mp3".
 */
object Sounds {
    fun names(): List<String> = try {
        Class.forName("com.pixelatedcheese1.echoclock.R\$raw").fields
            .map { it.name }.filter { !it.startsWith("$") }.sorted()
    } catch (e: Exception) { emptyList() }

    fun pretty(name: String): String =
        if (name.isEmpty()) "Default alarm" else name.replace('_', ' ').replaceFirstChar { it.uppercase() }

    fun uri(ctx: Context, name: String): Uri {
        val id = if (name.isEmpty()) 0 else ctx.resources.getIdentifier(name, "raw", ctx.packageName)
        return if (id != 0) Uri.parse("android.resource://${ctx.packageName}/$id")
        else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    }

    fun player(ctx: Context, name: String, loop: Boolean): MediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        )
        setDataSource(ctx, uri(ctx, name))
        isLooping = loop
        prepare()
        start()
    }
}

object SoundPreview {
    private var mp: MediaPlayer? = null
    fun play(ctx: Context, name: String) {
        stop()
        mp = try { Sounds.player(ctx, name, false) } catch (e: Exception) { null }
    }
    fun stop() { try { mp?.release() } catch (_: Exception) {}; mp = null }
}
