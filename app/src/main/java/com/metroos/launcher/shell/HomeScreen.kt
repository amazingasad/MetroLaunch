package com.metroos.launcher.shell

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metroos.launcher.data.AppInfo
import com.metroos.launcher.data.BuiltInApp
import com.metroos.launcher.data.Tile
import com.metroos.launcher.data.TileSize
import com.metroos.launcher.data.TileStore
import com.metroos.launcher.data.packColor
import com.metroos.launcher.data.tileColor
import com.metroos.launcher.util.Springs
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Modernized Windows Phone start screen: 4-column live tile grid,
 * typographic clock, status row and glass affordances.
 */
@Composable
fun HomeScreen(
    onOpenRoute: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit
) {
    var tiles by remember { mutableStateOf(TileStore.load()) }
    var resizeTarget by remember { mutableStateOf<Tile?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        StatusBarRow()
        Spacer(Modifier.height(10.dp))
        TypographicClock()
        Spacer(Modifier.height(18.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(tiles, key = { it.id }, span = { GridItemSpan(it.size.cols) }) { tile ->
                // Square grid cells: MEDIUM spans 2 columns and 1 row -> width:height of 2:1.
                val aspect = when (tile.size) {
                    TileSize.SMALL -> 1f
                    TileSize.MEDIUM -> 2f
                    TileSize.WIDE -> 1f
                }
                LiveTile(
                    tile = tile,
                    modifier = Modifier
                        .aspectRatio(aspect)
                        .combinedClickable(
                            onClick = {
                                if (tile.route.startsWith("tile/")) {
                                    if (tile.route == "tile/weather") onOpenRoute("app/weather")
                                } else {
                                    onOpenRoute(tile.route)
                                }
                            },
                            onLongClick = { resizeTarget = tile }
                        ),
                    onOpenRoute = onOpenRoute
                )
            }
            item(span = { GridItemSpan(4) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                        .clickable(onClick = onOpenDrawer),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "All apps",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "all apps",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }
    }

    resizeTarget?.let { target ->
        TileResizeDialog(
            tile = target,
            onDismiss = { resizeTarget = null },
            onResize = { newSize ->
                tiles = tiles.map { if (it.id == target.id) it.copy(size = newSize) else it }
                TileStore.save(tiles)
                resizeTarget = null
            },
            onRemove = {
                tiles = tiles.filter { it.id != target.id }
                TileStore.save(tiles)
                resizeTarget = null
            }
        )
    }
}

@Composable
fun StatusBarRow() {
    val context = LocalContext.current
    val battery = remember {
        val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    }
    val scale = remember {
        val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
    }
    val pct = if (battery >= 0 && scale > 0) battery * 100 / scale else 100
    var time by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
            delay(20_000)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(time, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Light)
        Text(
            "$pct%",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Light
        )
    }
}

@Composable
fun TypographicClock() {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }
    Column {
        Text(
            now.format(DateTimeFormatter.ofPattern("HH:mm")),
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            lineHeight = 68.sp
        )
        Text(
            now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())),
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Light
        )
    }
}

