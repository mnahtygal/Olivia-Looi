package com.nahtygal.olivialooi.games.tracing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LetterTracingEngineTest {
    private val stroke = listOf(TracingPoint(.2f, .2f), TracingPoint(.5f, .6f))

    private fun session(name: String): LetterTracingState {
        val resolved = requireNotNull(LetterTracingEngine.resolveName(name))
        return LetterTracingEngine.newNameSession(resolved.letters)
    }

    private fun drawCurrent(state: LetterTracingState): LetterTracingState =
        LetterTracingEngine.addStroke(state, stroke)

    @Test
    fun `Olivia and Eliana resolve to their own uppercase name sequences`() {
        assertEquals(OLIVIA_SEQUENCE, LetterTracingEngine.resolveName("Olivia")?.letters)
        assertEquals(ELIANA_SEQUENCE, LetterTracingEngine.resolveName("Eliana")?.letters)
        assertEquals("Olivia", LetterTracingEngine.resolveName(" Olivia ")?.displayName)
    }

    @Test
    fun `missing and invalid names do not substitute another child`() {
        assertNull(LetterTracingEngine.resolveName(null))
        assertNull(LetterTracingEngine.resolveName(""))
        assertNull(LetterTracingEngine.resolveName("Olivia 2"))
    }

    @Test
    fun `all required guides are normalized and repeated letters share guide definitions`() {
        (OLIVIA_SEQUENCE + ELIANA_SEQUENCE).forEach { assertNotNull(LETTER_GUIDES[it]) }
        LETTER_GUIDES.values.flatMap { it.paths }.flatten().forEach { assertTrue(it.isValid) }
        assertSame(LETTER_GUIDES[OLIVIA_SEQUENCE[2]], LETTER_GUIDES[OLIVIA_SEQUENCE[4]])
    }

    @Test
    fun `valid stroke is accepted and invalid coordinates cannot corrupt state`() {
        val valid = drawCurrent(session("Olivia"))
        assertEquals(1, valid.currentStrokes.size)
        listOf(
            TracingPoint(-.01f, .5f),
            TracingPoint(1.01f, .5f),
            TracingPoint(.5f, Float.NaN),
            TracingPoint(Float.POSITIVE_INFINITY, .5f),
        ).forEach { invalid ->
            assertEquals(valid, LetterTracingEngine.addStroke(valid, listOf(invalid)))
            assertFalse(invalid.isValid)
        }
    }

    @Test
    fun `repeated letter positions retain independent strokes`() {
        var state = session("Eliana")
        repeat(3) { state = LetterTracingEngine.next(state) }
        state = drawCurrent(state)
        repeat(2) { state = LetterTracingEngine.next(state) }
        assertEquals('A', state.currentLetter)
        assertTrue(state.currentStrokes.isEmpty())
        state = LetterTracingEngine.addStroke(state, listOf(TracingPoint(.8f, .8f)))
        assertEquals(stroke, state.strokesByPosition[3].single().points)
        assertEquals(TracingPoint(.8f, .8f), state.strokesByPosition[5].single().points.single())
    }

    @Test
    fun `name letter retains multiple strokes without changing repeated letter positions`() {
        var state = session("Eliana")
        repeat(3) { state = drawCurrent(state) }
        repeat(3) { state = LetterTracingEngine.next(state) }
        repeat(2) { state = drawCurrent(state) }
        repeat(2) { state = LetterTracingEngine.next(state) }

        assertTrue(state.currentStrokes.isEmpty())
        assertEquals(3, state.strokesByPosition[0].size)
        assertEquals(2, state.strokesByPosition[3].size)
    }

    @Test
    fun `navigation to final letter does not complete or celebrate`() {
        var state = session("Olivia")
        repeat(10) { state = LetterTracingEngine.next(state) }
        assertEquals(state.letters.lastIndex, state.letterPosition)
        assertFalse(state.isComplete)
        assertFalse(state.celebrationPending)
        assertEquals(state, LetterTracingEngine.next(state))
    }

    @Test
    fun `drawing only final letter does not complete whole name`() {
        var state = session("Olivia")
        repeat(state.letters.lastIndex) { state = LetterTracingEngine.next(state) }
        state = drawCurrent(state)
        assertFalse(state.isComplete)
        assertFalse(state.celebrationPending)
    }

    @Test
    fun `completion requires an accepted stroke at every position`() {
        var state = session("Olivia")
        state.letters.indices.forEach { index ->
            state = drawCurrent(state)
            if (index < state.letters.lastIndex) state = LetterTracingEngine.next(state)
        }
        assertTrue(state.isComplete)
        assertTrue(state.celebrationPending)
    }

    @Test
    fun `completion remains reachable after returning to fill skipped letter`() {
        var state = session("Eliana")
        state.letters.indices.forEach { index ->
            if (index != 2) state = drawCurrent(state)
            if (index < state.letters.lastIndex) state = LetterTracingEngine.next(state)
        }
        assertFalse(state.isComplete)
        repeat(3) { state = LetterTracingEngine.previous(state) }
        state = drawCurrent(state)
        assertEquals(2, state.letterPosition)
        assertTrue(state.isComplete)
        assertTrue(state.celebrationPending)
    }

    @Test
    fun `celebration is consumed once and unrelated changes do not retrigger it`() {
        var state = session("Olivia")
        state.letters.indices.forEach { index ->
            state = drawCurrent(state)
            if (index < state.letters.lastIndex) state = LetterTracingEngine.next(state)
        }
        state = LetterTracingEngine.consumeCelebration(state)
        assertTrue(state.completionCelebrated)
        assertFalse(state.celebrationPending)
        state = LetterTracingEngine.previous(state)
        state = drawCurrent(state)
        state = LetterTracingEngine.next(state)
        assertTrue(state.isComplete)
        assertFalse(state.celebrationPending)
    }

    @Test
    fun `restoring completed strokes does not create a celebration event`() {
        var state = session("Olivia")
        state.letters.indices.forEach { index ->
            state = drawCurrent(state)
            if (index < state.letters.lastIndex) state = LetterTracingEngine.next(state)
        }
        assertTrue(state.celebrationPending)

        val restored = requireNotNull(
            LetterTracingStateCodec.decode(LetterTracingStateCodec.encode(state)),
        )

        assertTrue(restored.isComplete)
        assertTrue(restored.completionCelebrated)
        assertFalse(restored.celebrationPending)
    }

    @Test
    fun `practice again clears every stroke and resets completion at first letter`() {
        var state = session("Olivia")
        state.letters.indices.forEach { index ->
            state = drawCurrent(state)
            if (index < state.letters.lastIndex) state = LetterTracingEngine.next(state)
        }
        state = LetterTracingEngine.consumeCelebration(state)
        val restarted = LetterTracingEngine.practiceAgain(state)
        assertEquals(0, restarted.letterPosition)
        assertEquals(OLIVIA_SEQUENCE, restarted.letters)
        assertEquals(TracingMode.NAME, restarted.mode)
        assertTrue(restarted.strokesByPosition.all(List<TracingStroke>::isEmpty))
        assertFalse(restarted.isComplete)
        assertFalse(restarted.completionCelebrated)
    }

    @Test
    fun `undo and clear remain scoped to current position`() {
        var state = session("Olivia")
        state = drawCurrent(state)
        state = LetterTracingEngine.addStroke(state, listOf(TracingPoint(.7f, .7f)))
        state = LetterTracingEngine.undo(state)
        assertEquals(listOf(TracingStroke(stroke)), state.currentStrokes)
        state = LetterTracingEngine.next(state)
        state = drawCurrent(state)
        state = LetterTracingEngine.clear(state)
        assertTrue(state.currentStrokes.isEmpty())
        assertEquals(1, LetterTracingEngine.previous(state).currentStrokes.size)
    }

    @Test
    fun `codec restores personalized state without creating a celebration event`() {
        var original = drawCurrent(session("Eliana"))
        repeat(4) { original = LetterTracingEngine.next(original) }
        original = drawCurrent(original)
        val restored = LetterTracingStateCodec.decode(LetterTracingStateCodec.encode(original))
        assertEquals(original, restored)
        assertFalse(requireNotNull(restored).celebrationPending)
        assertNull(LetterTracingStateCodec.decode("bad"))
        assertNull(LetterTracingStateCodec.decode("2|ELIANA|0|false|NaN,0~~~~~"))
    }

    @Test
    fun `version 2 name state restores as name mode`() {
        val restored = LetterTracingStateCodec.decode("2|OLIVIA|0|false|~~~~~")

        assertEquals(TracingMode.NAME, restored?.mode)
        assertEquals(OLIVIA_SEQUENCE, restored?.letters)
    }
}
