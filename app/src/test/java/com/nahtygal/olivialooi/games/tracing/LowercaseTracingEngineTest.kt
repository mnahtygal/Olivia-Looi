package com.nahtygal.olivialooi.games.tracing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LowercaseTracingEngineTest {
    private val stroke = listOf(TracingPoint(.25f, .25f), TracingPoint(.75f, .75f))

    private fun draw(state: LetterTracingState): LetterTracingState =
        LetterTracingEngine.addStroke(state, stroke)

    @Test
    fun `lowercase sequence is exactly a through z with valid normalized guides`() {
        assertEquals(26, LOWERCASE_SEQUENCE.size)
        assertEquals(('a'..'z').toList(), LOWERCASE_SEQUENCE)
        assertEquals(LOWERCASE_SEQUENCE.toSet(), LOWERCASE_LETTER_GUIDES.keys)

        LOWERCASE_SEQUENCE.forEach { letter ->
            val guide = LETTER_GUIDES[letter]
            assertNotNull("Missing lowercase guide for $letter", guide)
            assertEquals(letter, guide!!.letter)
            assertTrue("No paths for $letter", guide.paths.isNotEmpty())
            assertTrue("Invalid path for $letter", guide.paths.all { path ->
                path.size >= 2 && path.all(TracingPoint::isValid)
            })
        }
    }

    @Test
    fun `ascenders rise above x-height and descenders extend below baseline`() {
        val ascenders = listOf('b', 'd', 'f', 'h', 'k', 'l', 't')
        val xHeightLetters = listOf('a', 'c', 'e', 'm', 'n', 'o', 'r', 's', 'u', 'v', 'w', 'x', 'z')
        val descenders = listOf('g', 'j', 'p', 'q', 'y')

        ascenders.forEach { letter ->
            assertTrue("$letter must enter the ascender zone", minY(letter) < .30f)
        }
        xHeightLetters.forEach { letter ->
            assertTrue("$letter must remain at x-height", minY(letter) >= .30f)
        }
        descenders.forEach { letter ->
            assertTrue("$letter must enter the descender zone", maxY(letter) > .80f)
        }
        assertTrue(height('b') > height('a'))
        assertTrue(height('d') > height('o'))
    }

    @Test
    fun `a and g are single-storey forms and i and j use separate dots`() {
        val a = requireNotNull(LOWERCASE_LETTER_GUIDES['a'])
        val g = requireNotNull(LOWERCASE_LETTER_GUIDES['g'])
        assertEquals(2, a.paths.size)
        assertClosed(a.paths.first())
        assertEquals(2, g.paths.size)
        assertClosed(g.paths.first())
        assertTrue(g.paths[1].maxOf(TracingPoint::y) > .80f)

        listOf('i', 'j').forEach { letter ->
            val guide = requireNotNull(LOWERCASE_LETTER_GUIDES[letter])
            assertEquals(2, guide.paths.size)
            val dot = guide.paths.last()
            assertTrue(dot.maxOf(TracingPoint::y) < .30f)
            assertTrue(dot.maxOf(TracingPoint::y) - dot.minOf(TracingPoint::y) <= .03f)
        }
    }

    @Test
    fun `lowercase uppercase and personalized name sessions remain independent`() {
        val lowercase = draw(LetterTracingEngine.newLowercaseSession())
        val uppercase = draw(LetterTracingEngine.newAlphabetSession())
        val name = LetterTracingEngine.newNameSession(ELIANA_SEQUENCE)

        assertEquals(TracingMode.LOWERCASE, lowercase.mode)
        assertEquals(LOWERCASE_SEQUENCE, lowercase.letters)
        assertEquals(TracingMode.ALPHABET, uppercase.mode)
        assertEquals(ALPHABET_SEQUENCE, uppercase.letters)
        assertEquals(TracingMode.NAME, name.mode)
        assertEquals(ELIANA_SEQUENCE, name.letters)
        assertEquals(1, lowercase.currentStrokes.size)
        assertEquals(1, uppercase.currentStrokes.size)
        assertTrue(name.currentStrokes.isEmpty())
    }

    @Test
    fun `multiple lowercase strokes append and undo removes only the latest`() {
        var state = LetterTracingEngine.newLowercaseSession()
        val first = listOf(TracingPoint(.30f, .50f), TracingPoint(.50f, .35f))
        val second = listOf(TracingPoint(.65f, .38f), TracingPoint(.65f, .72f))
        state = LetterTracingEngine.addStroke(state, first)
        state = LetterTracingEngine.addStroke(state, second)

        assertEquals(listOf(first, second), state.currentStrokes.map(TracingStroke::points))
        state = LetterTracingEngine.undo(state)

        assertEquals(listOf(first), state.currentStrokes.map(TracingStroke::points))
    }

    @Test
    fun `clear affects only active lowercase position and navigation preserves strokes`() {
        var state = draw(LetterTracingEngine.newLowercaseSession())
        state = LetterTracingEngine.next(state)
        state = draw(state)
        state = LetterTracingEngine.addStroke(state, listOf(TracingPoint(.4f, .4f)))
        state = LetterTracingEngine.clear(state)

        assertTrue(state.currentStrokes.isEmpty())
        state = LetterTracingEngine.previous(state)
        assertEquals('a', state.currentLetter)
        assertEquals(1, state.currentStrokes.size)
    }

    @Test
    fun `early navigation and drawing only z do not complete lowercase`() {
        var state = LetterTracingEngine.newLowercaseSession()
        repeat(25) { state = LetterTracingEngine.next(state) }
        state = draw(state)

        assertEquals('z', state.currentLetter)
        assertFalse(state.isComplete)
        assertFalse(state.celebrationPending)
    }

    @Test
    fun `lowercase completion requires all positions and skipped letter can finish later`() {
        var state = LetterTracingEngine.newLowercaseSession()
        state.letters.indices.forEach { index ->
            if (index != 8) state = draw(state)
            if (index < state.letters.lastIndex) state = LetterTracingEngine.next(state)
        }
        assertFalse(state.isComplete)
        assertFalse(state.celebrationPending)

        repeat(17) { state = LetterTracingEngine.previous(state) }
        assertEquals('i', state.currentLetter)
        state = draw(state)

        assertTrue(state.isComplete)
        assertTrue(state.celebrationPending)
    }

    @Test
    fun `lowercase completion event and restored celebration are one-shot`() {
        var state = LetterTracingEngine.newLowercaseSession()
        state.letters.indices.forEach { index ->
            state = draw(state)
            if (index < state.letters.lastIndex) state = LetterTracingEngine.next(state)
        }
        state = LetterTracingEngine.consumeCelebration(state)
        state = draw(state)
        assertFalse(state.celebrationPending)

        val restored = requireNotNull(LetterTracingStateCodec.decode(LetterTracingStateCodec.encode(state)))
        assertEquals(TracingMode.LOWERCASE, restored.mode)
        assertTrue(restored.isComplete)
        assertTrue(restored.completionCelebrated)
        assertFalse(restored.celebrationPending)
    }

    @Test
    fun `lowercase codec restores mode position and every stroke and rejects malformed mode`() {
        var state = LetterTracingEngine.newLowercaseSession()
        state = draw(state)
        state = LetterTracingEngine.addStroke(state, listOf(TracingPoint(.4f, .4f)))
        state = LetterTracingEngine.next(state)
        state = draw(state)

        val restored = requireNotNull(LetterTracingStateCodec.decode(LetterTracingStateCodec.encode(state)))

        assertEquals(state, restored)
        assertEquals(TracingMode.LOWERCASE, restored.mode)
        assertEquals(2, restored.strokesByPosition[0].size)
        assertEquals(1, restored.strokesByPosition[1].size)
        assertEquals(null, LetterTracingStateCodec.decode("3|LOWERCASE|abc|0|false|~~"))
        assertEquals(null, LetterTracingStateCodec.decode("3|UNKNOWN|abc|0|false|~~"))
    }

    @Test
    fun `practice lowercase again resets only its own session`() {
        var lowercase = draw(LetterTracingEngine.newLowercaseSession())
        lowercase = LetterTracingEngine.next(lowercase)
        lowercase = draw(lowercase)
        val uppercase = draw(LetterTracingEngine.newAlphabetSession())

        val restarted = LetterTracingEngine.practiceAgain(lowercase)

        assertEquals(TracingMode.LOWERCASE, restarted.mode)
        assertEquals(0, restarted.letterPosition)
        assertTrue(restarted.strokesByPosition.all(List<TracingStroke>::isEmpty))
        assertEquals(1, uppercase.currentStrokes.size)
    }

    private fun points(letter: Char): List<TracingPoint> =
        requireNotNull(LOWERCASE_LETTER_GUIDES[letter]).paths.flatten()

    private fun minY(letter: Char): Float = points(letter).minOf(TracingPoint::y)

    private fun maxY(letter: Char): Float = points(letter).maxOf(TracingPoint::y)

    private fun height(letter: Char): Float = maxY(letter) - minY(letter)

    private fun assertClosed(path: List<TracingPoint>) {
        assertEquals(path.first().x, path.last().x, .001f)
        assertEquals(path.first().y, path.last().y, .001f)
    }
}
