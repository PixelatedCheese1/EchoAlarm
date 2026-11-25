package com.pixelatedcheese1.echoclock

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay

private data class Snap(
    val title: String, val artist: String, val art: Bitmap?,
    val playing: Boolean, val pos: Long, val dur: Long
)

private data class AppEntry(val pkg: String, val label: String, val icon: android.graphics.drawable.Drawable)

private fun findController(ctx: Context, pkg: String): MediaController? {
    val msm = ctx.getSystemService(MediaSessionManager::class.java)
    val list = try {
        msm.getActiveSessions(ComponentName(ctx, MediaListener::class.java))
    } catch (e: SecurityException) { return null }
    return if (pkg.isEmpty()) list.firstOrNull() else list.firstOrNull { it.packageName == pkg }
}

private fun snapOf(c: MediaController): Snap {
    val md = c.metadata
    val ps = c.playbackState
    val playing = ps?.state == PlaybackState.STATE_PLAYING
    var pos = ps?.position ?: 0L
    if (playing && ps != null) pos += ((SystemClock.elapsedRealtime() - ps.lastPositionUpdateTime) * ps.playbackSpeed).toLong()
    return Snap(
        title = md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown title",
        artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "",
        art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON),
        playing = playing, pos = pos, dur = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
    )
}

private fun appLabel(ctx: Context, pkg: String): String = try {
    val pm = ctx.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
} catch (e: Exception) { "Any app" }

private fun launchApp(ctx: Context, pkg: String) {
    ctx.packageManager.getLaunchIntentForPackage(pkg)?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(it)
    }
}

private fun sendPlayKey(ctx: Context) {
    val am = ctx.getSystemService(AudioManager::class.java)
    am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
    am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
}

private fun mmss(ms: Long): String { val s = (ms / 1000).coerceAtLeast(0); return "%d:%02d".format(s / 60, s % 60) }

@Composable
fun MediaScreen(settings: AppSettings) {
    val ctx = LocalContext.current
    val pkg = settings.mediaPkg
    var listenerOk by remember { mutableStateOf(false) }
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var snap by remember { mutableStateOf<Snap?>(null) }
    var picking by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(pkg) {
        while (true) {
            listenerOk = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
            val c = if (listenerOk) findController(ctx, pkg) else null
            controller = c
            snap = c?.let { snapOf(it) }
            delay(500)
        }
    }

    Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
        val art = remember(snap?.art) { snap?.art?.asImageBitmap() }
        Box(Modifier.size(190.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(Icons.Default.MusicNote, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { picking = true }, label = { Text(if (pkg.isEmpty()) "Source: any app" else appLabel(ctx, pkg)) },
                    leadingIcon = { Icon(Icons.Default.Apps, null) })
                if (pkg.isNotEmpty()) AssistChip(onClick = { launchApp(ctx, pkg) }, label = { Text("Open app") },
                    leadingIcon = { Icon(Icons.Default.OpenInNew, null) })
            }
            val s = snap
            when {
                !listenerOk -> {
                    Text("Allow Echo Clock to see what's playing", style = MaterialTheme.typography.titleMedium)
                    Text("Media controls need notification access (Android only lets apps read media sessions that way).",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) {
                        Text("Grant access")
                    }
                }
                s == null -> {
                    Text("Nothing playing", style = MaterialTheme.typography.headlineSmall)
                    Text(if (pkg.isEmpty()) "Pick an app, open it and start a song." else "Open ${appLabel(ctx, pkg)} and start a song, or try Play.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (pkg.isNotEmpty()) FilledTonalButton(onClick = { launchApp(ctx, pkg) }) { Text("Open ${appLabel(ctx, pkg)}") }
                        OutlinedButton(onClick = { sendPlayKey(ctx) }) { Icon(Icons.Default.PlayArrow, null); Text("Play") }
                    }
                }
                else -> {
                    Text(s.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
                    Text(s.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    val progress = if (s.dur > 0) (s.pos.toFloat() / s.dur).coerceIn(0f, 1f) else 0f
                    Slider(value = drag ?: progress, onValueChange = { drag = it },
                        onValueChangeFinished = { drag?.let { controller?.transportControls?.seekTo((it * s.dur).toLong()) }; drag = null },
                        enabled = s.dur > 0)
                    Row { Text(mmss(s.pos), style = MaterialTheme.typography.labelMedium); Spacer(Modifier.weight(1f)); Text(mmss(s.dur), style = MaterialTheme.typography.labelMedium) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        IconButton({ controller?.transportControls?.skipToPrevious() }, Modifier.size(56.dp)) { Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(34.dp)) }
                        Spacer(Modifier.width(12.dp))
                        FilledIconButton({ if (s.playing) controller?.transportControls?.pause() else controller?.transportControls?.play() }, Modifier.size(68.dp)) {
                            Icon(if (s.playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", Modifier.size(40.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        IconButton({ controller?.transportControls?.skipToNext() }, Modifier.size(56.dp)) { Icon(Icons.Default.SkipNext, "Next", Modifier.size(34.dp)) }
                    }
                }
            }
        }
    }

    if (picking) AppPicker(ctx, onPick = { settings.updateMediaPkg(it); picking = false }, onDismiss = { picking = false })
}

@Composable
private fun AppPicker(ctx: Context, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val apps = remember {
        val pm = ctx.packageManager
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { AppEntry(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
            .distinctBy { it.pkg }.filter { it.pkg != ctx.packageName }.sortedBy { it.label.lowercase() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose music app") },
        text = {
            LazyColumn(Modifier.heightIn(max = 260.dp)) {
                item { ListItem(headlineContent = { Text("Any app") }, modifier = Modifier.clickable { onPick("") }) }
                items(apps, key = { it.pkg }) { a ->
                    val bmp = remember(a.pkg) { a.icon.toBitmap(96, 96).asImageBitmap() }
                    ListItem(
                        headlineContent = { Text(a.label) },
                        leadingContent = { Image(bmp, null, Modifier.size(40.dp)) },
                        modifier = Modifier.clickable { onPick(a.pkg) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Close") } }
    )
}
