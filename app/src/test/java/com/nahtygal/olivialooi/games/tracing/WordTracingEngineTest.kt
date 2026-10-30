package com.nahtygal.olivialooi.games.tracing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WordTracingEngineTest {
    private val stroke = listOf(TracingPoint(.25f, .25f), TracingPoint(.75f, .75f))

    private fun session(
        categoryId: String = WordCatalog.THREE_LETTER_ID,
        profileId: String = "olivia",
    ) = WordTracingEngine.newSession(profileId, categoryId)

    private fun draw(state: WordTracingState): WordTracingState = WordTracingEngine.addStroke(state, stroke)

    @Test
    fun `catalog contains exact categories sizes ordering and stable unique ids`() {
        assertEquals(3, WordCatalog.categories.size)
        assertEquals(listOf(3, 4, 5), WordCatalog.categories.map(WordCategory::wordLength))
        assertEquals(30, WordCatalog.allWords.size)
        assertEquals(30, WordCatalog.allWords.map(TraceWord::stableId).toSet().size)
        assertEquals(listOf("CAT", "DOG", "SUN", "HAT", "BUG", "PIG", "COW", "FOX", "BED", "CAR"), values(WordCatalog.THREE_LETTER_ID))
        assertEquals(listOf("FISH", "BIRD", "DUCK", "FROG", "STAR", "MOON", "TREE", "BEAR", "LION", "BOAT"), values(WordCatalog.FOUR_LETTER_ID))
        assertEquals(listOf("APPLE", "HOUSE", "HAPPY", "TIGER", "WATER", "SMILE", "GREEN", "TRAIN", "HEART", "PIZZA"), values(WordCatalog.FIVE_LETTER_ID))
        WordCatalog.categories.forEach { category ->
            assertEquals(10, WordCatalog.words(category.stableId).size)
            assertTrue(WordCatalog.words(category.stableId).all { it.value.length == category.wordLength })
        }
    }

    @Test
    fun `word and letter navigation respects first and last boundaries`() {
        var state = session()
        assertSame(state, WordTracingEngine.previousLetter(state))
        assertSame(state, WordTracingEngine.previousWord(state))
        state = WordTracingEngine.nextLetter(state)
        assertEquals(1, state.letterPosition)
        state = WordTracingEngine.previousLetter(state)
        assertEquals(0, state.letterPosition)
        repeat(20) { state = WordTracingEngine.nextWord(state) }
        assertEquals("CAR", state.currentWord.value)
        assertSame(state, WordTracingEngine.nextWord(state))
        repeat(20) { state = WordTracingEngine.nextLetter(state) }
        assertEquals(2, state.letterPosition)
        assertSame(state, WordTracingEngine.nextLetter(state))
    }

    @Test
    fun `repeated letters have independent positions and retain multiple strokes`() {
        var state = session(WordCatalog.FIVE_LETTER_ID)
        state = WordTracingEngine.selectWord(state, checkNotNull(WordCatalog.allWords.first { it.value == "APPLE" }).stableId)
        state = WordTracingEngine.nextLetter(state)
        state = draw(state)
        state = draw(state)
        state = WordTracingEngine.nextLetter(state)

        assertEquals('P', state.currentLetter)
        assertTrue(state.currentStrokes.isEmpty())
        state = draw(state)
        state = WordTracingEngine.previousLetter(state)
        assertEquals(2, state.currentStrokes.size)
        state = WordTracingEngine.nextLetter(state)
        assertEquals(1, state.currentStrokes.size)
    }

    @Test
    fun `undo and clear affect only the current letter`() {
        var state = draw(session())
        state = draw(state)
        state = WordTracingEngine.nextLetter(state)
        state = draw(state)
        state = draw(state)
        state = WordTracingEngine.undo(state)
        assertEquals(1, state.currentStrokes.size)
        state = WordTracingEngine.clear(state)
        assertTrue(state.currentStrokes.isEmpty())
        state = WordTracingEngine.previousLetter(state)
        assertEquals(2, state.currentStrokes.size)
    }

    @Test
    fun `letter and word navigation preserve independent progress`() {
        var state = draw(session())
        state = WordTracingEngine.nextLetter(state)
        state = draw(state)
        state = WordTracingEngine.nextWord(state)
        assertEquals("DOG", state.currentWord.value)
        assertTrue(state.currentStrokes.isEmpty())
        state = draw(state)
        state = WordTracingEngine.previousWord(state)
        assertEquals("CAT", state.currentWord.value)
        assertEquals(1, state.currentStrokes.size)
        state = WordTracingEngine.nextLetter(state)
        assertEquals(1, state.currentStrokes.size)
    }

    @Test
    fun `categories and child profiles have independent state`() {
        val oliviaThree = draw(session(profileId = "olivia"))
        val elianaThree = session(profileId = "eliana")
        val oliviaFour = session(WordCatalog.FOUR_LETTER_ID, "olivia")

        assertNotEquals(oliviaThree.profileId, elianaThree.profileId)
        assertEquals(1, oliviaThree.currentStrokes.size)
        assertTrue(elianaThree.currentStrokes.isEmpty())
        assertTrue(oliviaFour.currentStrokes.isEmpty())
        assertNotEquals(oliviaThree.categoryId, oliviaFour.categoryId)
    }

    @Test
    fun `completion requires every position and celebration fires once`() {
        var state = session()
        state = traceCurrentLetter(state)
        state = WordTracingEngine.nextLetter(state)
        state = traceCurrentLetter(state)
        assertFalse(state.isCurrentWordComplete)
        assertFalse(state.currentProgress.celebrationPending)
        state = WordTracingEngine.nextLetter(state)
        val tGuide = checkNotNull(LETTER_GUIDES['T'])
        state = WordTracingEngine.addStroke(state, tGuide.paths[0])
        assertFalse(state.isCurrentWordComplete)
        assertFalse(state.currentProgress.celebrationPending)
        repeat(3) { state = WordTracingEngine.addStroke(state, tGuide.paths[0]) }
        assertFalse(state.isCurrentWordComplete)
        state = WordTracingEngine.addStroke(state, tGuide.paths[1])
        assertTrue(state.isCurrentWordComplete)
        assertTrue(state.currentProgress.celebrationPending)

        state = WordTracingEngine.consumeCelebration(state)
        state = draw(state)
        assertTrue(state.currentProgress.completionCelebrated)
        assertFalse(state.currentProgress.celebrationPending)
    }

    @Test
    fun `practice again resets only selected word and enables another completion cycle`() {
        var state = completeCurrentWord(session())
        state = WordTracingEngine.consumeCelebration(state)
        state = WordTracingEngine.nextWord(state)
        state = draw(state)
        state = WordTracingEngine.previousWord(state)
        state = WordTracingEngine.practiceWordAgain(state)

        assertTrue(state.currentProgress.strokesByLetter.all(List<TracingStroke>::isEmpty))
        assertFalse(state.currentProgress.completionCelebrated)
        state = completeCurrentWord(state)
        assertTrue(state.currentProgress.celebrationPending)
        state = WordTracingEngine.nextWord(state)
        assertEquals(1, state.currentStrokes.size)
    }

    @Test
    fun `codec restores stable ids strokes position and handled celebrations`() {
        var state = completeCurrentWord(session())
        state = WordTracingEngine.nextWord(state)
        state = draw(state)
        state = WordTracingEngine.nextLetter(state)
        val restored = requireNotNull(WordTracingStateCodec.decode(WordTracingStateCodec.encode(state)))

        assertEquals(state.profileId, restored.profileId)
        assertEquals(state.categoryId, restored.categoryId)
        assertEquals(state.currentWordId, restored.currentWordId)
        assertEquals(state.letterPosition, restored.letterPosition)
        assertEquals(1, restored.currentProgress.strokesByLetter[0].size)
        val cat = restored.progressByWordId.values.first()
        assertTrue(cat.isComplete(restored.words.first()))
        assertTrue(cat.completionCelebrated)
        assertFalse(cat.celebrationPending)
    }

    @Test
    fun `codec rejects malformed mismatched and unknown state`() {
        assertEquals(null, WordTracingStateCodec.decode("bad"))
        assertEquals(null, WordTracingStateCodec.decode("1|olivia|unknown|word|0|data"))
        val valid = WordTracingStateCodec.encode(session())
        assertEquals(null, WordTracingStateCodec.decode(valid.replace("three-letter-cat", "five-letter-apple")))
    }

    @Test
    fun `hear letter reuses vocabulary and hear word spells repeated letters`() {
        val cat = WordCatalog.allWords.first { it.value == "CAT" }
        val apple = WordCatalog.allWords.first { it.value == "APPLE" }
        assertEquals("This is the letter C. C. C is for Cookie.", LetterVocabulary.hearLetterPhrase(TracingMode.ALPHABET, 'C'))
        assertEquals("Let's write CAT. C, A, T. Cat!", WordTracingSpeech.hearWord(cat))
        assertEquals("Let's write APPLE. A, P, P, L, E. Apple!", WordTracingSpeech.hearWord(apple))
        assertEquals("Hooray, Eliana! You wrote CAT! C, A, T spells cat!", WordTracingSpeech.completion("Eliana", cat))
    }

    @Test
    fun `existing handwriting modes remain distinct from word state`() {
        val name = LetterTracingEngine.newNameSession(OLIVIA_SEQUENCE)
        val uppercase = LetterTracingEngine.newAlphabetSession()
        val lowercase = LetterTracingEngine.newLowercaseSession()
        val words = session()
        assertEquals(TracingMode.NAME, name.mode)
        assertEquals(TracingMode.ALPHABET, uppercase.mode)
        assertEquals(TracingMode.LOWERCASE, lowercase.mode)
        assertEquals("CAT", words.currentWord.value)
        assertTrue(words.currentStrokes.isEmpty())
    }

    @Test
    fun `T requires horizontal and vertical participation with direction awareness`() {
        val guide = checkNotNull(LETTER_GUIDES['T'])
        val topOnly = List(4) { TracingStroke(guide.paths[0]) }
        assertEquals(2, LetterCompletionPolicy.requiredComponentCount('T'))
        assertEquals(setOf(0), LetterCompletionPolicy.participatingComponents('T', topOnly))
        assertFalse(LetterCompletionPolicy.isComplete('T', topOnly))

        val both = topOnly + TracingStroke(guide.paths[1])
        assertEquals(setOf(0, 1), LetterCompletionPolicy.participatingComponents('T', both))
        assertTrue(LetterCompletionPolicy.isComplete('T', both))
    }

    @Test
    fun `undo and clear return a geometrically complete T to incomplete`() {
        var state = session()
        state = WordTracingEngine.nextLetter(WordTracingEngine.nextLetter(state))
        state = traceCurrentLetter(state)
        assertTrue(LetterCompletionPolicy.isComplete('T', state.currentStrokes))

        state = WordTracingEngine.undo(state)
        assertFalse(LetterCompletionPolicy.isComplete('T', state.currentStrokes))
        state = WordTracingEngine.addStroke(state, checkNotNull(LETTER_GUIDES['T']).paths[1])
        assertTrue(LetterCompletionPolicy.isComplete('T', state.currentStrokes))
        state = WordTracingEngine.clear(state)
        assertFalse(LetterCompletionPolicy.isComplete('T', state.currentStrokes))
    }

    @Test
    fun `repeated letter positions retain independent geometric completion`() {
        var state = session(WordCatalog.FIVE_LETTER_ID)
        state = WordTracingEngine.selectWord(state, WordCatalog.allWords.first { it.value == "APPLE" }.stableId)
        state = WordTracingEngine.nextLetter(state)
        state = traceCurrentLetter(state)
        val firstP = state.currentProgress.strokesByLetter[1]
        val secondP = state.currentProgress.strokesByLetter[2]
        assertTrue(LetterCompletionPolicy.isComplete('P', firstP))
        assertFalse(LetterCompletionPolicy.isComplete('P', secondP))
    }

    @Test
    fun `every catalog word can satisfy its actual guide components`() {
        WordCatalog.categories.forEach { category ->
            var state = session(category.stableId)
            WordCatalog.words(category.stableId).forEach { word ->
                state = WordTracingEngine.selectWord(state, word.stableId)
                state = completeCurrentWord(state)
                assertTrue("${word.value} should complete", state.isCurrentWordComplete)
            }
        }
    }

    @Test
    fun `restoration preserves incomplete T components and completes only after vertical`() {
        var state = completeLettersBeforeT(session())
        val tGuide = checkNotNull(LETTER_GUIDES['T'])
        state = WordTracingEngine.addStroke(state, tGuide.paths[0])
        val restored = requireNotNull(WordTracingStateCodec.decode(WordTracingStateCodec.encode(state)))
        assertFalse(restored.isCurrentWordComplete)
        assertFalse(restored.currentProgress.celebrationPending)

        state = WordTracingEngine.addStroke(restored, tGuide.paths[1])
        assertTrue(state.isCurrentWordComplete)
        assertTrue(state.currentProgress.celebrationPending)
        state = WordTracingEngine.consumeCelebration(state)
        state = WordTracingEngine.addStroke(state, tGuide.paths[1])
        assertFalse(state.currentProgress.celebrationPending)
    }

    private fun values(categoryId: String) = WordCatalog.words(categoryId).map(TraceWord::value)

    private fun completeCurrentWord(initial: WordTracingState): WordTracingState {
        var state = initial.copy(letterPosition = 0)
        state.currentWord.value.indices.forEach { index ->
            state = traceCurrentLetter(state)
            if (index < state.currentWord.value.lastIndex) state = WordTracingEngine.nextLetter(state)
        }
        return state
    }

    private fun completeLettersBeforeT(initial: WordTracingState): WordTracingState {
        var state = traceCurrentLetter(initial.copy(letterPosition = 0))
        state = WordTracingEngine.nextLetter(state)
        state = traceCurrentLetter(state)
        return WordTracingEngine.nextLetter(state)
    }

    private fun traceCurrentLetter(initial: WordTracingState): WordTracingState {
        var state = initial
        checkNotNull(LETTER_GUIDES[state.currentLetter]).paths.forEach { component ->
            state = WordTracingEngine.addStroke(state, component)
        }
        return state
    }
}
