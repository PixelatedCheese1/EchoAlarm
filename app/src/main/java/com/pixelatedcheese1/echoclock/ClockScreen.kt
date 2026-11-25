package com.pixelatedcheese1.echoclock

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClockScreen(settings: AppSettings, onLongPress: () -> Unit) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(250) } }
    val face = ClockFaces.byId(settings.faceId)

    Box(
        Modifier
            .fillMaxSize()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        when (face.kind) {
            FaceKind.Digital -> DigitalClock(now, face, settings)
            FaceKind.Stacked -> StackedClock(now, face, settings)
            FaceKind.Analog -> AnalogClock(now, settings)
        }
    }
}

private fun fmt(t: LocalDateTime, p: String) = t.format(DateTimeFormatter.ofPattern(p))

@Composable
private fun DigitalClock(now: LocalDateTime, face: ClockFace, s: AppSettings) {
    val text = fmt(now, (if (s.use24h) "HH:mm" else "h:mm") + (if (s.showSeconds) ":ss" else ""))
    val ampm = if (s.use24h) "" else fmt(now, "a")
    BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp)) {
        val extra = if (ampm.isEmpty()) 0f else 1.4f
        val size = minOf(maxHeight.value * 0.6f, maxWidth.value / (text.length * 0.58f + extra))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text, fontSize = size.sp, fontFamily = face.font, fontWeight = face.weight,
                    color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false
                )
                if (ampm.isNotEmpty()) Text(
                    ampm, fontSize = (size * 0.22f).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = (size * 0.12f).dp)
                )
            }
            if (s.showDate) Text(
                fmt(now, "EEEE, MMMM d"), fontSize = minOf(size * 0.2f, 30f).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StackedClock(now: LocalDateTime, face: ClockFace, s: AppSettings) {
    val hh = fmt(now, if (s.use24h) "HH" else "hh")
    val mm = fmt(now, "mm")
    BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp)) {
        val size = minOf(maxHeight.value * 0.44f, maxWidth.value / 2.2f)
        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(hh, fontSize = size.sp, lineHeight = (size * 0.88f).sp, fontFamily = face.font,
                    fontWeight = face.weight, color = MaterialTheme.colorScheme.primary)
                Text(mm, fontSize = size.sp, lineHeight = (size * 0.88f).sp, fontFamily = face.font,
                    fontWeight = face.weight, color = MaterialTheme.colorScheme.tertiary)
            }
            Column(Modifier.padding(start = 28.dp)) {
                if (!s.use24h) Text(fmt(now, "a"), fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface)
                if (s.showSeconds) Text(fmt(now, "ss"), fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (s.showDate) {
                    Text(fmt(now, "EEEE"), fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(fmt(now, "MMM d"), fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AnalogClock(now: LocalDateTime, s: AppSettings) {
    val cs = MaterialTheme.colorScheme
    val tick = cs.onSurfaceVariant
    val hourC = cs.primary
    val minC = cs.onSurface
    val secC = cs.tertiary
    val face = cs.surfaceVariant.copy(alpha = 0.35f)
    Row(Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center) {
        Canvas(Modifier.fillMaxHeight().aspectRatio(1f).padding(8.dp)) {
            val c = center
            val r = size.minDimension / 2f
            drawCircle(face, r, c)
            for (i in 0 until 60) {
                val a = Math.toRadians(i * 6.0 - 90.0)
                val long = i % 5 == 0
                val inner = r * if (long) 0.84f else 0.92f
                val outer = r * 0.97f
                drawLine(
                    tick, Offset(c.x + cos(a).toFloat() * inner, c.y + sin(a).toFloat() * inner),
                    Offset(c.x + cos(a).toFloat() * outer, c.y + sin(a).toFloat() * outer),
                    strokeWidth = if (long) 7f else 3f, cap = StrokeCap.Round
                )
            }
            fun hand(frac: Float, len: Float, w: Float, col: androidx.compose.ui.graphics.Color) {
                val a = frac * 2.0 * Math.PI - Math.PI / 2
                drawLine(col, c, Offset(c.x + cos(a).toFloat() * r * len, c.y + sin(a).toFloat() * r * len),
                    strokeWidth = w, cap = StrokeCap.Round)
            }
            val sec = now.second + now.nano / 1_000_000_000f
            val min = now.minute + sec / 60f
            val hr = (now.hour % 12) + min / 60f
            hand(hr / 12f, 0.5f, 14f, hourC)
            hand(min / 60f, 0.75f, 9f, minC)
            if (s.showSeconds) hand(sec / 60f, 0.82f, 4f, secC)
            drawCircle(hourC, 11f, c)
        }
        if (s.showDate) Column(Modifier.padding(start = 24.dp)) {
            Text(fmt(now, "EEEE"), fontSize = 28.sp, color = cs.onSurface)
            Text(fmt(now, "MMMM d"), fontSize = 22.sp, color = cs.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockSettingsSheet(settings: AppSettings, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("Clock face", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                items(ClockFaces.all) { f ->
                    FilterChip(selected = f.id == settings.faceId, onClick = { settings.setFace(f.id) },
                        label = { Text(f.name) })
                }
            }
            SettingRow("24-hour time", settings.use24h, settings::setUse24h)
            SettingRow("Show seconds", settings.showSeconds, settings::setShowSeconds)
            SettingRow("Show date", settings.showDate, settings::setShowDate)
            SettingRow("Keep screen on", settings.keepAwake, settings::setKeepAwake)
            SettingRow("Dark theme", settings.dark, settings::setDark)
        }
    }
}

@Composable
private fun SettingRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = value, onCheckedChange = onChange)
    }
}
