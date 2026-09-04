package com.nahtygal.olivialooi.games.math

import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

enum class MathLevel(val maximumAnswer: Int) {
    SINGLE_DIGIT(9),
    DOUBLE_DIGIT(99),
    TRIPLE_DIGIT(999),
    MIXED(999),
    ;

    val isNumericLevel: Boolean get() = this != MIXED
}

enum class MathOperation(val symbol: String, val spokenName: String) {
    ADDITION("+", "plus"),
    SUBTRACTION("−", "minus"),
}

data class MathProblem(
    val operation: MathOperation,
    val leftOperand: Int,
    val rightOperand: Int,
    val correctAnswer: Int,
    val answerChoices: List<Int>,
) {
    fun sameCalculationAs(other: MathProblem?): Boolean =
        other != null &&
            operation == other.operation &&
            leftOperand == other.leftOperand &&
            rightOperand == other.rightOperand
}

enum class MathRoundState {
    READY,
    EVALUATING,
    COMPLETED,
}

data class MathState(
    val selectedLevel: MathLevel,
    val effectiveLevel: MathLevel,
    val currentProblem: MathProblem,
    val previousProblem: MathProblem?,
    val selectedAnswer: Int?,
    val roundState: MathRoundState,
    val helpCount: Int,
    val completedProblemCount: Int,
    val sessionStars: Int,
    val sessionComplete: Boolean,
    val attemptIdentity: Long,
    val awardedAttemptIdentity: Long?,
) {
    val inputLocked: Boolean get() = roundState != MathRoundState.READY || sessionComplete
    val roundComplete: Boolean
        get() = selectedAnswer == currentProblem.correctAnswer &&
            roundState != MathRoundState.READY
}

sealed interface MathVisualHelp {
    data class Addition(val startingCount: Int, val addedCount: Int) : MathVisualHelp
    data class Subtraction(val startingCount: Int, val removedCount: Int) : MathVisualHelp
}

object MathEngine {
    const val PROBLEMS_PER_SESSION = 5
    const val CORRECT_STARS = 2
    const val INCORRECT_STARS = 1

    private val numericLevels = listOf(
        MathLevel.SINGLE_DIGIT,
        MathLevel.DOUBLE_DIGIT,
        MathLevel.TRIPLE_DIGIT,
    )

    fun newGame(level: MathLevel, random: Random = Random.Default): MathState =
        createRound(
            selectedLevel = level,
            previousProblem = null,
            completedProblemCount = 0,
            sessionStars = 0,
            attemptIdentity = 0,
            random = random,
        )

    fun selectAnswer(state: MathState, answer: Int): MathState {
        if (state.inputLocked || answer !in state.currentProblem.answerChoices) return state

        val attempt = state.attemptIdentity + 1
        val correct = answer == state.currentProblem.correctAnswer
        val completedCount = state.completedProblemCount + if (correct) 1 else 0
        val earnedStars = if (correct) CORRECT_STARS else INCORRECT_STARS
        return state.copy(
            selectedAnswer = answer,
            roundState = MathRoundState.EVALUATING,
            completedProblemCount = completedCount,
            sessionStars = state.sessionStars + earnedStars,
            sessionComplete = completedCount == PROBLEMS_PER_SESSION,
            attemptIdentity = attempt,
            awardedAttemptIdentity = attempt,
        )
    }

    fun finishEvaluation(state: MathState, attemptIdentity: Long): MathState {
        if (
            state.roundState != MathRoundState.EVALUATING ||
            state.attemptIdentity != attemptIdentity ||
            state.awardedAttemptIdentity != attemptIdentity
        ) {
            return state
        }

        return if (state.selectedAnswer == state.currentProblem.correctAnswer) {
            state.copy(roundState = MathRoundState.COMPLETED)
        } else {
            state.copy(
                selectedAnswer = null,
                roundState = MathRoundState.READY,
                awardedAttemptIdentity = null,
            )
        }
    }

    fun requestHelp(state: MathState): MathState =
        if (state.inputLocked) state else state.copy(helpCount = state.helpCount + 1)

    fun nextProblem(state: MathState, random: Random = Random.Default): MathState {
        if (state.roundState != MathRoundState.COMPLETED || state.sessionComplete) return state
        return createRound(
            selectedLevel = state.selectedLevel,
            previousProblem = state.currentProblem,
            completedProblemCount = state.completedProblemCount,
            sessionStars = state.sessionStars,
            attemptIdentity = state.attemptIdentity,
            random = random,
        )
    }

    fun newSession(state: MathState, random: Random = Random.Default): MathState {
        if (!state.sessionComplete) return state
        return createRound(
            selectedLevel = state.selectedLevel,
            previousProblem = state.currentProblem,
            completedProblemCount = 0,
            sessionStars = 0,
            attemptIdentity = state.attemptIdentity,
            random = random,
        )
    }

    fun visualHelp(state: MathState): MathVisualHelp? {
        if (state.effectiveLevel != MathLevel.SINGLE_DIGIT || state.helpCount == 0) return null
        return when (state.currentProblem.operation) {
            MathOperation.ADDITION -> MathVisualHelp.Addition(
                startingCount = state.currentProblem.leftOperand,
                addedCount = state.currentProblem.rightOperand,
            )

            MathOperation.SUBTRACTION -> MathVisualHelp.Subtraction(
                startingCount = state.currentProblem.leftOperand,
                removedCount = state.currentProblem.rightOperand,
            )
        }
    }

