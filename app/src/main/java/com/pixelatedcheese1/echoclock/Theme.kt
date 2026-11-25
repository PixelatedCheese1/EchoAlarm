package com.pixelatedcheese1.echoclock

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val NightScheme = darkColorScheme(
    primary = Color(0xFFD32F2F), onPrimary = Color.Black,
    primaryContainer = Color(0xFF3B0D0D), onPrimaryContainer = Color(0xFFE53935),
    secondary = Color(0xFFC62828), onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2E0A0A), onSecondaryContainer = Color(0xFFE53935),
    tertiary = Color(0xFFB71C1C), onTertiary = Color.Black,
    background = Color.Black, onBackground = Color(0xFFD32F2F),
    surface = Color.Black, onSurface = Color(0xFFD32F2F),
    surfaceVariant = Color(0xFF220808), onSurfaceVariant = Color(0xFFB71C1C),
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF0A0202),
    surfaceContainer = Color(0xFF120404), surfaceContainerHigh = Color(0xFF1A0606),
    surfaceContainerHighest = Color(0xFF220808),
    outline = Color(0xFF7F1D1D), outlineVariant = Color(0xFF3B0D0D)
)

/** Material You on Android 12+, seeded fallback below. night = red-on-black low-light scheme. */
@Composable
fun EchoClockTheme(darkOverride: Boolean?, night: Boolean = false, content: @Composable () -> Unit) {
    val dark = darkOverride ?: isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        night -> NightScheme
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme(
            primary = Color(0xFFD0BCFF), onPrimary = Color(0xFF381E72),
            secondary = Color(0xFFCCC2DC), tertiary = Color(0xFFEFB8C8),
            background = Color(0xFF141218), surface = Color(0xFF141218),
            surfaceVariant = Color(0xFF49454F)
        )
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
