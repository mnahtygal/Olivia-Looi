package com.nahtygal.olivialooi.games.abc

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AbcEngineTest {
    @Test
    fun `fresh learn begins on A without choices or Stars`() {
        val state = AbcEngine.newGame(AbcMode.LEARN, Random(99))
        assertEquals('A', state.currentTarget.letter)
        assertEquals(0, state.alphabetIndex)
        assertTrue(state.answerChoices.isEmpty())
        assertEquals(0, state.sessionStars)
        assertFalse(state.usesStars)
    }

    @Test
    fun `learn next and previous move alphabetically`() {
        val a = AbcEngine.newGame(AbcMode.LEARN)
        val b = AbcEngine.nextLetter(a)
        assertEquals('B', b.currentTarget.letter)
        assertEquals(a, AbcEngine.previousLetter(b))
    }

    @Test
    fun `learn navigation wraps consistently at A and Z`() {
        val a = AbcEngine.newGame(AbcMode.LEARN)
        val z = AbcEngine.previousLetter(a)
        assertEquals('Z', z.currentTarget.letter)
        assertEquals('A', AbcEngine.nextLetter(z).currentTarget.letter)
    }

    @Test
    fun `learn ignores answers help and scoring transitions`() {
        val state = AbcEngine.newGame(AbcMode.LEARN)
        assertSame(state, AbcEngine.selectChoice(state, "a"))
        assertSame(state, AbcEngine.requestHelp(state))
        assertEquals(0, state.sessionStars)
        assertEquals(0, state.completedRoundCount)
    }

    @Test
    fun `find letter has three unique choices with one target`() {
        repeat(300) { seed -> assertQuizRound(AbcEngine.newGame(AbcMode.FIND_LETTER, Random(seed))) }
    }

    @Test
    fun `starts with has three unique objects with one matching initial`() {
        repeat(300) { seed ->
            val state = AbcEngine.newGame(AbcMode.STARTS_WITH, Random(seed))
            assertQuizRound(state)
            assertEquals(
                1,
                state.answerChoices.count {
                    it.displayWord.startsWith(state.currentTarget.letter, ignoreCase = true)
                },
            )
        }
    }

    @Test
    fun `injected randomness makes both quiz modes deterministic`() {
        listOf(AbcMode.FIND_LETTER, AbcMode.STARTS_WITH).forEach { mode ->
            assertEquals(
                AbcEngine.newGame(mode, Random(12345)),
                AbcEngine.newGame(mode, Random(12345)),
            )
        }
    }

    @Test
    fun `choice order is randomized across all three positions`() {
        AbcMode.entries.filter { it != AbcMode.LEARN }.forEach { mode ->
            val positions = (0 until 300).map { seed ->
                val state = AbcEngine.newGame(mode, Random(seed))
                state.answerChoices.indexOf(state.currentTarget)
            }.toSet()
            assertEquals(setOf(0, 1, 2), positions)
        }
    }

    @Test
    fun `correct find attempt awards two Stars completes and locks`() {
        val state = AbcEngine.newGame(AbcMode.FIND_LETTER, Random(1))
        val answered = AbcEngine.selectChoice(state, state.currentTarget.stableId)
        assertEquals(2, answered.sessionStars)
        assertTrue(answered.roundComplete)
        assertTrue(answered.inputLocked)
        assertEquals(1, answered.completedRoundCount)
    }

    @Test
    fun `wrong find attempt awards one Star without completing`() {
        val state = AbcEngine.newGame(AbcMode.FIND_LETTER, Random(2))
        val answered = AbcEngine.selectChoice(state, wrongChoice(state).stableId)
        assertEquals(1, answered.sessionStars)
        assertFalse(answered.roundComplete)
        assertTrue(answered.inputLocked)
        assertEquals(0, answered.completedRoundCount)
    }

    @Test
    fun `correct starts with attempt awards two Stars and completes`() {
        val state = AbcEngine.newGame(AbcMode.STARTS_WITH, Random(3))
        val answered = AbcEngine.selectChoice(state, state.currentTarget.stableId)
        assertEquals(2, answered.sessionStars)
        assertTrue(answered.roundComplete)
        assertEquals(1, answered.completedRoundCount)
    }

    @Test
    fun `wrong starts with attempt awards one Star and permits retry`() {
        val state = AbcEngine.newGame(AbcMode.STARTS_WITH, Random(4))
        val answered = AbcEngine.selectChoice(state, wrongChoice(state).stableId)
        val retry = AbcEngine.finishEvaluation(answered, answered.attemptIdentity)
        assertEquals(1, retry.sessionStars)
        assertNull(retry.selectedChoiceId)
        assertFalse(retry.inputLocked)
        assertEquals(state.currentTarget, retry.currentTarget)
        assertEquals(state.answerChoices, retry.answerChoices)
    }

    @Test
    fun `wrong find attempt clears and retries the same round`() {
        val state = AbcEngine.newGame(AbcMode.FIND_LETTER, Random(5))
        val answered = AbcEngine.selectChoice(state, wrongChoice(state).stableId)
        val retry = AbcEngine.finishEvaluation(answered, answered.attemptIdentity)
        assertEquals(AbcRoundState.READY, retry.roundState)
        assertNull(retry.selectedChoiceId)
        assertEquals(state.currentTarget, retry.currentTarget)
    }

    @Test
    fun `duplicate evaluation and rapid taps cannot duplicate Stars`() {
        AbcMode.entries.filter { it != AbcMode.LEARN }.forEach { mode ->
            val state = AbcEngine.newGame(mode, Random(mode.ordinal))
            val wrong = wrongChoice(state).stableId
            val accepted = AbcEngine.selectChoice(state, wrong)
            repeat(20) { assertSame(accepted, AbcEngine.selectChoice(accepted, wrong)) }
            val retry = AbcEngine.finishEvaluation(accepted, accepted.attemptIdentity)
            assertSame(retry, AbcEngine.finishEvaluation(retry, accepted.attemptIdentity))
            assertEquals(1, retry.sessionStars)
        }
    }

    @Test
    fun `stale evaluation identity cannot unlock an attempt`() {
        val state = AbcEngine.newGame(AbcMode.FIND_LETTER, Random(7))
        val answered = AbcEngine.selectChoice(state, wrongChoice(state).stableId)
        assertSame(answered, AbcEngine.finishEvaluation(answered, answered.attemptIdentity - 1))
        assertTrue(answered.inputLocked)
    }

    @Test
    fun `a retry can earn another Star then a correct attempt earns two`() {
        var state = AbcEngine.newGame(AbcMode.FIND_LETTER, Random(8))
        state = AbcEngine.selectChoice(state, wrongChoice(state).stableId)
        state = AbcEngine.finishEvaluation(state, state.attemptIdentity)
        state = AbcEngine.selectChoice(state, wrongChoice(state).stableId)
        state = AbcEngine.finishEvaluation(state, state.attemptIdentity)
        state = AbcEngine.selectChoice(state, state.currentTarget.stableId)
        assertEquals(4, state.sessionStars)
        assertTrue(state.roundComplete)
    }

    @Test
    fun `round progress increments only once for a correct target`() {
        val state = AbcEngine.newGame(AbcMode.FIND_LETTER, Random(9))
        val answered = AbcEngine.selectChoice(state, state.currentTarget.stableId)
        assertEquals(1, answered.completedRoundCount)
        assertSame(answered, AbcEngine.selectChoice(answered, answered.currentTarget.stableId))
        assertEquals(1, answered.completedRoundCount)
    }

    @Test
    fun `both quiz modes complete after exactly five correct rounds`() {
        listOf(AbcMode.FIND_LETTER, AbcMode.STARTS_WITH).forEach { mode ->
            var state = AbcEngine.newGame(mode, Random(10))
            repeat(5) { round ->
                state = completeRound(state)
                assertEquals(round + 1, state.completedRoundCount)
                assertEquals(round == 4, state.sessionComplete)
                if (round < 4) state = AbcEngine.nextRound(state, Random(20 + round))
            }
        }
    }

    @Test
    fun `new quiz session resets Stars progress help and selection`() {
        listOf(AbcMode.FIND_LETTER, AbcMode.STARTS_WITH).forEach { mode ->
            val complete = completeSession(mode)
            val fresh = AbcEngine.newSession(complete, Random(44))
            assertEquals(mode, fresh.selectedMode)
            assertEquals(0, fresh.sessionStars)
            assertEquals(0, fresh.completedRoundCount)
            assertEquals(0, fresh.helpCount)
            assertNull(fresh.selectedChoiceId)
            assertFalse(fresh.sessionComplete)
            assertFalse(fresh.inputLocked)
        }
    }

    @Test
    fun `next rounds avoid immediate target repetition`() {
        listOf(AbcMode.FIND_LETTER, AbcMode.STARTS_WITH).forEach { mode ->
            var state = AbcEngine.newGame(mode, Random(50))
            repeat(35) { seed ->
                val previous = state.currentTarget
                state = completeRound(state)
                state = if (state.sessionComplete) {
                    AbcEngine.newSession(state, Random(100 + seed))
                } else {
                    AbcEngine.nextRound(state, Random(100 + seed))
                }
                assertNotEquals(previous.stableId, state.currentTarget.stableId)
                assertEquals(previous, state.previousTarget)
            }
        }
    }

    @Test
    fun `help changes no Stars selection completion target or choices`() {
        listOf(AbcMode.FIND_LETTER, AbcMode.STARTS_WITH).forEach { mode ->
            val state = AbcEngine.newGame(mode, Random(60))
            val helped = AbcEngine.requestHelp(state)
            assertEquals(1, helped.helpCount)
            assertEquals(state.sessionStars, helped.sessionStars)
            assertEquals(state.selectedChoiceId, helped.selectedChoiceId)
            assertEquals(state.completedRoundCount, helped.completedRoundCount)
            assertEquals(state.currentTarget, helped.currentTarget)
            assertEquals(state.answerChoices, helped.answerChoices)
        }
    }

    @Test
    fun `help is ignored while evaluating`() {
        val state = AbcEngine.newGame(AbcMode.FIND_LETTER, Random(61))
        val evaluating = AbcEngine.selectChoice(state, wrongChoice(state).stableId)
        assertSame(evaluating, AbcEngine.requestHelp(evaluating))
    }

    @Test
    fun `Stars remain nonnegative through repeated valid transitions`() {
        var state = AbcEngine.newGame(AbcMode.STARTS_WITH, Random(70))
        repeat(20) {
            state = AbcEngine.selectChoice(state, wrongChoice(state).stableId)
            assertTrue(state.sessionStars >= 0)
            state = AbcEngine.finishEvaluation(state, state.attemptIdentity)
        }
    }

    @Test
    fun `repeated randomized find rounds preserve invariants`() {
        assertRandomizedSessions(AbcMode.FIND_LETTER)
    }

    @Test
    fun `repeated randomized starts with rounds preserve invariants`() {
        assertRandomizedSessions(AbcMode.STARTS_WITH)
    }

    @Test
    fun `speech matches required prompts and feedback`() {
        val learn = AbcEngine.newGame(AbcMode.LEARN)
        assertEquals("A! A is for Apple!", AbcSpeech.prompt(learn))

        val find = stateWithTarget(AbcMode.FIND_LETTER, 'B')
        assertEquals("Can you find the letter B?", AbcSpeech.prompt(find))
        assertEquals("You found it! B!", AbcSpeech.correct(find))

        val starts = stateWithTarget(AbcMode.STARTS_WITH, 'C')
        assertEquals("What starts with C?", AbcSpeech.prompt(starts))
        assertEquals("That's right! Cat starts with C!", AbcSpeech.correct(starts))
        assertEquals("Almost! Try again.", AbcSpeech.INCORRECT)
    }

    @Test
    fun `second help gives mode-specific strategy without revealing answer`() {
        var find = stateWithTarget(AbcMode.FIND_LETTER, 'B')
        find = AbcEngine.requestHelp(AbcEngine.requestHelp(find))
        assertEquals("Look for the letter B.", AbcSpeech.help(find))

        var starts = stateWithTarget(AbcMode.STARTS_WITH, 'C')
        starts = AbcEngine.requestHelp(AbcEngine.requestHelp(starts))
        assertEquals("Listen to the first sound in the word.", AbcSpeech.help(starts))
        assertFalse(AbcSpeech.help(starts).contains("Cat"))
    }

    private fun assertQuizRound(state: AbcState) {
        assertTrue(state.usesStars)
        assertEquals(3, state.answerChoices.size)
        assertEquals(3, state.answerChoices.map(AlphabetEntry::stableId).distinct().size)
        assertEquals(1, state.answerChoices.count { it.stableId == state.currentTarget.stableId })
        assertEquals(0, state.sessionStars)
        assertFalse(state.inputLocked)
    }

    private fun assertRandomizedSessions(mode: AbcMode) {
        repeat(60) { sessionSeed ->
            var state = AbcEngine.newGame(mode, Random(sessionSeed))
            repeat(15) { round ->
                assertQuizStateInvariants(state)
                if (round % 2 == 0) {
                    state = AbcEngine.selectChoice(state, wrongChoice(state).stableId)
                    state = AbcEngine.finishEvaluation(state, state.attemptIdentity)
                    assertQuizStateInvariants(state)
                }
                state = completeRound(state)
                assertQuizStateInvariants(state)
                state = if (state.sessionComplete) {
                    AbcEngine.newSession(state, Random(sessionSeed * 100 + round))
                } else {
                    AbcEngine.nextRound(state, Random(sessionSeed * 100 + round))
                }
            }
        }
    }

    private fun assertQuizStateInvariants(state: AbcState) {
        assertEquals(3, state.answerChoices.size)
        assertEquals(3, state.answerChoices.map(AlphabetEntry::stableId).distinct().size)
        assertEquals(1, state.answerChoices.count { it.stableId == state.currentTarget.stableId })
        assertTrue(state.sessionStars >= 0)
        assertTrue(state.completedRoundCount in 0..AbcEngine.ROUNDS_PER_SESSION)
    }

    private fun wrongChoice(state: AbcState): AlphabetEntry =
        state.answerChoices.first { it.stableId != state.currentTarget.stableId }

    private fun completeRound(state: AbcState): AbcState {
        val answered = AbcEngine.selectChoice(state, state.currentTarget.stableId)
        return AbcEngine.finishEvaluation(answered, answered.attemptIdentity)
    }

    private fun completeSession(mode: AbcMode): AbcState {
        var state = AbcEngine.newGame(mode, Random(80))
        repeat(5) { round ->
            state = completeRound(state)
            if (round < 4) state = AbcEngine.nextRound(state, Random(90 + round))
        }
        return state
    }

    private fun stateWithTarget(mode: AbcMode, targetLetter: Char): AbcState {
        val generated = AbcEngine.newGame(mode, Random(101))
        val target = requireNotNull(AlphabetCatalog.findByLetter(targetLetter))
        val distractors = AlphabetCatalog.entries.filter { it != target }.take(2)
        return generated.copy(
            currentTarget = target,
            answerChoices = distractors + target,
            alphabetIndex = AlphabetCatalog.entries.indexOf(target),
        )
    }
}
