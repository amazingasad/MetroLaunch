package com.metroos.launcher.games

import kotlin.math.abs

/**
 * Compact, allocation-friendly chess engine: full legal move generation
 * (including castling and promotion to queen), check detection and a
 * negamax alpha-beta AI. Board is an 8x8 char grid; UPPERCASE = white.
 */
object ChessEngine {

    const val EMPTY = ' '

    data class Move(val fr: Int, val fc: Int, val tr: Int, val tc: Int, val promo: Char = EMPTY)

    fun initial(): Array<CharArray> = arrayOf(
        charArrayOf('r', 'n', 'b', 'q', 'k', 'b', 'n', 'r'),
        charArrayOf('p', 'p', 'p', 'p', 'p', 'p', 'p', 'p'),
        CharArray(8) { EMPTY },
        CharArray(8) { EMPTY },
        CharArray(8) { EMPTY },
        CharArray(8) { EMPTY },
        charArrayOf('P', 'P', 'P', 'P', 'P', 'P', 'P', 'P'),
        charArrayOf('R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R')
    )

    fun cloneBoard(board: Array<CharArray>): Array<CharArray> =
        Array(board.size) { board[it].clone() }

    private fun inside(r: Int, c: Int): Boolean = r in 0..7 && c in 0..7

    private fun sideOf(piece: Char): Int = when {
        piece == EMPTY -> 0
        piece.isUpperCase() -> 1
        else -> -1
    }

    private fun isWhite(piece: Char): Boolean = piece.isUpperCase()

