package com.nahtygal.olivialooi.games.tictactoe

enum class TicTacToeMark {
    X,
    O,
}

enum class TicTacToeGameMode {
    PersonVsPerson,
    PersonVsComputer,
}

enum class TicTacToeDifficulty {
    Easy,
    Smart,
}

data class TicTacToeGameState(
    val board: List<TicTacToeMark?> = List(BOARD_CELL_COUNT) { null },
    val currentTurn: TicTacToeMark = TicTacToeMark.X,
    val winner: TicTacToeMark? = null,
    val winningCells: Set<Int> = emptySet(),
    val isDraw: Boolean = false,
) {
    val isGameOver: Boolean
        get() = winner != null || isDraw

    val legalMoves: List<Int>
        get() = if (isGameOver) emptyList() else board.indices.filter { board[it] == null }
}

object TicTacToeEngine {
    val winningCombinations: List<List<Int>> = listOf(
        listOf(0, 1, 2),
        listOf(3, 4, 5),
        listOf(6, 7, 8),
        listOf(0, 3, 6),
        listOf(1, 4, 7),
        listOf(2, 5, 8),
        listOf(0, 4, 8),
        listOf(2, 4, 6),
    )

    fun newGame(): TicTacToeGameState = TicTacToeGameState()

    fun playMove(state: TicTacToeGameState, cellIndex: Int): TicTacToeGameState {
        if (state.isGameOver || cellIndex !in state.board.indices || state.board[cellIndex] != null) {
            return state
        }

        val updatedBoard = state.board.toMutableList().also {
            it[cellIndex] = state.currentTurn
        }
        val winningCells = winningCombination(updatedBoard, state.currentTurn)?.toSet().orEmpty()
        val winner = state.currentTurn.takeIf { winningCells.isNotEmpty() }
        val isDraw = winner == null && updatedBoard.none { it == null }

        return TicTacToeGameState(
            board = updatedBoard,
            currentTurn = if (state.currentTurn == TicTacToeMark.X) {
                TicTacToeMark.O
            } else {
                TicTacToeMark.X
            },
            winner = winner,
            winningCells = winningCells,
            isDraw = isDraw,
        )
    }

    fun winner(board: List<TicTacToeMark?>): TicTacToeMark? =
        TicTacToeMark.entries.firstOrNull { mark -> winningCombination(board, mark) != null }

    private fun winningCombination(
        board: List<TicTacToeMark?>,
        mark: TicTacToeMark,
    ): List<Int>? = winningCombinations.firstOrNull { combination ->
        combination.all { board[it] == mark }
    }
}

const val BOARD_CELL_COUNT = 9
