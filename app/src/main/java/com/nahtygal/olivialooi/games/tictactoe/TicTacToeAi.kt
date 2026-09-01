package com.nahtygal.olivialooi.games.tictactoe

import kotlin.random.Random

object TicTacToeAi {
    fun easyMove(
        state: TicTacToeGameState,
        random: Random = Random.Default,
    ): Int? = state.legalMoves.randomOrNull(random)

    fun smartMove(
        state: TicTacToeGameState,
        aiMark: TicTacToeMark = state.currentTurn,
    ): Int? {
        if (state.isGameOver || state.currentTurn != aiMark) return null

        val humanMark = aiMark.other()
        var bestScore = Int.MIN_VALUE
        var bestMove: Int? = null

        for (move in state.legalMoves) {
            val board = state.board.toMutableList().also { it[move] = aiMark }
            val score = minimax(
                board = board,
                turn = humanMark,
                aiMark = aiMark,
                humanMark = humanMark,
                depth = 1,
                alpha = Int.MIN_VALUE,
                beta = Int.MAX_VALUE,
            )
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        return bestMove
    }

    private fun minimax(
        board: MutableList<TicTacToeMark?>,
        turn: TicTacToeMark,
        aiMark: TicTacToeMark,
        humanMark: TicTacToeMark,
        depth: Int,
        alpha: Int,
        beta: Int,
    ): Int {
        TicTacToeEngine.winner(board)?.let { winner ->
            return if (winner == aiMark) 10 - depth else depth - 10
        }
        if (board.none { it == null }) return 0

        var currentAlpha = alpha
        var currentBeta = beta
        if (turn == aiMark) {
            var bestScore = Int.MIN_VALUE
            for (move in board.indices.filter { board[it] == null }) {
                board[move] = turn
                bestScore = maxOf(
                    bestScore,
                    minimax(
                        board,
                        humanMark,
                        aiMark,
                        humanMark,
                        depth + 1,
                        currentAlpha,
                        currentBeta,
                    ),
                )
                board[move] = null
                currentAlpha = maxOf(currentAlpha, bestScore)
                if (currentBeta <= currentAlpha) break
            }
            return bestScore
        }

        var bestScore = Int.MAX_VALUE
        for (move in board.indices.filter { board[it] == null }) {
            board[move] = turn
            bestScore = minOf(
                bestScore,
                minimax(
                    board,
                    aiMark,
                    aiMark,
                    humanMark,
                    depth + 1,
                    currentAlpha,
                    currentBeta,
                ),
            )
            board[move] = null
            currentBeta = minOf(currentBeta, bestScore)
            if (currentBeta <= currentAlpha) break
        }
        return bestScore
    }

    private fun TicTacToeMark.other(): TicTacToeMark =
        if (this == TicTacToeMark.X) TicTacToeMark.O else TicTacToeMark.X
}
