package com.nahtygal.olivialooi.games.tracing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlphabetTracingEngineTest {
    private val stroke = listOf(TracingPoint(.25f, .25f), TracingPoint(.75f, .75f))

    private fun draw(state: LetterTracingState): LetterTracingState = LetterTracingEngine.addStroke(state, stroke)

    @Test
    fun `first stroke remains after second and three gestures retain three strokes`() {
        var state = LetterTracingEngine.newAlphabetSession()
        val first = listOf(TracingPoint(.20f, .80f), TracingPoint(.50f, .20f))
        val second = listOf(TracingPoint(.50f, .20f), TracingPoint(.80f, .80f))
        val third = listOf(TracingPoint(.35f, .55f), TracingPoint(.65f, .55f))

        state = LetterTracingEngine.addStroke(state, first)
        state = LetterTracingEngine.addStroke(state, second)
        assertEquals(listOf(first, second), state.currentStrokes.map(TracingStroke::points))
        state = LetterTracingEngine.addStroke(state, third)

        assertEquals(listOf(first, second, third), state.currentStrokes.map(TracingStroke::points))
        assertEquals(3, state.currentStrokes.size)
    }

    @Test
    fun `undo removes latest of three strokes and clear removes remaining current-letter strokes`() {
        var state = LetterTracingEngine.newAlphabetSession()
        repeat(3) { index ->
            state = LetterTracingEngine.addStroke(
                state,
                listOf(TracingPoint(.1f + index * .1f, .2f), TracingPoint(.8f, .7f)),
            )
        }

        state = LetterTracingEngine.undo(state)
        assertEquals(2, state.currentStrokes.size)
        state = LetterTracingEngine.clear(state)

        assertTrue(state.currentStrokes.isEmpty())
    }

    @Test
    fun `all strokes survive A to B to A navigation`() {
        var state = LetterTracingEngine.newAlphabetSession()
        repeat(3) { state = draw(state) }
        state = LetterTracingEngine.next(state)
        state = draw(state)
        state = LetterTracingEngine.previous(state)

        assertEquals(3, state.currentStrokes.size)
        assertEquals(1, state.strokesByPosition[1].size)
    }

    @Test
    fun `codec restores every completed stroke on a position`() {
        var state = LetterTracingEngine.newAlphabetSession()
        repeat(3) { state = draw(state) }

        val restored = requireNotNull(LetterTracingStateCodec.decode(LetterTracingStateCodec.encode(state)))

        assertEquals(state, restored)
        assertEquals(3, restored.currentStrokes.size)
    }

    @Test
    fun `alphabet contains exactly A through Z and every letter has a vector guide`() {
        assertEquals(26, ALPHABET_SEQUENCE.size)
        assertEquals(('A'..'Z').toList(), ALPHABET_SEQUENCE)
        ALPHABET_SEQUENCE.forEach { letter ->
            val guide = LETTER_GUIDES[letter]
            assertNotNull("Missing guide for $letter", guide)
            assertTrue(guide!!.paths.isNotEmpty())
            assertTrue(guide.paths.all { path -> path.size >= 2 && path.all(TracingPoint::isValid) })
        }
    }

    @Test
    fun `name and alphabet sessions are independent`() {
        val name = draw(LetterTracingEngine.newNameSession(OLIVIA_SEQUENCE))
        val alphabet = LetterTracingEngine.newAlphabetSession()

        assertEquals(TracingMode.NAME, name.mode)
        assertEquals(TracingMode.ALPHABET, alphabet.mode)
        assertEquals(1, name.currentStrokes.size)
        assertTrue(alphabet.currentStrokes.isEmpty())
    }

    @Test
    fun `previous next undo and clear preserve position-specific alphabet drawing`() {
        var state = draw(LetterTracingEngine.newAlphabetSession())
        state = LetterTracingEngine.next(state)
        state = draw(state)
        state = LetterTracingEngine.addStroke(state, listOf(TracingPoint(.4f, .4f)))
        state = LetterTracingEngine.undo(state)
        assertEquals(1, state.currentStrokes.size)
        state = LetterTracingEngine.clear(state)
        assertTrue(state.currentStrokes.isEmpty())
        state = LetterTracingEngine.previous(state)
        assertEquals('A', state.currentLetter)
        assertEquals(1, state.currentStrokes.size)
    }

    @Test
    fun `navigating to Z does not complete alphabet`() {
        var state = LetterTracingEngine.newAlphabetSession()
        repeat(30) { state = LetterTracingEngine.next(state) }

        assertEquals('Z', state.currentLetter)
        assertFalse(state.isComplete)
        assertFalse(state.celebrationPending)
    }

    @Test
    fun `drawing only Z does not complete alphabet`() {
        var state = LetterTracingEngine.newAlphabetSession()
        repeat(25) { state = LetterTracingEngine.next(state) }
        state = draw(state)

        assertFalse(state.isComplete)
        assertFalse(state.celebrationPending)
    }

    @Test
    fun `alphabet completion requires strokes on all 26 positions`() {
        var state = LetterTracingEngine.newAlphabetSession()
        state.letters.indices.forEach { index ->
            state = draw(state)
            if (index < state.letters.lastIndex) state = LetterTracingEngine.next(state)
        }

        assertTrue(state.isComplete)
        assertTrue(state.celebrationPending)
    }

    @Test
    fun `skipped alphabet letter can complete later and celebration fires once`() {
        var state = LetterTracingEngine.newAlphabetSession()
        state.letters.indices.forEach { index ->
            if (index != 7) state = draw(state)
            if (index < state.letters.lastIndex) state = LetterTracingEngine.next(state)
        }
        repeat(18) { state = LetterTracingEngine.previous(state) }
        assertEquals('H', state.currentLetter)

        state = draw(state)
        assertTrue(state.celebrationPending)
        state = LetterTracingEngine.consumeCelebration(state)
        state = draw(state)

        assertTrue(state.isComplete)
        assertFalse(state.celebrationPending)
    }

    @Test
    fun `restoring completed alphabet does not replay celebration`() {
        var state = LetterTracingEngine.newAlphabetSession()
        state.letters.indices.forEach { index ->
            state = draw(state)
            if (index < state.letters.lastIndex) state = LetterTracingEngine.next(state)
        }

        val restored = requireNotNull(LetterTracingStateCodec.decode(LetterTracingStateCodec.encode(state)))

        assertEquals(TracingMode.ALPHABET, restored.mode)
        assertTrue(restored.isComplete)
        assertTrue(restored.completionCelebrated)
        assertFalse(restored.celebrationPending)
    }

    @Test
    fun `practice ABCs again resets only an alphabet session`() {
        var alphabet = draw(LetterTracingEngine.newAlphabetSession())
        alphabet = LetterTracingEngine.next(alphabet)
        alphabet = draw(alphabet)
        val name = draw(LetterTracingEngine.newNameSession(ELIANA_SEQUENCE))

        val restarted = LetterTracingEngine.practiceAgain(alphabet)

        assertEquals(TracingMode.ALPHABET, restarted.mode)
        assertEquals(0, restarted.letterPosition)
        assertTrue(restarted.strokesByPosition.all(List<TracingStroke>::isEmpty))
        assertEquals(1, name.currentStrokes.size)
    }

    @Test
    fun `fresh name session after alphabet keeps personalization and no alphabet strokes`() {
        val alphabet = draw(LetterTracingEngine.newAlphabetSession())
        val freshName = LetterTracingEngine.newNameSession(ELIANA_SEQUENCE)

        assertEquals(ELIANA_SEQUENCE, freshName.letters)
        assertEquals(TracingMode.NAME, freshName.mode)
        assertTrue(freshName.strokesByPosition.all(List<TracingStroke>::isEmpty))
        assertEquals(1, alphabet.currentStrokes.size)
    }
}
