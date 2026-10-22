package com.metroos.launcher.games

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metroos.launcher.ui.theme.MetroOrange
import com.metroos.launcher.util.Prefs
import kotlin.math.abs

private data class Tile2048(val id: Int, val r: Int, val c: Int, val v: Int, val pop: Boolean = false)

/**
 * 2048 with stable tile identities so Compose animates slide + merge
 * with spring physics. Swipe controls; score and best score persist.
 */
private class Game2048Logic {
    private val grid = Array(4) { arrayOfNulls<Tile2048>(4) }
    private var nextId = 1
    var score = 0
        private set

    fun tiles(): List<Tile2048> = grid.flatten().filterNotNull()

    fun spawn() {
        val empties = mutableListOf<Pair<Int, Int>>()
        for (r in 0..3) for (c in 0..3) if (grid[r][c] == null) empties.add(r to c)
        if (empties.isEmpty()) return
        val (r, c) = empties.random()
        grid[r][c] = Tile2048(nextId++, r, c, if (Math.random() < 0.9) 2 else 4, pop = true)
    }

    /** dir: 0 up, 1 right, 2 down, 3 left. Returns true if the board changed. */
    fun move(dir: Int): Boolean {
        val dr = intArrayOf(-1, 0, 1, 0)[dir]
        val dc = intArrayOf(0, 1, 0, -1)[dir]
        val rowOrder = if (dr == 1) (3 downTo 0) else (0..3)
        val colOrder = if (dc == 1) (3 downTo 0) else (0..3)
        val merged = mutableSetOf<Int>()
        var moved = false

        for (r in rowOrder) {
            for (c in colOrder) {
                val tile = grid[r][c] ?: continue
                var nr = r
                var nc = c
                var target: Tile2048? = null
                // Slide until blocked.
                while (true) {
                    val ar = nr + dr
                    val ac = nc + dc
                    if (ar !in 0..3 || ac !in 0..3) break
                    val ahead = grid[ar][ac]
                    if (ahead == null) {
                        nr = ar; nc = ac
                    } else {
                        if (ahead.v == tile.v && ahead.id !in merged) target = ahead
                        break
                    }
                }
                if (target != null) {
                    // Merge into the destination tile.
                    grid[r][c] = null
                    val mergedTile = target.copy(v = target.v * 2, pop = true)
                    grid[target.r][target.c] = mergedTile
                    merged.add(mergedTile.id)
                    score += mergedTile.v
                    moved = true
                } else if (nr != r || nc != c) {
                    grid[r][c] = null
                    grid[nr][nc] = tile.copy(r = nr, c = nc)
                    moved = true
                }
            }
        }
        if (moved) spawn()
        return moved
    }

    fun gameOver(): Boolean {
        for (r in 0..3) for (c in 0..3) {
            if (grid[r][c] == null) return false
            val v = grid[r][c]!!.v
            if (r < 3 && grid[r + 1][c]?.v == v) return false
            if (c < 3 && grid[r][c + 1]?.v == v) return false
        }
        return true
    }

    fun reset() {
        for (r in 0..3) for (c in 0..3) grid[r][c] = null
        score = 0
        spawn()
        spawn()
    }
}

@Composable
fun Game2048() {
    val context = LocalContext.current
    val logic = remember { Game2048Logic() }
    var version by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(Prefs.getInt(context, "best_2048", 0)) }
    var over by remember { mutableStateOf(false) }

    fun update() {
        version++
        over = logic.gameOver()
        if (logic.score > best) {
            best = logic.score
            Prefs.putInt(context, "best_2048", best)
        }
    }

    LaunchedEffect(Unit) {
        if (logic.tiles().isEmpty()) {
            logic.reset()
            update()
        }
    }

    fun swipe(dir: Int) {
        if (over) return
        if (logic.move(dir)) update()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070E))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Text("2048", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("Score ${logic.score}", color = Color.White, fontSize = 15.sp)
                Text("Best $best", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            if (over) "Game over — tap board to restart" else "Swipe to move tiles",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )

        Spacer(Modifier.height(14.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    var startX = 0f
                    var startY = 0f
                    var endX = 0f
                    var endY = 0f
                    var active = false
                    detectDragGestures(
                        onDragStart = { start ->
                            startX = start.x; startY = start.y
                            endX = start.x; endY = start.y
                            active = true
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            endX += dragAmount.x
                            endY += dragAmount.y
                        },
                        onDragEnd = {
                            if (active) {
                                val dx = endX - startX
                                val dy = endY - startY
                                val dir = when {
                                    abs(dx) > abs(dy) && dx > 0 -> 1
                                    abs(dx) > abs(dy) -> 3
                                    dy > 0 -> 2
                                    else -> 0
                                }
                                if (maxOf(abs(dx), abs(dy)) > 40f) swipe(dir)
                            }
                            active = false
                        }
                    )
                }
        ) {
            @Suppress("UNUSED_EXPRESSION")
            version

            val gap = 8.dp
            val boardSide = minOf(maxWidth, maxHeight)
            val cell = (boardSide - gap * 5f) / 4f

            // Board background cells.
            Box(
                modifier = Modifier
                    .size(boardSide)
                    .background(Color(0xFFBBADA0))
                    .align(Alignment.Center)
            ) {
                for (r in 0..3) for (c in 0..3) {
                    Box(
                        modifier = Modifier
                            .offset(x = gap + cell * c + gap * c, y = gap + cell * r + gap * r)
                            .size(cell)
                            .background(Color(0xFFCDC1B4))
                    )
                }
                // Animated tiles keyed by identity.
                logic.tiles().forEach { tile ->
                    key(tile.id) {
                        AnimatedTile2048(
                            tile = tile,
                            cell = cell,
                            gap = gap
                        )
                    }
                }
                if (over) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x99000000))
                            .clickable {
                                logic.reset()
                                update()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Game over", color = Color.White, fontSize = 24.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedTile2048(tile: Tile2048, cell: Dp, gap: Dp) {
    val x by animateDpAsState(
        targetValue = gap + cell * tile.c + gap * tile.c,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f),
        label = "x${tile.id}"
    )
    val y by animateDpAsState(
        targetValue = gap + cell * tile.r + gap * tile.r,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f),
        label = "y${tile.id}"
    )
    var popScale by remember(tile.id, tile.v) { mutableStateOf(if (tile.pop) 0.4f else 1f) }
    LaunchedEffect(tile.id, tile.v) {
        if (tile.pop) {
            popScale = 1.18f
            kotlinx.coroutines.delay(60)
            popScale = 1f
        }
    }

    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(cell)
            .graphicsLayer {
                scaleX = popScale
                scaleY = popScale
            }
            .background(tileColor2048(tile.v)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            tile.v.toString(),
            color = if (tile.v <= 4) Color(0xFF776E65) else Color.White,
            fontSize = when {
                tile.v < 100 -> 28.sp
                tile.v < 1000 -> 22.sp
                else -> 17.sp
            },
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun tileColor2048(v: Int): Color = when {
    v <= 2 -> Color(0xFFEEE4DA)
    v <= 4 -> Color(0xFFEDE0C8)
    v <= 8 -> Color(0xFFF2B179)
    v <= 16 -> Color(0xFFF59563)
    v <= 32 -> Color(0xFFF67C5F)
    v <= 64 -> Color(0xFFF65E3B)
    v <= 128 -> Color(0xFFEDCF72)
    v <= 256 -> Color(0xFFEDCC61)
    v <= 512 -> Color(0xFFEDC850)
    v <= 1024 -> Color(0xFFEDC53F)
    else -> Color(0xFFEDC22E)
}
