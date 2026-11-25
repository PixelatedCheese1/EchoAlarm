package com.pixelatedcheese1.echoclock

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AppSettings(ctx: Context) {
    private val sp = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var faceId by mutableStateOf(sp.getString("face", "big")!!); private set
    var use24h by mutableStateOf(sp.getBoolean("h24", false)); private set
    var showSeconds by mutableStateOf(sp.getBoolean("secs", false)); private set
    var showDate by mutableStateOf(sp.getBoolean("date", true)); private set
    var keepAwake by mutableStateOf(sp.getBoolean("awake", true)); private set
    var dark by mutableStateOf(sp.getBoolean("dark", true)); private set
    var wallpaper by mutableStateOf(sp.getString("wp", "")!!); private set
    var night by mutableStateOf(sp.getBoolean("night", false)); private set
    var mediaPkg by mutableStateOf(sp.getString("media_pkg", "")!!); private set

    fun updateFace(v: String) { faceId = v; sp.edit().putString("face", v).apply() }
    fun updateUse24h(v: Boolean) { use24h = v; sp.edit().putBoolean("h24", v).apply() }
    fun updateShowSeconds(v: Boolean) { showSeconds = v; sp.edit().putBoolean("secs", v).apply() }
    fun updateShowDate(v: Boolean) { showDate = v; sp.edit().putBoolean("date", v).apply() }
    fun updateKeepAwake(v: Boolean) { keepAwake = v; sp.edit().putBoolean("awake", v).apply() }
    fun updateDark(v: Boolean) { dark = v; sp.edit().putBoolean("dark", v).apply() }
    fun updateMediaPkg(v: String) { mediaPkg = v; sp.edit().putString("media_pkg", v).apply() }
    fun updateWallpaper(v: String) { wallpaper = v; sp.edit().putString("wp", v).apply() }
    fun updateNight(v: Boolean) { night = v; sp.edit().putBoolean("night", v).apply() }
}
