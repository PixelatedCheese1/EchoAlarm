package com.pixelatedcheese1.echoclock

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class DrawerApp(val pkg: String, val label: String, val icon: Drawable)

private fun loadDrawerApps(ctx: Context): List<DrawerApp> {
    val pm = ctx.packageManager
    return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        .map { DrawerApp(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
        .distinctBy { it.pkg }.filter { it.pkg != ctx.packageName }.sortedBy { it.label.lowercase() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerScreen() {
    val ctx = LocalContext.current
    val focus: FocusManager = LocalFocusManager.current
    val kb = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf("") }
    val apps by produceState(emptyList<DrawerApp>()) { value = withContext(Dispatchers.IO) { loadDrawerApps(ctx) } }
    val shown = remember(apps, query) { apps.filter { it.label.contains(query.trim(), ignoreCase = true) } }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Apps", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            OutlinedTextField(
                query, { query = it }, Modifier.width(240.dp), singleLine = true,
                placeholder = { Text("Search apps") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); kb?.hide() })
            )
        }
        LazyVerticalGrid(GridCells.Adaptive(92.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
            items(shown, key = { it.pkg }) { app ->
                val bmp = remember(app.pkg) { app.icon.toBitmap(112, 112).asImageBitmap() }
                Column(
                    Modifier.fillMaxWidth().clickable {
                        ctx.packageManager.getLaunchIntentForPackage(app.pkg)?.let {
                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(it)
                        }
                    }.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(bmp, app.label, Modifier.size(52.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(app.label, maxLines = 2, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
