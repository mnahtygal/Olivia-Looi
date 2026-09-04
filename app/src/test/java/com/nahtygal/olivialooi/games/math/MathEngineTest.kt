package com.nahtygal.olivialooi.games.math

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MathEngineTest {
    @Test
    fun `levels expose exact requested answer maxima`() {
        assertEquals(9, MathLevel.SINGLE_DIGIT.maximumAnswer)
        assertEquals(99, MathLevel.DOUBLE_DIGIT.maximumAnswer)
        assertEquals(999, MathLevel.TRIPLE_DIGIT.maximumAnswer)
        assertFalse(MathLevel.MIXED.isNumericLevel)
    }

    @Test
    fun `single double and triple digit problems remain valid`() {
        listOf(
            MathLevel.SINGLE_DIGIT,
            MathLevel.DOUBLE_DIGIT,
            MathLevel.TRIPLE_DIGIT,
        ).forEach { level ->
            repeat(300) { seed -> assertProblemIsValid(MathEngine.newGame(level, Random(seed))) }
        }
    }

    @Test
    fun `mixed problems remain valid and use a numeric effective level`() {
        repeat(500) { seed ->
            val state = MathEngine.newGame(MathLevel.MIXED, Random(seed))
            assertTrue(state.effectiveLevel.isNumericLevel)
            assertProblemIsValid(state)
        }
    }

    @Test
    fun `only addition and subtraction operations exist`() {
        assertEquals(
            setOf(MathOperation.ADDITION, MathOperation.SUBTRACTION),
            MathOperation.entries.toSet(),
        )
        assertTrue(MathOperation.entries.none { it.name.contains("MULTIP") || it.name.contains("DIV") })
    }

    @Test
    fun `generated operations are approximately balanced`() {
        val operations = (0 until 1_000).map {
            MathEngine.newGame(MathLevel.TRIPLE_DIGIT, Random(it)).currentProblem.operation
        }
        val additions = operations.count { it == MathOperation.ADDITION }
        assertTrue(additions in 400..600)
    }

    @Test
    fun `larger levels can generate carrying borrowing and simple problems`() {
        listOf(MathLevel.DOUBLE_DIGIT, MathLevel.TRIPLE_DIGIT).forEach { level ->
            val problems = (0 until 3_000).map {
                MathEngine.newGame(level, Random(it)).currentProblem
            }
            assertTrue(problems.any { problem ->
                problem.operation == MathOperation.ADDITION &&
                    problem.leftOperand % 10 + problem.rightOperand % 10 >= 10
            })
            assertTrue(problems.any { problem ->
                problem.operation == MathOperation.SUBTRACTION &&
                    problem.leftOperand % 10 < problem.rightOperand % 10
            })
            assertTrue(problems.any { problem ->
                problem.operation == MathOperation.ADDITION &&
                    problem.leftOperand % 10 + problem.rightOperand % 10 < 10
            })
        }
    }

    @Test
    fun `subtraction never produces a negative answer`() {
        repeat(1_000) { seed ->
            val problem = MathEngine.newGame(MathLevel.MIXED, Random(seed)).currentProblem
            if (problem.operation == MathOperation.SUBTRACTION) {
                assertTrue(problem.leftOperand >= problem.rightOperand)
                assertTrue(problem.correctAnswer >= 0)
            }
        }
    }

    @Test
    fun `addition never exceeds effective maximum`() {
        repeat(1_000) { seed ->
            val state = MathEngine.newGame(MathLevel.MIXED, Random(seed))
            if (state.currentProblem.operation == MathOperation.ADDITION) {
                assertTrue(state.currentProblem.correctAnswer <= state.effectiveLevel.maximumAnswer)
            }
        }
    }

    @Test
    fun `every problem has three unique bounded choices with exactly one correct`() {
        repeat(1_000) { seed ->
            val state = MathEngine.newGame(MathLevel.MIXED, Random(seed))
            val choices = state.currentProblem.answerChoices
            assertEquals(3, choices.size)
            assertEquals(3, choices.distinct().size)
            assertEquals(1, choices.count { it == state.currentProblem.correctAnswer })
            assertTrue(choices.all { it in 0..state.effectiveLevel.maximumAnswer })
        }
    }

    @Test
    fun `correct answer order is randomized across all three positions`() {
        val positions = (0 until 300).map { seed ->
            val problem = MathEngine.newGame(MathLevel.DOUBLE_DIGIT, Random(seed)).currentProblem
            problem.answerChoices.indexOf(problem.correctAnswer)
        }.toSet()
        assertEquals(setOf(0, 1, 2), positions)
    }

    @Test
    fun `correct answer awards two stars completes round and locks input`() {
        val state = testState()
        val answered = MathEngine.selectAnswer(state, state.currentProblem.correctAnswer)

        assertEquals(2, answered.sessionStars)
        assertTrue(answered.roundComplete)
        assertTrue(answered.inputLocked)
        assertEquals(1, answered.completedProblemCount)
    }

    @Test
    fun `incorrect answer awards one star without completing problem`() {
        val state = testState()
        val answered = MathEngine.selectAnswer(state, wrongAnswer(state))

        assertEquals(1, answered.sessionStars)
        assertFalse(answered.roundComplete)
        assertTrue(answered.inputLocked)
        assertEquals(0, answered.completedProblemCount)
    }

    @Test
    fun `incorrect evaluation clears selection and unlocks retry`() {
        val state = testState()
        val answered = MathEngine.selectAnswer(state, wrongAnswer(state))
        val retry = MathEngine.finishEvaluation(answered, answered.attemptIdentity)

        assertNull(retry.selectedAnswer)
        assertFalse(retry.inputLocked)
        assertEquals(MathRoundState.READY, retry.roundState)
        assertEquals(state.currentProblem, retry.currentProblem)
    }

    @Test
    fun `duplicate evaluation and rapid taps cannot duplicate stars`() {
        val state = testState()
        val answer = wrongAnswer(state)
        val accepted = MathEngine.selectAnswer(state, answer)

        repeat(20) {
            assertSame(accepted, MathEngine.selectAnswer(accepted, answer))
        }
        val finished = MathEngine.finishEvaluation(accepted, accepted.attemptIdentity)
        assertSame(finished, MathEngine.finishEvaluation(finished, accepted.attemptIdentity))
        assertEquals(1, finished.sessionStars)
    }

    @Test
    fun `stale evaluation identity cannot unlock current attempt`() {
        val state = testState()
        val accepted = MathEngine.selectAnswer(state, wrongAnswer(state))

        assertSame(accepted, MathEngine.finishEvaluation(accepted, accepted.attemptIdentity - 1))
        assertTrue(accepted.inputLocked)
    }

    @Test
    fun `new retry earns another star and later correct answer earns two`() {
        var state = testState()
        state = MathEngine.selectAnswer(state, wrongAnswer(state))
        state = MathEngine.finishEvaluation(state, state.attemptIdentity)
        state = MathEngine.selectAnswer(state, wrongAnswer(state))
        state = MathEngine.finishEvaluation(state, state.attemptIdentity)
        state = MathEngine.selectAnswer(state, state.currentProblem.correctAnswer)

        assertEquals(4, state.sessionStars)
        assertTrue(state.roundComplete)
    }

    @Test
    fun `stars never become negative through valid transitions`() {
        var state = testState()
        repeat(12) {
            state = MathEngine.selectAnswer(state, wrongAnswer(state))
            assertTrue(state.sessionStars >= 0)
            state = MathEngine.finishEvaluation(state, state.attemptIdentity)
        }
    }

    @Test
    fun `help changes no scoring answer or completion state`() {
        val state = testState()
        val helped = MathEngine.requestHelp(state)

        assertEquals(1, helped.helpCount)
        assertEquals(state.sessionStars, helped.sessionStars)
        assertEquals(state.currentProblem, helped.currentProblem)
        assertEquals(state.selectedAnswer, helped.selectedAnswer)
        assertEquals(state.roundState, helped.roundState)
        assertEquals(state.completedProblemCount, helped.completedProblemCount)
    }

    @Test
    fun `help is ignored while answer input is locked`() {
        val state = testState()
        val evaluating = MathEngine.selectAnswer(state, wrongAnswer(state))
        assertSame(evaluating, MathEngine.requestHelp(evaluating))
    }

    @Test
    fun `progress increments once only for correct completion`() {
        val state = testState()
        val wrong = MathEngine.selectAnswer(state, wrongAnswer(state))
        assertEquals(0, wrong.completedProblemCount)
        val retry = MathEngine.finishEvaluation(wrong, wrong.attemptIdentity)
        val correct = MathEngine.selectAnswer(retry, retry.currentProblem.correctAnswer)
        assertEquals(1, correct.completedProblemCount)
        assertSame(correct, MathEngine.selectAnswer(correct, correct.currentProblem.correctAnswer))
        assertEquals(1, correct.completedProblemCount)
    }

    @Test
    fun `session completes after exactly five correct problems`() {
        var state = MathEngine.newGame(MathLevel.DOUBLE_DIGIT, Random(10))
        repeat(5) { index ->
            state = completeProblem(state)
            assertEquals(index + 1, state.completedProblemCount)
            assertEquals(index == 4, state.sessionComplete)
            if (index < 4) state = MathEngine.nextProblem(state, Random(20 + index))
        }
        assertTrue(state.inputLocked)
    }

    @Test
    fun `stars accumulate between problems`() {
        var state = MathEngine.newGame(MathLevel.SINGLE_DIGIT, Random(31))
        state = MathEngine.selectAnswer(state, wrongAnswer(state))
        state = MathEngine.finishEvaluation(state, state.attemptIdentity)
        state = completeProblem(state)
        assertEquals(3, state.sessionStars)
        state = MathEngine.nextProblem(state, Random(32))
        assertEquals(3, state.sessionStars)
        state = completeProblem(state)
        assertEquals(5, state.sessionStars)
    }

    @Test
    fun `math some more resets stars progress and round while preserving level`() {
        val completed = completeSession(MathLevel.TRIPLE_DIGIT)
        val fresh = MathEngine.newSession(completed, Random(88))

        assertEquals(MathLevel.TRIPLE_DIGIT, fresh.selectedLevel)
        assertEquals(0, fresh.sessionStars)
        assertEquals(0, fresh.completedProblemCount)
        assertEquals(0, fresh.helpCount)
        assertFalse(fresh.sessionComplete)
        assertFalse(fresh.inputLocked)
    }

    @Test
    fun `new problem avoids an immediate exact repeat`() {
        var state = MathEngine.newGame(MathLevel.SINGLE_DIGIT, Random(5))
        repeat(40) { seed ->
            val previous = state.currentProblem
            state = completeProblem(state)
            state = if (state.sessionComplete) {
                MathEngine.newSession(state, Random(100 + seed))
            } else {
                MathEngine.nextProblem(state, Random(100 + seed))
            }
            assertFalse(state.currentProblem.sameCalculationAs(previous))
            assertEquals(previous, state.previousProblem)
        }
    }

    @Test
    fun `injected randomness is deterministic`() {
        assertEquals(
            MathEngine.newGame(MathLevel.MIXED, Random(12345)),
            MathEngine.newGame(MathLevel.MIXED, Random(12345)),
        )
    }

    @Test
    fun `repeated randomized sessions preserve all invariants`() {
        repeat(80) { sessionSeed ->
            var state = MathEngine.newGame(MathLevel.MIXED, Random(sessionSeed))
            repeat(15) { round ->
                assertProblemIsValid(state)
                if (round % 3 == 0) {
                    state = MathEngine.selectAnswer(state, wrongAnswer(state))
                    state = MathEngine.finishEvaluation(state, state.attemptIdentity)
                    assertProblemIsValid(state)
                }
                state = completeProblem(state)
                assertProblemIsValid(state)
                state = if (state.sessionComplete) {
                    MathEngine.newSession(state, Random(sessionSeed * 100 + round))
                } else {
                    MathEngine.nextProblem(state, Random(sessionSeed * 100 + round))
                }
            }
        }
    }

    @Test
    fun `mixed generation is capable of all three ranges`() {
        val levels = (0 until 500).map {
            MathEngine.newGame(MathLevel.MIXED, Random(it)).effectiveLevel
        }.toSet()
        assertEquals(
            setOf(MathLevel.SINGLE_DIGIT, MathLevel.DOUBLE_DIGIT, MathLevel.TRIPLE_DIGIT),
            levels,
        )
    }

    @Test
    fun `zero value edge cases are valid`() {
        val addition = MathEngine.createProblem(
            MathLevel.SINGLE_DIGIT,
            MathOperation.ADDITION,
            0,
            0,
            Random(1),
        )
        val subtraction = MathEngine.createProblem(
            MathLevel.SINGLE_DIGIT,
            MathOperation.SUBTRACTION,
            0,
            0,
            Random(2),
        )
        assertEquals(0, addition.correctAnswer)
        assertEquals(0, subtraction.correctAnswer)
        assertChoicesValid(addition, 9)
        assertChoicesValid(subtraction, 9)
    }

    @Test
    fun `maximum boundary answers have three valid choices`() {
        listOf(MathLevel.SINGLE_DIGIT, MathLevel.DOUBLE_DIGIT, MathLevel.TRIPLE_DIGIT).forEach { level ->
            val problem = MathEngine.createProblem(
                level,
                MathOperation.ADDITION,
                0,
                level.maximumAnswer,
                Random(level.ordinal),
            )
            assertEquals(level.maximumAnswer, problem.correctAnswer)
            assertChoicesValid(problem, level.maximumAnswer)
        }
    }

    @Test
    fun `choices are unique near zero one and upper boundaries`() {
        listOf(0, 1, 8, 9).forEach { answer ->
            val problem = MathEngine.createProblem(
                MathLevel.SINGLE_DIGIT,
                MathOperation.ADDITION,
                0,
                answer,
                Random(answer),
            )
            assertChoicesValid(problem, 9)
        }
        listOf(98, 99).forEach { answer ->
            assertChoicesValid(
                MathEngine.createProblem(
                    MathLevel.DOUBLE_DIGIT,
                    MathOperation.ADDITION,
                    0,
                    answer,
                    Random(answer),
                ),
                99,
            )
        }
        listOf(998, 999).forEach { answer ->
            assertChoicesValid(
                MathEngine.createProblem(
                    MathLevel.TRIPLE_DIGIT,
                    MathOperation.ADDITION,
                    0,
                    answer,
                    Random(answer),
                ),
                999,
            )
        }
    }

    @Test
    fun `single digit visual help represents addition operands`() {
        val state = MathEngine.requestHelp(
            MathEngine.stateForProblem(
                MathLevel.SINGLE_DIGIT,
                MathLevel.SINGLE_DIGIT,
                MathOperation.ADDITION,
                3,
                2,
                Random(1),
            ),
        )
        assertEquals(MathVisualHelp.Addition(3, 2), MathEngine.visualHelp(state))
    }

    @Test
    fun `single digit visual help represents subtraction operands`() {
        val state = MathEngine.requestHelp(
            MathEngine.stateForProblem(
                MathLevel.SINGLE_DIGIT,
                MathLevel.SINGLE_DIGIT,
                MathOperation.SUBTRACTION,
                5,
                2,
                Random(1),
            ),
        )
        assertEquals(MathVisualHelp.Subtraction(5, 2), MathEngine.visualHelp(state))
    }

    @Test
    fun `visual help is hidden initially and for larger effective levels`() {
        val little = testState()
        val big = MathEngine.requestHelp(
            MathEngine.stateForProblem(
                MathLevel.DOUBLE_DIGIT,
                MathLevel.DOUBLE_DIGIT,
                MathOperation.ADDITION,
                24,
                13,
                Random(1),
            ),
        )
        assertNull(MathEngine.visualHelp(little))
        assertNull(MathEngine.visualHelp(big))
    }

    @Test
    fun `visual help does not alter scoring or answer state`() {
        val state = testState()
        val helped = MathEngine.requestHelp(state)
        MathEngine.visualHelp(helped)
        assertEquals(state.sessionStars, helped.sessionStars)
        assertEquals(state.selectedAnswer, helped.selectedAnswer)
        assertEquals(state.roundState, helped.roundState)
    }

    @Test
    fun `speech gives problem feedback and non-answer help`() {
        val state = MathEngine.stateForProblem(
            MathLevel.SINGLE_DIGIT,
            MathLevel.SINGLE_DIGIT,
            MathOperation.ADDITION,
            3,
            2,
            Random(1),
        )
        assertEquals("What is 3 plus 2?", MathSpeech.problem(state))
        assertTrue(MathSpeech.correct(state).contains("equals 5"))
        val help = MathSpeech.help(MathEngine.requestHelp(state))
        assertEquals("Start with 3, then count 2 more.", help)
        assertFalse(help.contains("5"))
    }

    private fun assertProblemIsValid(state: MathState) {
        val problem = state.currentProblem
        assertTrue(state.effectiveLevel.isNumericLevel)
        assertTrue(problem.leftOperand >= 0)
        assertTrue(problem.rightOperand >= 0)
        val expected = when (problem.operation) {
            MathOperation.ADDITION -> problem.leftOperand + problem.rightOperand
            MathOperation.SUBTRACTION -> problem.leftOperand - problem.rightOperand
        }
        assertEquals(expected, problem.correctAnswer)
        assertTrue(problem.correctAnswer in 0..state.effectiveLevel.maximumAnswer)
        if (problem.operation == MathOperation.SUBTRACTION) {
            assertTrue(problem.leftOperand >= problem.rightOperand)
        }
        assertChoicesValid(problem, state.effectiveLevel.maximumAnswer)
    }

    private fun assertChoicesValid(problem: MathProblem, maximum: Int) {
        assertEquals(3, problem.answerChoices.size)
        assertEquals(3, problem.answerChoices.distinct().size)
        assertEquals(1, problem.answerChoices.count { it == problem.correctAnswer })
        assertTrue(problem.answerChoices.all { it in 0..maximum })
    }

    private fun testState(): MathState = MathEngine.stateForProblem(
        selectedLevel = MathLevel.SINGLE_DIGIT,
        effectiveLevel = MathLevel.SINGLE_DIGIT,
        operation = MathOperation.ADDITION,
        leftOperand = 3,
        rightOperand = 2,
        random = Random(9),
    )

    private fun wrongAnswer(state: MathState): Int =
        state.currentProblem.answerChoices.first { it != state.currentProblem.correctAnswer }

    private fun completeProblem(state: MathState): MathState {
        val answered = MathEngine.selectAnswer(state, state.currentProblem.correctAnswer)
        return MathEngine.finishEvaluation(answered, answered.attemptIdentity)
    }

    private fun completeSession(level: MathLevel): MathState {
        var state = MathEngine.newGame(level, Random(50))
        repeat(5) { index ->
            state = completeProblem(state)
            if (index < 4) state = MathEngine.nextProblem(state, Random(60 + index))
        }
        return state
    }
}
