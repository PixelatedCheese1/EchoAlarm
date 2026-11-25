package com.pixelatedcheese1.echoclock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Wallpapers = every .jpg/.jpeg/.png/.webp you put in  app/src/main/assets/wallpapers/
 * They are discovered automatically and shown in the wallpaper picker.
 */
object Wallpapers {
    private const val DIR = "wallpapers"
    private val EXT = listOf(".jpg", ".jpeg", ".png", ".webp")

    fun list(ctx: Context): List<String> = try {
        ctx.assets.list(DIR)?.filter { n -> EXT.any { n.lowercase().endsWith(it) } }?.sorted() ?: emptyList()
    } catch (e: Exception) { emptyList() }

    fun load(ctx: Context, name: String, maxDim: Int): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.assets.open("$DIR/$name").use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        ctx.assets.open("$DIR/$name").use { BitmapFactory.decodeStream(it, null, opts) }
    } catch (e: Exception) { null }
}

@Composable
fun rememberWallpaper(name: String, maxDim: Int): State<Bitmap?> {
    val ctx = LocalContext.current.applicationContext
    return produceState<Bitmap?>(null, name, maxDim) {
        value = withContext(Dispatchers.IO) { Wallpapers.load(ctx, name, maxDim) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperPicker(settings: AppSettings, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val names = remember { Wallpapers.list(ctx) }
    BackHandler(onBack = onClose)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text("Wallpapers", style = MaterialTheme.typography.titleLarge)
            }
            if (names.isEmpty()) Text(
                "No wallpapers found. Put images in app/src/main/assets/wallpapers/ and rebuild.",
                Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyVerticalGrid(
                GridCells.Adaptive(170.dp), contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Tile("", "None", settings.wallpaper.isEmpty()) { settings.updateWallpaper("") } }
                items(names, key = { it }) { n -> Tile(n, null, settings.wallpaper == n) { settings.updateWallpaper(n) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Tile(name: String, title: String?, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (title != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Block, null); Text(title)
                }
            } else {
                val bmp by rememberWallpaper(name, 400)
                bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
            if (selected) Icon(Icons.Default.Check, null, Modifier.align(Alignment.TopEnd).padding(6.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}
