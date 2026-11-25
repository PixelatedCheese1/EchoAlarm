package com.pixelatedcheese1.echoclock

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Material You: wallpaper-derived dynamic colors on Android 12+, a seeded fallback below that. */
@Composable
fun EchoClockTheme(darkOverride: Boolean?, content: @Composable () -> Unit) {
    val dark = darkOverride ?: isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
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
