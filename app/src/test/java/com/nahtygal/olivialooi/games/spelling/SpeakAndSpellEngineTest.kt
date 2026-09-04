package com.nahtygal.olivialooi.games.spelling

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakAndSpellEngineTest {
    @Test
    fun levelOneContainsOnlyExpectedThreeLetterWords() {
        assertEquals(
            setOf("CAT", "DOG", "COW", "PIG", "SUN", "CAR", "RED", "BED", "HAT", "BUG"),
            words(SpellingLevel.Level1),
        )
        assertTrue(words(SpellingLevel.Level1).all { it.length == 3 })
    }

    @Test
    fun levelTwoContainsExpectedFourLetterWords() {
        assertEquals(
            setOf("STAR", "BLUE", "PINK", "DUCK", "FISH", "FROG", "BIRD", "MOON", "BOOK", "TREE"),
            words(SpellingLevel.Level2),
        )
        assertTrue(words(SpellingLevel.Level2).all { it.length == 4 })
    }

    @Test
    fun levelThreeContainsExpectedMixedWords() {
        assertEquals(
            setOf("APPLE", "HOUSE", "PUPPY", "FLOWER", "HAPPY", "GREEN", "WATER", "HORSE", "HEART", "LOOLOO"),
            words(SpellingLevel.Level3),
        )
        assertTrue(words(SpellingLevel.Level3).map(String::length).toSet().size > 1)
    }

    @Test
    fun newRoundCreatesOneAnswerSlotPerLetter() {
        SpellingLevel.entries.forEach { level ->
            val state = SpeakAndSpellEngine.newGame(level, Random(4))
            assertEquals(state.currentWord.text.length, state.answerSlots.size)
            assertTrue(state.answerSlots.all { it == null })
        }
    }

    @Test
    fun letterBankContainsEveryRequiredLetterAndDistractors() {
        SpellingLevel.entries.forEach { level ->
            val state = SpeakAndSpellEngine.newGame(level, Random(7))
            val requiredCounts = state.currentWord.text.groupingBy { it }.eachCount()
            val bankCounts = state.letterBank.map(LetterTile::letter).groupingBy { it }.eachCount()
            requiredCounts.forEach { (letter, count) ->
                assertTrue(bankCounts.getValue(letter) >= count)
            }
            assertTrue(state.letterBank.any { it.letter !in state.currentWord.text })
        }
    }

    @Test
    fun injectedRandomnessMakesRoundDeterministic() {
        val first = SpeakAndSpellEngine.newGame(SpellingLevel.Level3, Random(123))
        val second = SpeakAndSpellEngine.newGame(SpellingLevel.Level3, Random(123))

        assertEquals(first, second)
        assertNotEquals(
            first.letterBank.map(LetterTile::letter),
            SpeakAndSpellEngine.newGame(SpellingLevel.Level3, Random(456)).letterBank.map(LetterTile::letter),
        )
    }

    @Test
    fun selectingTileFillsNextSlotAndConsumesOnlyThatTile() {
        val state = stateFor("CAT")
        val tile = state.letterBank.first { it.letter == 'C' }
        val selected = SpeakAndSpellEngine.selectTile(state, tile.id)

        assertEquals(listOf('C'), selected.enteredLetters)
        assertFalse(selected.letterBank.first { it.id == tile.id }.isAvailable)
        assertTrue(selected.letterBank.filter { it.id != tile.id }.all(LetterTile::isAvailable))
    }

    @Test
    fun backspaceRemovesLatestLetterAndRestoresExactTile() {
        val state = stateFor("CAT")
        val cTile = state.letterBank.first { it.letter == 'C' }
        val aTile = state.letterBank.first { it.letter == 'A' }
        var entered = SpeakAndSpellEngine.selectTile(state, cTile.id)
        entered = SpeakAndSpellEngine.selectTile(entered, aTile.id)

        val backedUp = SpeakAndSpellEngine.backspace(entered)

        assertEquals(listOf(cTile.id), backedUp.enteredTileIds)
        assertTrue(backedUp.letterBank.first { it.id == aTile.id }.isAvailable)
        assertFalse(backedUp.letterBank.first { it.id == cTile.id }.isAvailable)
    }

    @Test
    fun backspaceOnEmptyAnswerIsSafe() {
        val state = stateFor("CAT")
        assertSame(state, SpeakAndSpellEngine.backspace(state))
    }

    @Test
    fun duplicateLettersHaveUniqueTileIds() {
        listOf("APPLE", "LOOLOO", "GREEN").forEach { word ->
            val state = stateFor(word)
            assertEquals(state.letterBank.size, state.letterBank.map(LetterTile::id).distinct().size)
            word.toSet().forEach { letter ->
                assertEquals(
                    word.count { it == letter },
                    state.letterBank.count { it.letter == letter },
                )
            }
        }
    }

    @Test
    fun appleConsumesTwoIndependentPTiles() {
        val completed = spellCorrectly(stateFor("APPLE"))
        val pTiles = completed.letterBank.filter { it.letter == 'P' }

        assertEquals(2, pTiles.size)
        assertTrue(pTiles.all { !it.isAvailable })
        assertEquals(2, completed.enteredTileIds.map { id -> completed.tile(id).letter }.count { it == 'P' })
    }

    @Test
    fun loolooConsumesAllDuplicateTilesIndependently() {
        val completed = spellCorrectly(stateFor("LOOLOO"))

        assertEquals("LOOLOO", completed.enteredLetters.joinToString(""))
        assertEquals(6, completed.enteredTileIds.distinct().size)
        assertTrue(completed.enteredTileIds.all { id -> !completed.tile(id).isAvailable })
    }

    @Test
    fun completedAnswersAreRecognizedAsCorrectOrIncorrect() {
        val correct = spellCorrectly(stateFor("CAT"))
        val incorrect = spellIncorrectly(stateFor("CAT"))

        assertEquals(SpellingRoundState.Correct, correct.roundState)
        assertEquals(SpellingRoundState.Incorrect, incorrect.roundState)
    }

    @Test
    fun incompleteAnswerIsNotEvaluated() {
        val state = stateFor("CAT")
        val tile = state.letterBank.first { it.letter == 'C' }

        assertEquals(
            SpellingRoundState.Playing,
            SpeakAndSpellEngine.selectTile(state, tile.id).roundState,
        )
    }

    @Test
    fun incorrectResetClearsAnswerAndRestoresEntireBank() {
        val incorrect = spellIncorrectly(stateFor("CAT"))
        val reset = SpeakAndSpellEngine.resetIncorrectAnswer(incorrect)

        assertEquals(SpellingRoundState.Playing, reset.roundState)
        assertTrue(reset.enteredTileIds.isEmpty())
        assertTrue(reset.letterBank.all(LetterTile::isAvailable))
    }

    @Test
    fun helpCountAndFirstLetterHintWorkWithoutFillingAnswer() {
        val state = stateFor("DOG")
        val firstHelp = SpeakAndSpellEngine.requestHelp(state)
        val secondHelp = SpeakAndSpellEngine.requestHelp(firstHelp)
        val thirdHelp = SpeakAndSpellEngine.requestHelp(secondHelp)

        assertEquals(1, firstHelp.helpCount)
        assertEquals(null, firstHelp.firstLetterHint)
        assertEquals('D', secondHelp.firstLetterHint)
        assertEquals(3, thirdHelp.helpCount)
        assertTrue(thirdHelp.enteredTileIds.isEmpty())
    }

    @Test
    fun nextWordResetsHelpAndAvoidsImmediateRepeat() {
        var state = SpeakAndSpellEngine.newGame(SpellingLevel.Level1, Random(2))
        state = SpeakAndSpellEngine.requestHelp(SpeakAndSpellEngine.requestHelp(state))
        val previous = state.currentWord
        state = spellCorrectly(state)
        val next = SpeakAndSpellEngine.nextWord(state, Random(2))

        assertEquals(0, next.helpCount)
        assertEquals(previous, next.previousWord)
        assertNotEquals(previous, next.currentWord)
    }

    @Test
    fun correctWordIncrementsProgressButIncorrectWordDoesNot() {
        val state = stateFor("CAT", completedCount = 2)

        assertEquals(3, spellCorrectly(state).completedWordCount)
        assertEquals(2, spellIncorrectly(state).completedWordCount)
    }

    @Test
    fun sessionCompletesAfterExactlyFiveCorrectWords() {
        var state = SpeakAndSpellEngine.newGame(SpellingLevel.Level1, Random(8))
        repeat(4) { index ->
            state = spellCorrectly(state)
            assertEquals(index + 1, state.completedWordCount)
            assertEquals(SpellingRoundState.Correct, state.roundState)
            state = SpeakAndSpellEngine.nextWord(state, Random(index))
        }
        state = spellCorrectly(state)

        assertEquals(5, state.completedWordCount)
        assertEquals(SpellingRoundState.SessionComplete, state.roundState)
    }

    @Test
    fun newSessionResetsProgressAndStartsDifferentWord() {
        val complete = stateFor("CAT", completedCount = 4).let(::spellCorrectly)
        val fresh = SpeakAndSpellEngine.newSession(complete, Random(5))

        assertEquals(0, fresh.completedWordCount)
        assertEquals(SpellingRoundState.Playing, fresh.roundState)
        assertTrue(fresh.enteredTileIds.isEmpty())
        assertNotEquals(complete.currentWord.text, fresh.currentWord.text)
    }

    @Test
    fun rapidInputIsIgnoredWhileEvaluationIsLocked() {
        listOf(spellCorrectly(stateFor("CAT")), spellIncorrectly(stateFor("CAT"))).forEach { locked ->
            val available = locked.letterBank.firstOrNull(LetterTile::isAvailable)
            if (available != null) assertSame(locked, SpeakAndSpellEngine.selectTile(locked, available.id))
            assertSame(locked, SpeakAndSpellEngine.backspace(locked))
            assertSame(locked, SpeakAndSpellEngine.requestHelp(locked))
        }
    }

    @Test
    fun repeatedRandomRoundsPreserveStateInvariants() {
        SpellingLevel.entries.forEach { level ->
            repeat(50) { seed ->
                var state = SpeakAndSpellEngine.newGame(level, Random(seed))
                repeat(4) { round ->
                    assertInvariants(state)
                    state = spellCorrectly(state)
                    assertInvariants(state)
                    state = SpeakAndSpellEngine.nextWord(state, Random(seed + round + 1))
                }
            }
        }
    }

    @Test
    fun invalidOrAlreadyConsumedTileSelectionIsSafe() {
        val state = stateFor("CAT")
        assertSame(state, SpeakAndSpellEngine.selectTile(state, -1))
        val tile = state.letterBank.first()
        val selected = SpeakAndSpellEngine.selectTile(state, tile.id)
        assertSame(selected, SpeakAndSpellEngine.selectTile(selected, tile.id))
    }

    @Test
    fun incorrectResetAndNextWordRequireMatchingRoundState() {
        val playing = stateFor("CAT")
        assertSame(playing, SpeakAndSpellEngine.resetIncorrectAnswer(playing))
        assertSame(playing, SpeakAndSpellEngine.nextWord(playing, Random(1)))
    }

    @Test
    fun correctSpeechSeparatesLettersDeliberately() {
        assertEquals("You got it! C, A, T spells cat!", SpeakAndSpellSpeech.correctAnswer("CAT"))
        assertEquals(
            "You got it! A, P, P, L, E spells apple!",
            SpeakAndSpellSpeech.correctAnswer("APPLE"),
        )
        assertEquals("Cat starts with C.", SpeakAndSpellSpeech.firstLetterHelp("CAT"))
    }

    private fun words(level: SpellingLevel): Set<String> =
        SpellingWordCatalog.words(level).map(SpellingWord::text).toSet()

    private fun stateFor(word: String, completedCount: Int = 0): SpeakAndSpellState {
        val tiles = (word.toList() + listOf('X', 'Z')).mapIndexed { id, letter ->
            LetterTile(id = id, letter = letter)
        }
        return SpeakAndSpellState(
            level = when (word.length) {
                3 -> SpellingLevel.Level1
                4 -> SpellingLevel.Level2
                else -> SpellingLevel.Level3
            },
            currentWord = SpellingWord(word, "✦"),
            letterBank = tiles,
            completedWordCount = completedCount,
        )
    }

    private fun spellCorrectly(initial: SpeakAndSpellState): SpeakAndSpellState {
        var state = initial
        state.currentWord.text.forEach { letter ->
            val tile = state.letterBank.first { it.letter == letter && it.isAvailable }
            state = SpeakAndSpellEngine.selectTile(state, tile.id)
        }
        return state
    }

    private fun spellIncorrectly(initial: SpeakAndSpellState): SpeakAndSpellState {
        var state = initial
        val distractor = state.letterBank.first { it.letter !in state.currentWord.text }
        state = SpeakAndSpellEngine.selectTile(state, distractor.id)
        while (state.enteredTileIds.size < state.currentWord.text.length) {
            val tile = state.letterBank.first(LetterTile::isAvailable)
            state = SpeakAndSpellEngine.selectTile(state, tile.id)
        }
        return state
    }

    private fun SpeakAndSpellState.tile(id: Int): LetterTile = letterBank.first { it.id == id }

    private fun assertInvariants(state: SpeakAndSpellState) {
        assertEquals(state.letterBank.size, state.letterBank.map(LetterTile::id).distinct().size)
        assertEquals(state.enteredTileIds.size, state.enteredTileIds.distinct().size)
        assertTrue(state.enteredTileIds.size <= state.currentWord.text.length)
        assertEquals(
            state.enteredTileIds.toSet(),
            state.letterBank.filter { !it.isAvailable }.map(LetterTile::id).toSet(),
        )
        val required = state.currentWord.text.groupingBy { it }.eachCount()
        val bank = state.letterBank.map(LetterTile::letter).groupingBy { it }.eachCount()
        required.forEach { (letter, count) -> assertTrue(bank.getValue(letter) >= count) }
    }
}
