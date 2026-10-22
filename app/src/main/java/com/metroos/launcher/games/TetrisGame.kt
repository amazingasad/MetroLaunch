package com.metroos.launcher.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metroos.launcher.ui.theme.MetroBlue
import com.metroos.launcher.ui.theme.MetroGreen
import com.metroos.launcher.ui.theme.MetroMagenta
import com.metroos.launcher.ui.theme.MetroOrange
import com.metroos.launcher.ui.theme.MetroPurple
import com.metroos.launcher.ui.theme.MetroRed
import com.metroos.launcher.ui.theme.MetroTeal
import com.metroos.launcher.util.Prefs
import kotlinx.coroutines.delay
import kotlin.math.min

/** Tetromino definitions: 4 base cells in a 4x4 frame; rotation maps (r,c) -> (c, 3-r). */
object TetrisPieces {
    val SHAPES: List<List<Pair<Int, Int>>> = listOf(
        listOf(0 to 1, 1 to 1, 2 to 1, 3 to 1), // I
        listOf(0 to 1, 0 to 2, 1 to 1, 1 to 2), // O
        listOf(0 to 1, 1 to 0, 1 to 1, 1 to 2), // T
        listOf(0 to 1, 0 to 2, 1 to 0, 1 to 1), // S
        listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2), // Z
        listOf(0 to 0, 1 to 0, 1 to 1, 1 to 2), // J
        listOf(0 to 2, 1 to 0, 1 to 1, 1 to 2)  // L
    )

    val COLORS: List<Color> = listOf(
        MetroTeal, Color(0xFFF7C948), MetroPurple, MetroGreen,
        MetroRed, MetroBlue, MetroOrange
    )

    fun rotate(cells: List<Pair<Int, Int>>): List<Pair<Int, Int>> =
        cells.map { (r, c) -> c to (3 - r) }
}

/**
 * Falling tetromino engine: 10x20 matrix, wall-friendly rotation,
 * line clears, level speed curve and persisted high score.
 */
class TetrisLogic {
    val cols = 10
    val rows = 20
    var board = Array(rows) { IntArray(cols) }
        private set
    var type = 0
        private set
    var x = 3
        private set
    var y = 0
        private set
    var rot = 0
        private set
    var score = 0
        private set
    var lines = 0
        private set
    var gameOver = false
        private set

    fun cells(t: Int = type, rotation: Int = rot): List<Pair<Int, Int>> {
        var shape = TetrisPieces.SHAPES[t % TetrisPieces.SHAPES.size]
        repeat(rotation % 4) { shape = TetrisPieces.rotate(shape) }
        return shape
    }

    private fun collides(shape: List<Pair<Int, Int>>, px: Int, py: Int): Boolean =
        shape.any { (r, c) ->
            val br = py + r
            val bc = px + c
            br >= rows || bc < 0 || bc >= cols || (br >= 0 && board[br][bc] != 0)
        }

    fun spawn() {
        type = (0 until TetrisPieces.SHAPES.size).random()
        rot = 0
        x = 3
        y = -1
        if (collides(cells(), x, maxOf(0, y))) gameOver = true
    }

    fun moveLeft() {
        if (!gameOver && !collides(cells(), x - 1, y)) x--
    }

    fun moveRight() {
        if (!gameOver && !collides(cells(), x + 1, y)) x++
    }

    fun rotatePiece() {
        val next = (rot + 1) % 4
        if (!gameOver && !collides(cells(type, next), x, y)) rot = next
    }

    fun softDrop(): Boolean {
        if (gameOver) return false
        return if (!collides(cells(), x, y + 1)) {
            y++
            true
        } else {
            lock()
            false
        }
    }

    fun hardDrop() {
        while (softDrop()) { /* fall until lock */ }
    }

    private fun lock() {
        val shape = cells()
        // Materialize piece above the visible floor.
        shape.forEach { (r, c) ->
            val br = y + r
            if (br in 0 until rows) board[br][x + c] = type + 1
        }
        clearLines()
        spawn()
    }

    private fun clearLines() {
        val full = board.indices.filter { r -> board[r].all { it != 0 } }
        full.forEach { r ->
            for (row in r downTo 1) board[row] = board[row - 1].clone()
            board[0] = IntArray(cols)
        }
        lines += full.size
        score += when (full.size) {
            1 -> 100; 2 -> 300; 3 -> 500; 4 -> 800; else -> 0
        }
    }

    fun tick() {
        softDrop()
    }

    fun reset() {
        board = Array(rows) { IntArray(cols) }
        score = 0
        lines = 0
        gameOver = false
        spawn()
    }
}

@Composable
fun TetrisGame() {
    val context = LocalContext.current
    val logic = remember { TetrisLogic() }
    var version by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    var best by remember { mutableIntStateOf(Prefs.getInt(context, "best_tetris", 0)) }

    fun update() {
        version++
        if (logic.score > best) {
            best = logic.score
            Prefs.putInt(context, "best_tetris", best)
        }
    }

    LaunchedEffect(Unit) {
        if (logic.score == 0 && !logic.gameOver) {
            logic.reset()
            update()
        }
    }

    // Gravity loop with level speed curve.
    LaunchedEffect(playing, version) {
        if (playing && !logic.gameOver) {
            val level = 1 + logic.lines / 10
            delay((maxOf(140, 640 - (level - 1) * 70)).toLong())
            logic.tick()
            update()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070E))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Text("TETRIS", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("Score ${logic.score}", color = Color.White, fontSize = 15.sp)
                Text("Lines ${logic.lines} · Best $best", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            @Suppress("UNUSED_EXPRESSION")
            version

            val cell = min(
                (maxWidth.value - 24f) / logic.cols,
                (maxHeight.value - 8f) / logic.rows
            ).dp

            // Ghost of locked cells.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(cell * logic.cols, cell * logic.rows)
            ) {
                for (r in 0 until logic.rows) for (c in 0 until logic.cols) {
                    val filled = logic.board[r][c]
                    if (filled != 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = cell * c, y = cell * r)
                                .size(cell - 1.dp)
                                .background(TetrisPieces.COLORS[filled - 1])
                        )
                    }
                }
                // Active piece.
                if (!logic.gameOver) {
                    logic.cells().forEach { (r, c) ->
                        val br = logic.y + r
                        if (br >= 0) {
                            Box(
                                modifier = Modifier
                                    .offset(x = cell * (logic.x + c), y = cell * br)
                                    .size(cell - 1.dp)
                                    .background(TetrisPieces.COLORS[logic.type])
                            )
                        }
                    }
                }
                if (logic.gameOver) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x99000000))
                            .clickable {
                                logic.reset()
                                playing = true
                                update()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Game over — tap to restart", color = Color.White, fontSize = 18.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Touch controls: hold-style repeat is delegated to repeated taps for simplicity.
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TetrisControl("←", Modifier.weight(1f)) { logic.moveLeft(); update() }
            TetrisControl("⟳", Modifier.weight(1f)) { logic.rotatePiece(); update() }
            TetrisControl("→", Modifier.weight(1f)) { logic.moveRight(); update() }
            TetrisControl("↓", Modifier.weight(1f)) { logic.softDrop(); update() }
            TetrisControl("⤓", Modifier.weight(1f)) { logic.hardDrop(); update() }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (playing) "Playing · tap board area not needed" else "Paused",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun TetrisControl(label: String, modifier: Modifier = Modifier, onPress: () -> Unit) {
    Box(
        modifier = modifier
            .height(54.dp)
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onPress),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 20.sp)
    }
}
