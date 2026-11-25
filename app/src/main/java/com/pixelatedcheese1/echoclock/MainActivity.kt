package com.pixelatedcheese1.echoclock

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

enum class Screen(val label: String, val icon: ImageVector) {
    Clock("Clock", Icons.Default.Schedule),
    Alarms("Alarms", Icons.Default.Alarm),
    Media("Media", Icons.Default.MusicNote),
    Apps("Apps", Icons.Default.Apps)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideBars()
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        val settings = AppSettings(this)
        AlarmScheduler.rescheduleAll(this)
        setContent { EchoClockTheme(settings.dark, settings.night) { App(settings) } }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideBars()
    }

    private fun hideBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
fun App(settings: AppSettings) {
    val activity = LocalContext.current as Activity
    var screen by remember { mutableStateOf(Screen.Clock) }
    var showSettings by remember { mutableStateOf(false) }
    var showWallpapers by remember { mutableStateOf(false) }

    DisposableEffect(settings.keepAwake) {
        if (settings.keepAwake) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { }
    }

    // Night mode: very low window brightness. Off = back to the system brightness.
    DisposableEffect(settings.night) {
        val lp = activity.window.attributes
        lp.screenBrightness = if (settings.night) 0.01f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        activity.window.attributes = lp
        onDispose { }
    }

    fun setNight(on: Boolean) {
        settings.updateNight(on)
        settings.updateDark(on)   // on -> dark theme, off -> light theme
    }

    Box(Modifier.fillMaxSize()) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.width(80.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(
                        Modifier.align(Alignment.Center).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Screen.values().forEach { s ->
                            NavigationRailItem(selected = screen == s, onClick = { screen = s },
                                icon = { Icon(s.icon, s.label) }, label = { Text(s.label) })
                        }
                        NavigationRailItem(selected = settings.night, onClick = { setNight(!settings.night) },
                            icon = { Icon(Icons.Default.Bedtime, "Night mode") }, label = { Text("Night") })
                        NavigationRailItem(selected = false, onClick = { showSettings = true },
                            icon = { Icon(Icons.Default.Palette, "Clock style") }, label = { Text("Style") })
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (screen) {
                        Screen.Clock -> ClockScreen(settings) { showSettings = true }
                        Screen.Alarms -> AlarmsScreen(settings)
                        Screen.Media -> MediaScreen(settings)
                        Screen.Apps -> AppDrawerScreen()
                    }
                }
            }
        }
        if (showWallpapers) WallpaperPicker(settings) { showWallpapers = false }
    }

    if (showSettings) ClockSettingsSheet(
        settings,
        onNightChange = { setNight(it) },
        onWallpapers = { showSettings = false; screen = Screen.Clock; showWallpapers = true },
        onDismiss = { showSettings = false }
    )
}
