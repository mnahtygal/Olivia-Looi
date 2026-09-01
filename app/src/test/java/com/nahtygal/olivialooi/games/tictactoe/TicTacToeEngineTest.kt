package com.nahtygal.olivialooi.games.tictactoe

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TicTacToeEngineTest {
    @Test
    fun newBoardIsEmpty() {
        val state = TicTacToeEngine.newGame()

        assertEquals(9, state.board.size)
        assertTrue(state.board.all { it == null })
        assertEquals(TicTacToeMark.X, state.currentTurn)
        assertFalse(state.isGameOver)
    }

    @Test
    fun validMovePlacesCurrentMark() {
        val state = TicTacToeEngine.playMove(TicTacToeEngine.newGame(), 4)

        assertEquals(TicTacToeMark.X, state.board[4])
    }

    @Test
    fun occupiedCellCannotBeOverwritten() {
        val afterFirstMove = TicTacToeEngine.playMove(TicTacToeEngine.newGame(), 4)

        val rejectedMove = TicTacToeEngine.playMove(afterFirstMove, 4)

        assertSame(afterFirstMove, rejectedMove)
        assertEquals(TicTacToeMark.X, rejectedMove.board[4])
        assertEquals(TicTacToeMark.O, rejectedMove.currentTurn)
    }

    @Test
    fun turnsAlternate() {
        val afterX = TicTacToeEngine.playMove(TicTacToeEngine.newGame(), 0)
        val afterO = TicTacToeEngine.playMove(afterX, 1)

        assertEquals(TicTacToeMark.O, afterX.currentTurn)
        assertEquals(TicTacToeMark.X, afterO.currentTurn)
    }

    @Test
    fun detectsAllEightWinningCombinations() {
        TicTacToeEngine.winningCombinations.forEach { combination ->
            val state = playXWinningCombination(combination)

            assertEquals("Combination $combination", TicTacToeMark.X, state.winner)
            assertEquals(combination.toSet(), state.winningCells)
            assertTrue(state.isGameOver)
        }
    }

    @Test
    fun detectsDraw() {
        val state = playMoves(0, 1, 2, 4, 3, 5, 7, 6, 8)

        assertTrue(state.isDraw)
        assertNull(state.winner)
        assertTrue(state.isGameOver)
    }

    @Test
    fun rejectsMovesAfterWin() {
        val won = playMoves(0, 3, 1, 4, 2)

        val rejectedMove = TicTacToeEngine.playMove(won, 8)

        assertSame(won, rejectedMove)
        assertNull(rejectedMove.board[8])
    }

    @Test
    fun easyAiReturnsLegalMove() {
        val state = playMoves(0, 4, 8)

        val move = TicTacToeAi.easyMove(state, Random(7))

        assertNotNull(move)
        assertTrue(move in state.legalMoves)
    }

    @Test
    fun smartAiReturnsLegalMove() {
        val state = playMoves(0)

        val move = TicTacToeAi.smartMove(state, TicTacToeMark.O)

        assertNotNull(move)
        assertTrue(move in state.legalMoves)
    }

    @Test
    fun smartAiTakesImmediateWin() {
        val state = playMoves(0, 3, 1, 4, 8)

        assertEquals(5, TicTacToeAi.smartMove(state, TicTacToeMark.O))
    }

    @Test
    fun smartAiBlocksImmediateOpponentWin() {
        val state = playMoves(0, 4, 1)

        assertEquals(2, TicTacToeAi.smartMove(state, TicTacToeMark.O))
    }

    @Test
    fun smartAiCannotLoseFromInitialBoard() {
        assertFalse(canOliviaForceWin(TicTacToeEngine.newGame()))
    }

    private fun canOliviaForceWin(state: TicTacToeGameState): Boolean {
        if (state.winner == TicTacToeMark.X) return true
        if (state.isGameOver) return false

        return if (state.currentTurn == TicTacToeMark.X) {
            state.legalMoves.any { move ->
                canOliviaForceWin(TicTacToeEngine.playMove(state, move))
            }
        } else {
            val move = TicTacToeAi.smartMove(state, TicTacToeMark.O)
                ?: return false
            canOliviaForceWin(TicTacToeEngine.playMove(state, move))
        }
    }

    private fun playXWinningCombination(combination: List<Int>): TicTacToeGameState {
        val fillerMoves = (0 until BOARD_CELL_COUNT).filterNot(combination::contains).iterator()
        var state = TicTacToeEngine.newGame()
        combination.forEachIndexed { index, move ->
            state = TicTacToeEngine.playMove(state, move)
            if (index < combination.lastIndex) {
                state = TicTacToeEngine.playMove(state, fillerMoves.next())
            }
        }
        return state
    }

    private fun playMoves(vararg moves: Int): TicTacToeGameState =
        moves.fold(TicTacToeEngine.newGame(), TicTacToeEngine::playMove)
}
