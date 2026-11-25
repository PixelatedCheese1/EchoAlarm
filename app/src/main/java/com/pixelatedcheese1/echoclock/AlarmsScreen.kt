package com.pixelatedcheese1.echoclock

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")
private val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun daysSummary(mask: Int) = when (mask) {
    0 -> "Once"
    127 -> "Every day"
    31 -> "Weekdays"
    96 -> "Weekends"
    else -> DAY_NAMES.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.joinToString(" ")
}

private fun timeText(h: Int, m: Int, use24h: Boolean): String =
    if (use24h) "%02d:%02d".format(h, m)
    else "%d:%02d %s".format(if (h % 12 == 0) 12 else h % 12, m, if (h < 12) "AM" else "PM")

@Composable
fun AlarmsScreen(settings: AppSettings) {
    val ctx = LocalContext.current
    var alarms by remember { mutableStateOf(AlarmStore.load(ctx)) }
    var editing by remember { mutableStateOf<Alarm?>(null) }

    fun commit(list: List<Alarm>, changed: Alarm? = null, removed: Int? = null) {
        alarms = list
        AlarmStore.save(ctx, list)
        removed?.let { AlarmScheduler.cancel(ctx, it) }
        changed?.let { AlarmScheduler.schedule(ctx, it) }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Alarms", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = {
                editing = Alarm(AlarmStore.nextId(alarms), 7, 0, sound = Sounds.names().firstOrNull() ?: "")
            }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("New alarm") }
        }
        Spacer(Modifier.height(8.dp))
        if (alarms.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No alarms yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(alarms, key = { it.id }) { a ->
                ElevatedCard(Modifier.fillMaxWidth().clickable { editing = a }) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(timeText(a.hour, a.minute, settings.use24h), fontSize = 34.sp, fontWeight = FontWeight.Medium,
                            color = if (a.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.width(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.label.ifBlank { daysSummary(a.days) }, style = MaterialTheme.typography.titleMedium)
                            Text("${daysSummary(a.days)} · ${Sounds.pretty(a.sound)}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = a.enabled, onCheckedChange = { on ->
                            val upd = a.copy(enabled = on)
                            commit(alarms.map { if (it.id == a.id) upd else it }, upd)
                        })
                    }
                }
            }
        }
    }

    editing?.let { cur ->
        AlarmEditor(ctx, cur, settings.use24h, isNew = alarms.none { it.id == cur.id },
            onDismiss = { editing = null },
            onDelete = { commit(alarms.filter { it.id != cur.id }, removed = cur.id); editing = null },
            onSave = { saved ->
                val list = if (alarms.any { it.id == saved.id }) alarms.map { if (it.id == saved.id) saved else it }
                else alarms + saved
                commit(list.sortedWith(compareBy({ it.hour }, { it.minute })), saved)
                editing = null
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditor(
    ctx: Context, alarm: Alarm, use24h: Boolean, isNew: Boolean,
    onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (Alarm) -> Unit
) {
    val time = rememberTimePickerState(alarm.hour, alarm.minute, use24h)
    var days by remember { mutableIntStateOf(alarm.days) }
    var sound by remember { mutableStateOf(alarm.sound) }
    var label by remember { mutableStateOf(alarm.label) }
    var menu by remember { mutableStateOf(false) }
    var previewing by remember { mutableStateOf(false) }
    val sounds = remember { Sounds.names() }
    DisposableEffect(Unit) { onDispose { SoundPreview.stop() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.widthIn(max = 700.dp).padding(16.dp),
        title = { Text(if (isNew) "New alarm" else "Edit alarm") },
        text = {
            Row(Modifier.verticalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                TimeInput(time)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DAY_LETTERS.forEachIndexed { i, l ->
                            val on = days and (1 shl i) != 0
                            Box(
                                Modifier.size(36.dp).clip(CircleShape)
                                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { days = days xor (1 shl i) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(l, color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Text(daysSummary(days), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(label, { label = it }, label = { Text("Label") }, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(Sounds.pretty(sound), maxLines = 1)
                            }
                            DropdownMenu(menu, { menu = false }) {
                                DropdownMenuItem(text = { Text(Sounds.pretty("")) }, onClick = { sound = ""; menu = false })
                                sounds.forEach { n ->
                                    DropdownMenuItem(text = { Text(Sounds.pretty(n)) }, onClick = { sound = n; menu = false })
                                }
                            }
                        }
                        IconButton(onClick = {
                            if (previewing) { SoundPreview.stop(); previewing = false }
                            else { SoundPreview.play(ctx, sound); previewing = true }
                        }) { Icon(if (previewing) Icons.Default.Stop else Icons.Default.PlayArrow, "Preview") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(alarm.copy(hour = time.hour, minute = time.minute, days = days, sound = sound, label = label.trim(), enabled = true))
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = onDelete) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(4.dp)); Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
