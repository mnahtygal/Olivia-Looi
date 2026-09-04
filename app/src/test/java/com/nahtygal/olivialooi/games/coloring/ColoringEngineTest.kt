package com.nahtygal.olivialooi.games.coloring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ColoringEngineTest {
    @Test
    fun newPictureStartsCleanWithMediumBrush() {
        val state = ColoringEngine.newPicture(ColoringPicture.Butterfly)

        assertTrue(state.strokes.isEmpty())
        assertNull(state.activeStroke)
        assertEquals(ColoringBrush.Medium, state.selectedBrush)
    }

    @Test
    fun startedStrokeStoresSelectedColorAndBrush() {
        var state = ColoringEngine.newPicture(ColoringPicture.Flower)
        state = ColoringEngine.selectColor(state, ColoringColor.Pink)
        state = ColoringEngine.selectBrush(state, ColoringBrush.Big)
        state = ColoringEngine.startStroke(state, point(0.2f, 0.3f))

        assertEquals(ColoringColor.Pink, state.activeStroke?.color)
        assertEquals(ColoringBrush.Big, state.activeStroke?.brush)
    }

    @Test
    fun appendedPointsRemainInOrder() {
        val points = listOf(point(0.1f, 0.2f), point(0.3f, 0.4f), point(0.5f, 0.6f))
        var state = ColoringEngine.startStroke(newState(), points.first())
        points.drop(1).forEach { state = ColoringEngine.appendPoint(state, it) }

        assertEquals(points, state.activeStroke?.points)
    }

    @Test
    fun finishingStrokeMovesItToHistory() {
        var state = ColoringEngine.startStroke(newState(), point(0.1f, 0.2f))
        state = ColoringEngine.appendPoint(state, point(0.2f, 0.3f))
        state = ColoringEngine.finishStroke(state)

        assertEquals(1, state.strokes.size)
        assertEquals(2, state.strokes.single().points.size)
        assertNull(state.activeStroke)
    }

    @Test
    fun undoRemovesOnlyLatestCompletedStroke() {
        val first = listOf(point(0.1f, 0.1f))
        val second = listOf(point(0.2f, 0.2f))
        var state = ColoringEngine.addCompletedStroke(newState(), first)
        state = ColoringEngine.addCompletedStroke(state, second)

        val undone = ColoringEngine.undo(state)

        assertEquals(1, undone.strokes.size)
        assertEquals(first, undone.strokes.single().points)
    }

    @Test
    fun multipleUndoWorksUntilHistoryIsEmpty() {
        var state = newState()
        repeat(3) { index ->
            state = ColoringEngine.addCompletedStroke(state, listOf(point(index / 10f, 0.5f)))
        }

        repeat(3) { state = ColoringEngine.undo(state) }

        assertTrue(state.strokes.isEmpty())
        assertSame(state, ColoringEngine.undo(state))
    }

    @Test
    fun clearRemovesAllStrokesAndIsSafeWhenEmpty() {
        var state = ColoringEngine.addCompletedStroke(newState(), listOf(point(0.4f, 0.4f)))
        state = ColoringEngine.startStroke(state, point(0.5f, 0.5f))

        val cleared = ColoringEngine.clear(state)

        assertTrue(cleared.strokes.isEmpty())
        assertNull(cleared.activeStroke)
        assertSame(cleared, ColoringEngine.clear(cleared))
    }

    @Test
    fun changingColorAffectsNewStrokeButNotPreviousStroke() {
        var state = ColoringEngine.selectColor(newState(), ColoringColor.Red)
        state = ColoringEngine.addCompletedStroke(state, listOf(point(0.1f, 0.1f)))
        state = ColoringEngine.selectColor(state, ColoringColor.Blue)
        state = ColoringEngine.addCompletedStroke(state, listOf(point(0.2f, 0.2f)))

        assertEquals(listOf(ColoringColor.Red, ColoringColor.Blue), state.strokes.map { it.color })
    }

    @Test
    fun changingBrushAffectsNewStrokeButNotPreviousStroke() {
        var state = ColoringEngine.selectBrush(newState(), ColoringBrush.Small)
        state = ColoringEngine.addCompletedStroke(state, listOf(point(0.1f, 0.1f)))
        state = ColoringEngine.selectBrush(state, ColoringBrush.Big)
        state = ColoringEngine.addCompletedStroke(state, listOf(point(0.2f, 0.2f)))

        assertEquals(listOf(ColoringBrush.Small, ColoringBrush.Big), state.strokes.map { it.brush })
    }

    @Test
    fun pictureIdentityIsRetainedAcrossDrawingOperations() {
        var state = ColoringEngine.newPicture(ColoringPicture.Snowman)
        state = ColoringEngine.addCompletedStroke(state, listOf(point(0.5f, 0.5f)))
        state = ColoringEngine.undo(state)

        assertEquals(ColoringPicture.Snowman, state.picture)
    }

    @Test
    fun resetCreatesCleanStateForSamePicture() {
        var state = ColoringEngine.newPicture(ColoringPicture.Puppy)
        state = ColoringEngine.selectColor(state, ColoringColor.Orange)
        state = ColoringEngine.selectBrush(state, ColoringBrush.Big)
        state = ColoringEngine.addCompletedStroke(state, listOf(point(0.2f, 0.2f)))

        val reset = ColoringEngine.resetPicture(state)

        assertEquals(ColoringPicture.Puppy, reset.picture)
        assertTrue(reset.strokes.isEmpty())
        assertEquals(ColoringColor.Purple, reset.selectedColor)
        assertEquals(ColoringBrush.Medium, reset.selectedBrush)
    }

    @Test
    fun invalidAndEmptyStrokesAreIgnoredSafely() {
        val state = newState()

        assertSame(state, ColoringEngine.finishStroke(state))
        assertSame(state, ColoringEngine.addCompletedStroke(state, emptyList()))
        assertSame(state, ColoringEngine.startStroke(state, point(-0.1f, 0.5f)))
        assertSame(state, ColoringEngine.startStroke(state, point(Float.NaN, 0.5f)))
    }

    @Test
    fun invalidAppendedPointDoesNotDamageActiveStroke() {
        val active = ColoringEngine.startStroke(newState(), point(0.5f, 0.5f))

        assertSame(active, ColoringEngine.appendPoint(active, point(1.1f, 0.5f)))
        assertEquals(listOf(point(0.5f, 0.5f)), active.activeStroke?.points)
    }

    @Test
    fun secondStartIsIgnoredWhileStrokeIsActive() {
        val active = ColoringEngine.startStroke(newState(), point(0.1f, 0.1f))

        assertSame(active, ColoringEngine.startStroke(active, point(0.9f, 0.9f)))
    }

    @Test
    fun undoCancelsActiveStrokeBeforeCompletedHistory() {
        var state = ColoringEngine.addCompletedStroke(newState(), listOf(point(0.1f, 0.1f)))
        state = ColoringEngine.startStroke(state, point(0.2f, 0.2f))

        val undone = ColoringEngine.undo(state)

        assertNull(undone.activeStroke)
        assertEquals(1, undone.strokes.size)
    }

    private fun newState(): ColoringState = ColoringEngine.newPicture(ColoringPicture.Butterfly)

    private fun point(x: Float, y: Float): ColoringPoint = ColoringPoint(x, y)
}