/** Live tile with periodic 3D content flip driven by spring physics. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiveTile(
    tile: Tile,
    modifier: Modifier = Modifier,
    onOpenRoute: (String) -> Unit
) {
    val isLive = tile.route.startsWith("tile/") || tile.id == "chess"
    var flipped by remember(tile.id) { mutableStateOf(false) }
    val rotation = remember(tile.id) { Animatable(0f) }

    LaunchedEffect(tile.id, isLive) {
        if (!isLive) return@LaunchedEffect
        while (true) {
            delay(4_500)
            flipped = !flipped
            rotation.animateTo(
                if (flipped) 180f else 0f,
                spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    val showBack = rotation.value > 90f
    val builtIn = BuiltInApp.byRoute(tile.route)

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation.value
                cameraDistance = 14 * density * 160f
            }
            .background(tileColor(tile.color)),
        contentAlignment = Alignment.Center
    ) {
        // Front face mirrored when past 90deg so text is not reversed.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationY = if (showBack) 180f else 0f },
            contentAlignment = Alignment.Center
        ) {
            if (showBack) {
                TileBackFace(tile)
            } else {
                TileFrontFace(tile, builtIn)
            }
        }
    }
}

@Composable
private fun TileFrontFace(tile: Tile, builtIn: BuiltInApp?) {
    when (tile.route) {
        "tile/clock" -> ClockTileFace()
        "tile/weather" -> WeatherTileFace()
        else -> TileIconFace(builtIn)
    }
}

@Composable
private fun ClockTileFace() {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(12.dp)) {
        Text(
            now.format(DateTimeFormatter.ofPattern("HH:mm")),
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Light
        )
        Text(
            now.format(DateTimeFormatter.ofPattern("EEE d MMM")),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun WeatherTileFace() {
    val weather = rememberWeatherSummary()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                weather?.let { "${it.hi}°" } ?: "--°",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                weather?.condition ?: "Loading…",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp
            )
        }
        Icon(
            Icons.Filled.Cloud,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun TileIconFace(builtIn: BuiltInApp?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(8.dp)
    ) {
        Icon(
            builtIn?.icon ?: Icons.Filled.Search,
            contentDescription = builtIn?.label,
            tint = Color.White,
            modifier = Modifier.size(if (builtIn != null && builtIn.route.startsWith("game/")) 34.dp else 28.dp)
        )
        if (builtIn != null) {
            Spacer(Modifier.height(6.dp))
            Text(builtIn.label, color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
private fun TileBackFace(tile: Tile) {
    when (tile.route) {
        "tile/clock" -> {
            val now = remember { LocalDateTime.now() }
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Time", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                Text(
                    now.format(DateTimeFormatter.ofPattern("ss")),
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Light
                )
                Text("seconds", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
            }
        }
        "tile/weather" -> {
            val weather = rememberWeatherSummary()
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "H ${weather?.hi ?: "--"}°  L ${weather?.lo ?: "--"}°",
                    color = Color.White, fontSize = 16.sp
                )
                Text(
                    weather?.let { "${it.day} · ${it.condition}" } ?: "—",
                    color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp
                )
            }
        }
        "game/chess" -> {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("Chess", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Minimax AI ready.\nTap to play.",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp
                )
            }
        }
        else -> {
            val builtIn = BuiltInApp.byRoute(tile.route)
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    builtIn?.label ?: "App",
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                )
                Text("Open", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun TileResizeDialog(
    tile: Tile,
    onDismiss: () -> Unit,
    onResize: (TileSize) -> Unit,
    onRemove: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        title = { Text("Tile \"${tile.id}\"") },
        text = {
            Column {
                TileSize.entries.forEach { size ->
                    Text(
                        text = when (size) {
                            TileSize.SMALL -> "Small (1x1)"
                            TileSize.MEDIUM -> "Medium (2x1)"
                            TileSize.WIDE -> "Large (2x2)"
                        },
                        fontSize = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResize(size) }
                            .padding(vertical = 10.dp)
                    )
                }
                Text(
                    "Remove tile",
                    color = Color(0xFFE51400),
                    fontSize = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRemove)
                        .padding(vertical = 10.dp)
                )
            }
        }
    )
}

/** Subtle breathing glow used behind the clock. */
@Composable
fun AmbientGlow(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "glow")
    val alpha by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(tween(3600), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    Box(modifier.graphicsLayer { this.alpha = alpha }) {
        Box(
            Modifier
                .size(220.dp)
                .background(color, CircleShape)
        )
    }
}

/** Placeholder weather summary used by tiles until WeatherApp data lands. */
data class TileWeather(val day: String, val hi: Int, val lo: Int, val condition: String)

@Composable
fun rememberWeatherSummary(): TileWeather? {
    var result by remember { mutableStateOf<TileWeather?>(null) }
    LaunchedEffect(Unit) {
        result = runCatching { com.metroos.launcher.apps.WeatherRepository.fetchSummary() }
            .getOrNull()
    }
    return result
}

/** Small helper so external launches keep FLAG_ACTIVITY_NEW_TASK. */
fun ensureNewTask(intent: Intent): Intent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
