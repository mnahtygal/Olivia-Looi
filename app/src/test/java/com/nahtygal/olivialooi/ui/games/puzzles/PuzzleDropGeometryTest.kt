package com.nahtygal.olivialooi.ui.games.puzzles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class PuzzleDropGeometryTest {
    private val board = Rect(40f, 80f, 340f, 380f)
    @Test fun `center inside correct cell snaps`() {
        assertEquals(4, puzzleDropCell(Offset(190f, 230f), Size(80f, 80f), board, 3, 3, 4))
    }
    @Test fun `clear overlap snaps with center just outside`() {
        assertEquals(4, puzzleDropCell(Offset(245f, 230f), Size(80f, 80f), board, 3, 3, 4))
    }
    @Test fun `tiny overlap does not reveal correct target`() {
        assertEquals(5, puzzleDropCell(Offset(275f, 230f), Size(80f, 80f), board, 3, 3, 4))
    }
    @Test fun `wrong cell evaluated as wrong cell`() {
        assertEquals(0, puzzleDropCell(Offset(80f, 120f), Size(80f, 80f), board, 3, 3, 8))
    }
    @Test fun `outside board rejected`() {
        assertEquals(-1, puzzleDropCell(Offset(400f, 500f), Size(80f, 80f), board, 3, 3, 4))
    }
    @Test fun `unmeasured board rejected`() {
        assertEquals(-1, puzzleDropCell(Offset.Zero, Size(80f, 80f), Rect.Zero, 3, 3, 0))
    }
    @Test fun `non square cells support all difficulties`() {
        listOf(2 to 3, 3 to 3, 3 to 4).forEach { (columns, rows) ->
            repeat(columns * rows) { target ->
                val center = Offset(board.left + (target % columns + .5f) * board.width / columns, board.top + (target / columns + .5f) * board.height / rows)
                assertEquals(target, puzzleDropCell(center, Size(80f, 60f), board, rows, columns, target))
            }
        }
    }
    @Test fun `offset board uses root coordinates`() {
        assertEquals(0, puzzleDropCell(Offset(550f, 950f), Size(100f, 100f), Rect(500f, 900f, 800f, 1200f), 3, 3, 0))
    }
}
