package com.nahtygal.olivialooi.ui.games.puzzles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/** UI-only hit testing in root coordinates; engine receives only the resulting cell ID.
 * Accept the correct cell when it contains the tile center, or overlaps at least 30%
 * of the smaller tile/cell area. This is generous without accepting a neighboring tile
 * that only touches the correct cell's edge. No answer location is displayed.
 */
internal fun puzzleDropCell(
    center: Offset,
    draggedSize: Size,
    board: Rect,
    rows: Int,
    columns: Int,
    correctTarget: Int,
): Int {
    if (board.width <= 0f || board.height <= 0f) return -1
    val cellWidth = board.width / columns
    val cellHeight = board.height / rows
    val left = board.left + correctTarget % columns * cellWidth
    val top = board.top + correctTarget / columns * cellHeight
    val correct = Rect(left, top, left + cellWidth, top + cellHeight)
    val dragged = Rect(center - Offset(draggedSize.width / 2, draggedSize.height / 2), draggedSize)
    val overlap = correct.intersect(dragged)
    val smallerArea = minOf(cellWidth * cellHeight, draggedSize.width * draggedSize.height)
    if (correct.contains(center) || (smallerArea > 0 && overlap.width > 0 && overlap.height > 0 && overlap.width * overlap.height >= smallerArea * .30f)) {
        return correctTarget
    }
    if (!board.contains(center)) return -1
    return ((center.y - board.top) / cellHeight).toInt().coerceIn(0, rows - 1) * columns +
        ((center.x - board.left) / cellWidth).toInt().coerceIn(0, columns - 1)
}
