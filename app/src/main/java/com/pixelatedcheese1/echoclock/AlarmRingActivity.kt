package com.pixelatedcheese1.echoclock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AlarmRingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val id = intent.getIntExtra("id", -1)
        val alarm = AlarmStore.load(this).firstOrNull { it.id == id }
        val settings = AppSettings(this)

        fun send(action: String) {
            startService(Intent(this, AlarmService::class.java).setAction(action).putExtra("id", id))
            finish()
        }

        setContent {
            EchoClockTheme(settings.dark, settings.night) {
                var now by remember { mutableStateOf(LocalTime.now()) }
                LaunchedEffect(Unit) {
                    delay(1500)
                    while (AlarmService.running) { now = LocalTime.now(); delay(500) }
                    finish()
                }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(now.format(DateTimeFormatter.ofPattern(if (settings.use24h) "HH:mm" else "h:mm a")),
                            fontSize = 96.sp, color = MaterialTheme.colorScheme.primary)
                        Text(alarm?.label?.ifBlank { null } ?: "Alarm", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            FilledTonalButton({ send(AlarmService.ACTION_SNOOZE) }, Modifier.height(56.dp)) { Text("Snooze 10 min") }
                            Button({ send(AlarmService.ACTION_DISMISS) }, Modifier.height(56.dp)) { Text("Dismiss") }
                        }
                    }
                }
            }
        }
    }
}
