package com.metroos.launcher.games

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metroos.launcher.ui.theme.MetroBlue
import com.metroos.launcher.ui.theme.MetroOrange
import com.metroos.launcher.ui.theme.MetroPurple
import com.metroos.launcher.ui.theme.MetroRed
import com.metroos.launcher.ui.theme.MetroTeal
import kotlinx.coroutines.delay

private val LightSquare = Color(0xFFE6EAF2)
private val DarkSquare = Color(0xFF64748F)
private val SelectedSquare = Color(0xFF00ABA9)
private val LastMoveSquare = Color(0xFFFA6800)

/**
 * Touch-friendly chess with full legal move validation, move history,
 * undo and a depth-3 minimax AI opponent.
 */
@Composable
fun ChessGame() {
    var board by remember { mutableStateOf(ChessEngine.initial()) }
    var history by remember { mutableStateOf(listOf(board)) }
    var whiteTurn by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var legal by remember { mutableStateOf(listOf<ChessEngine.Move>()) }
    var lastMove by remember { mutableStateOf<ChessEngine.Move?>(null) }
    var thinking by remember { mutableStateOf(false) }

    val gameOver = remember(board) { !ChessEngine.hasAnyLegalMove(board, whiteTurn) }
    val winner = if (gameOver && ChessEngine.inCheck(board, whiteTurn)) !whiteTurn else null

    fun reset() {
        board = ChessEngine.initial()
        history = listOf(board)
        whiteTurn = true
        selected = null
        legal = emptyList()
        lastMove = null
    }

    fun commit(move: ChessEngine.Move) {
        val next = ChessEngine.applyMove(board, move)
        history = history + next
        board = next
        whiteTurn = !whiteTurn
        lastMove = move
        selected = null
        legal = emptyList()
    }

    fun tapCell(r: Int, c: Int) {
        val move = legal.firstOrNull { it.tr == r && it.tc == c }
        if (move != null) {
            commit(move)
            return
        }
        val piece = board[r][c]
        val isOwn = piece != ChessEngine.EMPTY &&
            piece.isUpperCase() == whiteTurn
        if (isOwn) {
            selected = r to c
            legal = ChessEngine.legalMoves(board, r, c)
        } else {
            selected = null
            legal = emptyList()
        }
    }

    // AI opponent turn.
    LaunchedEffect(whiteTurn, history.size) {
        if (!whiteTurn && !gameOver) {
            thinking = true
            delay(320)
            val move = ChessEngine.bestMove(board, white = false, depth = 3)
            thinking = false
            move?.let { commit(it) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070E))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                when {
                    gameOver && winner == true -> "White wins"
                    gameOver && winner == false -> "Black wins"
                    gameOver -> "Stalemate"
                    thinking -> "AI thinking…"
                    whiteTurn -> "Your move"
                    else -> "Black to move"
                },
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Undo",
                color = MetroTeal,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable {
                        if (history.size > 1) {
                            // Step back two plies so the human stays on turn.
                            val target = maxOf(1, history.size - 2)
                            history = history.subList(0, target)
                            board = history.last()
                            whiteTurn = true
                            selected = null
                            legal = emptyList()
                            lastMove = null
                        }
                    }
                    .padding(8.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "New",
                color = MetroOrange,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable { reset() }
                    .padding(8.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val side = minOf(maxWidth, maxHeight)
            val cell = side / 8f
            Box(modifier = Modifier.size(side)) {
                for (r in 0..7) for (c in 0..7) {
                    val dark = (r + c) % 2 == 1
                    val isSel = selected == (r to c)
                    val isLegal = legal.any { it.tr == r && it.tc == c }
                    val isLast = lastMove?.let { lm ->
                        (lm.tr == r && lm.tc == c) || (lm.fr == r && lm.fc == c)
                    } == true
                    val bg = when {
                        isSel -> SelectedSquare
                        isLast -> LastMoveSquare.copy(alpha = 0.55f)
                        dark -> DarkSquare
                        else -> LightSquare
                    }
                    Box(
                        modifier = Modifier
                            .size(cell)
                            .offset(cell * c, cell * r)
                            .background(bg)
                            .clickable { tapCell(r, c) },
                        contentAlignment = Alignment.Center
                    ) {
                        val piece = board[r][c]
                        if (piece != ChessEngine.EMPTY) {
                            Text(
                                ChessEngine.glyph(piece),
                                fontSize = (cell.value * 0.62f).sp,
                                color = if (piece.isUpperCase()) Color(0xFF101418) else Color(0xFFF4F6FA)
                            )
                        }
                        if (isLegal) {
                            Box(
                                Modifier
                                    .size(cell / 4f)
                                    .background(
                                        MetroPurple.copy(alpha = 0.7f),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
