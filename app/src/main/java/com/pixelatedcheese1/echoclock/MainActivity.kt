package com.pixelatedcheese1.echoclock

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

enum class Screen(val label: String, val icon: ImageVector) {
    Clock("Clock", Icons.Default.Schedule),
    Alarms("Alarms", Icons.Default.Alarm),
    Media("Media", Icons.Default.MusicNote)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideBars()
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        val settings = AppSettings(this)
        AlarmScheduler.rescheduleAll(this)
        setContent { EchoClockTheme(settings.dark) { App(settings) } }
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

    DisposableEffect(settings.keepAwake) {
        if (settings.keepAwake) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Spacer(Modifier.weight(1f))
                Screen.values().forEach { s ->
                    NavigationRailItem(selected = screen == s, onClick = { screen = s },
                        icon = { Icon(s.icon, s.label) }, label = { Text(s.label) })
                }
                NavigationRailItem(selected = false, onClick = { showSettings = true },
                    icon = { Icon(Icons.Default.Settings, "Clock settings") }, label = { Text("Clock") })
                Spacer(Modifier.weight(1f))
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when (screen) {
                    Screen.Clock -> ClockScreen(settings) { showSettings = true }
                    Screen.Alarms -> AlarmsScreen(settings)
                    Screen.Media -> MediaScreen(settings)
                }
            }
        }
    }
    if (showSettings) ClockSettingsSheet(settings) { showSettings = false }
}