    internal fun stateForProblem(
        selectedLevel: MathLevel,
        effectiveLevel: MathLevel,
        operation: MathOperation,
        leftOperand: Int,
        rightOperand: Int,
        random: Random = Random.Default,
    ): MathState = MathState(
        selectedLevel = selectedLevel,
        effectiveLevel = effectiveLevel,
        currentProblem = createProblem(
            effectiveLevel = effectiveLevel,
            operation = operation,
            leftOperand = leftOperand,
            rightOperand = rightOperand,
            random = random,
        ),
        previousProblem = null,
        selectedAnswer = null,
        roundState = MathRoundState.READY,
        helpCount = 0,
        completedProblemCount = 0,
        sessionStars = 0,
        sessionComplete = false,
        attemptIdentity = 0,
        awardedAttemptIdentity = null,
    )

    internal fun createProblem(
        effectiveLevel: MathLevel,
        operation: MathOperation,
        leftOperand: Int,
        rightOperand: Int,
        random: Random,
    ): MathProblem {
        require(effectiveLevel.isNumericLevel)
        require(leftOperand >= 0 && rightOperand >= 0)
        val answer = when (operation) {
            MathOperation.ADDITION -> leftOperand + rightOperand
            MathOperation.SUBTRACTION -> leftOperand - rightOperand
        }
        require(answer in 0..effectiveLevel.maximumAnswer)
        if (operation == MathOperation.SUBTRACTION) require(leftOperand >= rightOperand)
        return MathProblem(
            operation = operation,
            leftOperand = leftOperand,
            rightOperand = rightOperand,
            correctAnswer = answer,
            answerChoices = answerChoices(answer, effectiveLevel.maximumAnswer, random),
        )
    }

    private fun createRound(
        selectedLevel: MathLevel,
        previousProblem: MathProblem?,
        completedProblemCount: Int,
        sessionStars: Int,
        attemptIdentity: Long,
        random: Random,
    ): MathState {
        var effectiveLevel: MathLevel
        var problem: MathProblem
        var attempts = 0
        do {
            effectiveLevel = if (selectedLevel == MathLevel.MIXED) {
                numericLevels[random.nextInt(numericLevels.size)]
            } else {
                selectedLevel
            }
            problem = generateProblem(effectiveLevel, random)
            attempts += 1
        } while (problem.sameCalculationAs(previousProblem) && attempts < 24)

        if (problem.sameCalculationAs(previousProblem)) {
            problem = deterministicAlternative(effectiveLevel, previousProblem!!, random)
        }

        return MathState(
            selectedLevel = selectedLevel,
            effectiveLevel = effectiveLevel,
            currentProblem = problem,
            previousProblem = previousProblem,
            selectedAnswer = null,
            roundState = MathRoundState.READY,
            helpCount = 0,
            completedProblemCount = completedProblemCount,
            sessionStars = sessionStars,
            sessionComplete = false,
            attemptIdentity = attemptIdentity,
            awardedAttemptIdentity = null,
        )
    }

    private fun generateProblem(level: MathLevel, random: Random): MathProblem {
        val operation = if (random.nextBoolean()) {
            MathOperation.ADDITION
        } else {
            MathOperation.SUBTRACTION
        }
        val maximum = level.maximumAnswer
        val left: Int
        val right: Int
        if (operation == MathOperation.ADDITION) {
            val answer = random.nextInt(maximum + 1)
            left = random.nextInt(answer + 1)
            right = answer - left
        } else {
            left = random.nextInt(maximum + 1)
            right = random.nextInt(left + 1)
        }
        return createProblem(level, operation, left, right, random)
    }

    private fun deterministicAlternative(
        level: MathLevel,
        previousProblem: MathProblem,
        random: Random,
    ): MathProblem {
        val operation = if (previousProblem.operation == MathOperation.ADDITION) {
            MathOperation.SUBTRACTION
        } else {
            MathOperation.ADDITION
        }
        return if (operation == MathOperation.ADDITION) {
            createProblem(level, operation, 0, min(1, level.maximumAnswer), random)
        } else {
            createProblem(level, operation, min(1, level.maximumAnswer), 0, random)
        }
    }

    private fun answerChoices(correctAnswer: Int, maximum: Int, random: Random): List<Int> {
        val nearby = buildList {
            for (distance in 1..maximum) {
                val lower = correctAnswer - distance
                val upper = correctAnswer + distance
                if (lower >= 0) add(lower)
                if (upper <= maximum) add(upper)
                if (size >= 8) break
            }
        }
        val wrongAnswers = nearby.shuffled(random).take(2)
        return (wrongAnswers + correctAnswer).shuffled(random)
    }
}

object MathSpeech {
    fun problem(state: MathState): String = with(state.currentProblem) {
        "What is $leftOperand ${operation.spokenName} $rightOperand?"
    }

    fun correct(state: MathState): String = with(state.currentProblem) {
        "You got it! $leftOperand ${operation.spokenName} $rightOperand equals $correctAnswer!"
    }

    fun help(state: MathState): String = with(state.currentProblem) {
        when {
            state.effectiveLevel == MathLevel.SINGLE_DIGIT && operation == MathOperation.ADDITION ->
                if (state.helpCount == 1) {
                    "Start with $leftOperand, then count $rightOperand more."
                } else {
                    "Use the dots to count on from $leftOperand."
                }

            state.effectiveLevel == MathLevel.SINGLE_DIGIT ->
                if (state.helpCount == 1) {
                    "Start with $leftOperand, then count back $rightOperand."
                } else {
                    "Use the dots and take away $rightOperand."
                }

            operation == MathOperation.ADDITION ->
                if (state.helpCount == 1) {
                    "Try adding the ones first."
                } else {
                    "Line up each place value, then add from right to left."
                }

            else ->
                if (state.helpCount == 1) {
                    "Try subtracting the ones first."
                } else {
                    "Line up each place value, then subtract from right to left."
                }
        }
    }

    fun sessionComplete(stars: Int): String =
        "Great math, Olivia! You earned $stars stars!"

    const val INCORRECT = "Almost! Let's try again."
}