    /** Squares attacked by [byWhite] side. Used for check and castling legality. */
    fun attacked(board: Array<CharArray>, r: Int, c: Int, byWhite: Boolean): Boolean {
        // Pawn attacks
        val pr = if (byWhite) r + 1 else r - 1
        val pawn = if (byWhite) 'P' else 'p'
        for (dc in intArrayOf(-1, 1)) {
            if (inside(pr, c + dc) && board[pr][c + dc] == pawn) return true
        }
        // Knight attacks
        val knight = if (byWhite) 'N' else 'n'
        for (off in KNIGHT_OFFSETS) {
            val kr = r + off[0]; val kc = c + off[1]
            if (inside(kr, kc) && board[kr][kc] == knight) return true
        }
        // King adjacency
        val king = if (byWhite) 'K' else 'k'
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val kr = r + dr; val kc = c + dc
            if (inside(kr, kc) && board[kr][kc] == king) return true
        }
        // Sliding pieces
        val rook = if (byWhite) 'R' else 'r'
        val bishop = if (byWhite) 'B' else 'b'
        val queen = if (byWhite) 'Q' else 'q'
        for (dir in STRAIGHT_DIRS) {
            var sr = r + dir[0]; var sc = c + dir[1]
            while (inside(sr, sc)) {
                val piece = board[sr][sc]
                if (piece != EMPTY) {
                    if (piece == rook || piece == queen) return true
                    break
                }
                sr += dir[0]; sc += dir[1]
            }
        }
        for (dir in DIAGONAL_DIRS) {
            var sr = r + dir[0]; var sc = c + dir[1]
            while (inside(sr, sc)) {
                val piece = board[sr][sc]
                if (piece != EMPTY) {
                    if (piece == bishop || piece == queen) return true
                    break
                }
                sr += dir[0]; sc += dir[1]
            }
        }
        return false
    }

    fun pseudoMoves(board: Array<CharArray>, fr: Int, fc: Int): List<Move> {
        val piece = board[fr][fc]
        if (piece == EMPTY) return emptyList()
        val white = isWhite(piece)
        val type = piece.lowercaseChar()
        val moves = mutableListOf<Move>()

        fun add(tr: Int, tc: Int) {
            if (inside(tr, tc) && sideOf(board[tr][tc]) != sideOf(piece)) {
                moves.add(Move(fr, fc, tr, tc))
            }
        }

        when (type) {
            'p' -> {
                val dir = if (white) -1 else 1
                val startRow = if (white) 6 else 1
                val promoRow = if (white) 0 else 7
                val fwd = fr + dir
                if (inside(fwd, fc) && board[fwd][fc] == EMPTY) {
                    if (fwd == promoRow) {
                        moves.add(Move(fr, fc, fwd, fc, if (white) 'Q' else 'q'))
                    } else {
                        moves.add(Move(fr, fc, fwd, fc))
                        val dbl = fr + 2 * dir
                        if (fr == startRow && board[dbl][fc] == EMPTY) {
                            moves.add(Move(fr, fc, dbl, fc))
                        }
                    }
                }
                for (dc in intArrayOf(-1, 1)) {
                    val tr = fwd; val tc = fc + dc
                    if (inside(tr, tc) && sideOf(board[tr][tc]) == -sideOf(piece)) {
                        if (tr == promoRow) {
                            moves.add(Move(fr, fc, tr, tc, if (white) 'Q' else 'q'))
                        } else {
                            moves.add(Move(fr, fc, tr, tc))
                        }
                    }
                }
            }
            'n' -> {
                for (off in KNIGHT_OFFSETS) add(fr + off[0], fc + off[1])
            }
            'k' -> {
                for (dr in -1..1) for (dc in -1..1) {
                    if (dr != 0 || dc != 0) add(fr + dr, fc + dc)
                }
                // Castling: king and rook still on home squares, path empty,
                // and no attacked transit squares.
                val enemyWhite = !white
                if (white && fr == 7 && fc == 4) {
                    if (board[7][5] == EMPTY && board[7][6] == EMPTY && board[7][7] == 'R' &&
                        !attacked(board, 7, 4, enemyWhite) &&
                        !attacked(board, 7, 5, enemyWhite) &&
                        !attacked(board, 7, 6, enemyWhite)
                    ) moves.add(Move(7, 4, 7, 6))
                    if (board[7][3] == EMPTY && board[7][2] == EMPTY && board[7][1] == EMPTY &&
                        board[7][0] == 'R' &&
                        !attacked(board, 7, 4, enemyWhite) &&
                        !attacked(board, 7, 3, enemyWhite) &&
                        !attacked(board, 7, 2, enemyWhite)
                    ) moves.add(Move(7, 4, 7, 2))
                } else if (!white && fr == 0 && fc == 4) {
                    if (board[0][5] == EMPTY && board[0][6] == EMPTY && board[0][7] == 'r' &&
                        !attacked(board, 0, 4, enemyWhite) &&
                        !attacked(board, 0, 5, enemyWhite) &&
                        !attacked(board, 0, 6, enemyWhite)
                    ) moves.add(Move(0, 4, 0, 6))
                    if (board[0][3] == EMPTY && board[0][2] == EMPTY && board[0][1] == EMPTY &&
                        board[0][0] == 'r' &&
                        !attacked(board, 0, 4, enemyWhite) &&
                        !attacked(board, 0, 3, enemyWhite) &&
                        !attacked(board, 0, 2, enemyWhite)
                    ) moves.add(Move(0, 4, 0, 2))
                }
            }
            'r' -> slide(board, fr, fc, piece, STRAIGHT_DIRS, moves)
            'b' -> slide(board, fr, fc, piece, DIAGONAL_DIRS, moves)
            'q' -> slide(board, fr, fc, piece, STRAIGHT_DIRS + DIAGONAL_DIRS, moves)
        }
        return moves
    }

    private fun slide(
        board: Array<CharArray>,
        fr: Int,
        fc: Int,
        piece: Char,
        dirs: Array<IntArray>,
        moves: MutableList<Move>
    ) {
        for (dir in dirs) {
            var r = fr + dir[0]
            var c = fc + dir[1]
            while (r in 0..7 && c in 0..7) {
                val target = board[r][c]
                if (target == EMPTY) {
                    moves.add(Move(fr, fc, r, c))
                } else {
                    if (sideOf(target) != sideOf(piece)) moves.add(Move(fr, fc, r, c))
                    break
                }
                r += dir[0]; c += dir[1]
            }
        }
    }

    fun applyMove(board: Array<CharArray>, move: Move): Array<CharArray> {
        val b = cloneBoard(board)
        val piece = b[move.fr][move.fc]
        b[move.fr][move.fc] = EMPTY
        b[move.tr][move.tc] = if (move.promo != EMPTY) move.promo else piece
        if ((piece == 'K' || piece == 'k') && abs(move.tc - move.fc) == 2) {
            if (move.tc == 6) {
                b[move.fr][5] = b[move.fr][7]; b[move.fr][7] = EMPTY
            } else {
                b[move.fr][3] = b[move.fr][0]; b[move.fr][0] = EMPTY
            }
        }
        return b
    }

    fun inCheck(board: Array<CharArray>, white: Boolean): Boolean {
        val target = if (white) 'K' else 'k'
        for (r in 0..7) for (c in 0..7) {
            if (board[r][c] == target) return attacked(board, r, c, !white)
        }
        return false
    }

    /** Fully legal moves for the piece at (fr, fc): filters self-check. */
    fun legalMoves(board: Array<CharArray>, fr: Int, fc: Int): List<Move> {
        val piece = board[fr][fc]
        if (piece == EMPTY) return emptyList()
        return pseudoMoves(board, fr, fc).filter { m ->
            !inCheck(applyMove(board, m), isWhite(piece))
        }
    }

    fun allLegalMoves(board: Array<CharArray>, white: Boolean): List<Move> {
        val out = mutableListOf<Move>()
        for (r in 0..7) for (c in 0..7) {
            val piece = board[r][c]
            if (piece != EMPTY && isWhite(piece) == white) {
                out.addAll(legalMoves(board, r, c))
            }
        }
        return out
    }

    fun hasAnyLegalMove(board: Array<CharArray>, white: Boolean): Boolean {
        for (r in 0..7) for (c in 0..7) {
            val piece = board[r][c]
            if (piece != EMPTY && isWhite(piece) == white && legalMoves(board, r, c).isNotEmpty()) {
                return true
            }
        }
        return false
    }

    /** Material + light central control, white-positive. */
    fun evaluate(board: Array<CharArray>): Int {
        var score = 0
        for (r in 0..7) for (c in 0..7) {
            val piece = board[r][c]
            if (piece == EMPTY) continue
            val white = isWhite(piece)
            val base = when (piece.lowercaseChar()) {
                'p' -> 100
                'n' -> 320
                'b' -> 330
                'r' -> 500
                'q' -> 900
                else -> 0
            }
            val central = if (r in 2..5 && c in 2..5 && piece.lowercaseChar() in "nb") 12 else 0
            score += if (white) base + central else -(base + central)
        }
        return score
    }

    /** Negamax with alpha-beta. Returns score from [white]'s perspective. */
    fun negamax(board: Array<CharArray>, depth: Int, alpha: Int, beta: Int, white: Boolean): Int {
        if (depth == 0) return if (white) evaluate(board) else -evaluate(board)
        val moves = allLegalMoves(board, white)
        if (moves.isEmpty()) {
            return if (inCheck(board, white)) -100_000 - depth else 0
        }
        var a = alpha
        var best = Int.MIN_VALUE
        for (m in moves) {
            val score = -negamax(applyMove(board, m), depth - 1, -beta, -a, !white)
            if (score > best) best = score
            if (best > a) a = best
            if (a >= beta) break
        }
        return best
    }

    fun bestMove(board: Array<CharArray>, white: Boolean, depth: Int = 3): Move? {
        val moves = allLegalMoves(board, white)
        if (moves.isEmpty()) return null
        var best: Move? = null
        var bestScore = Int.MIN_VALUE
        var alpha = Int.MIN_VALUE / 2
        for (m in moves) {
            val score = -negamax(
                applyMove(board, m),
                depth - 1,
                Int.MIN_VALUE / 2,
                -alpha,
                !white
            )
            if (score > bestScore) {
                bestScore = score
                best = m
            }
            if (score > alpha) alpha = score
        }
        return best
    }

    private val KNIGHT_OFFSETS = arrayOf(
        intArrayOf(1, 2), intArrayOf(2, 1), intArrayOf(-1, 2), intArrayOf(-2, 1),
        intArrayOf(1, -2), intArrayOf(2, -1), intArrayOf(-1, -2), intArrayOf(-2, -1)
    )
    private val STRAIGHT_DIRS = arrayOf(
        intArrayOf(0, 1), intArrayOf(0, -1), intArrayOf(1, 0), intArrayOf(-1, 0)
    )
    private val DIAGONAL_DIRS = arrayOf(
        intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1)
    )

    fun glyph(piece: Char): String = when (piece) {
        'P' -> "♙"; 'N' -> "♘"; 'B' -> "♗"; 'R' -> "♖"; 'Q' -> "♕"; 'K' -> "♔"
        'p' -> "♟"; 'n' -> "♞"; 'b' -> "♝"; 'r' -> "♜"; 'q' -> "♛"; 'k' -> "♚"
        else -> ""
    }
}
