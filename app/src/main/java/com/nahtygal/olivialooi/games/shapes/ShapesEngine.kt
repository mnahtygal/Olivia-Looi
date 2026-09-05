package com.nahtygal.olivialooi.games.shapes

import kotlin.random.Random

enum class ShapesMode {
    LEARN,
    FIND_SHAPE,
    FIND_COLOR_SHAPE,
}

enum class ShapesRoundState {
    READY,
    EVALUATING,
    COMPLETED,
}

data class ShapesState(
    val selectedMode: ShapesMode,
    val currentTarget: ShapeChoice,
    val previousTarget: ShapeChoice?,
    val answerChoices: List<ShapeChoice>,
    val selectedChoiceId: String?,
    val sessionStars: Int,
    val completedRoundCount: Int,
    val roundState: ShapesRoundState,
    val helpCount: Int,
    val attemptIdentity: Long,
    val awardedAttemptIdentity: Long?,
    val sessionComplete: Boolean,
    val learnIndex: Int,
) {
    val usesStars: Boolean get() = selectedMode != ShapesMode.LEARN
    val inputLocked: Boolean
        get() = selectedMode == ShapesMode.LEARN || roundState != ShapesRoundState.READY || sessionComplete
    val roundComplete: Boolean
        get() = selectedChoiceId == currentTarget.stableId && roundState != ShapesRoundState.READY
}

object ShapesEngine {
    // Twelve fixed combinations, wrapping in both directions; each shape meets two colors.
    val learnSequence: List<ShapeChoice> = (0 until 12).map { index ->
        ShapeChoice(ShapeId.entries[index % 6], ColorId.entries[(index % 6 + index / 6) % 6])
    }

    fun isCorrect(state: ShapesState, choice: ShapeChoice): Boolean =
        choice.shape == state.currentTarget.shape &&
            (state.selectedMode == ShapesMode.FIND_SHAPE || choice.color == state.currentTarget.color)

    const val ROUNDS_PER_SESSION = 5
    const val CORRECT_STARS = 2
    const val INCORRECT_STARS = 1

    fun newGame(mode: ShapesMode, random: Random = Random.Default): ShapesState =
        if (mode == ShapesMode.LEARN) {
            learnState(0)
        } else {
            createQuizRound(
                mode = mode,
                previousTarget = null,
                completedRoundCount = 0,
                sessionStars = 0,
                attemptIdentity = 0,
                random = random,
            )
        }

    /** LEARN wraps at both ends so Previous and Next always remain useful. */
    fun nextShape(state: ShapesState): ShapesState {
        if (state.selectedMode != ShapesMode.LEARN) return state
        return learnState((state.learnIndex + 1) % learnSequence.size)
    }

    fun previousShape(state: ShapesState): ShapesState {
        if (state.selectedMode != ShapesMode.LEARN) return state
        val previousIndex = (state.learnIndex - 1 + learnSequence.size) %
            learnSequence.size
        return learnState(previousIndex)
    }

    /** Callbacks may carry the identity of the READY state that rendered them. */
    fun selectChoice(
        state: ShapesState,
        choiceId: String,
        expectedAttemptIdentity: Long = state.attemptIdentity,
    ): ShapesState {
        if (expectedAttemptIdentity != state.attemptIdentity || state.inputLocked || state.answerChoices.none { it.stableId == choiceId }) return state
        val attempt = state.attemptIdentity + 1
        val correct = isCorrect(state, state.answerChoices.first { it.stableId == choiceId })
        val completedRounds = state.completedRoundCount + if (correct) 1 else 0
        return state.copy(
            selectedChoiceId = choiceId,
            sessionStars = state.sessionStars + if (correct) CORRECT_STARS else INCORRECT_STARS,
            completedRoundCount = completedRounds,
            roundState = ShapesRoundState.EVALUATING,
            attemptIdentity = attempt,
            awardedAttemptIdentity = attempt,
            sessionComplete = completedRounds == ROUNDS_PER_SESSION,
        )
    }

    fun finishEvaluation(state: ShapesState, attemptIdentity: Long): ShapesState {
        if (
            state.roundState != ShapesRoundState.EVALUATING ||
            state.attemptIdentity != attemptIdentity ||
            state.awardedAttemptIdentity != attemptIdentity
        ) {
            return state
        }
        return if (state.selectedChoiceId == state.currentTarget.stableId) {
            state.copy(roundState = ShapesRoundState.COMPLETED)
        } else {
            state.copy(
                selectedChoiceId = null,
                roundState = ShapesRoundState.READY,
                awardedAttemptIdentity = null,
            )
        }
    }

    fun requestHelp(state: ShapesState): ShapesState =
        if (state.inputLocked || state.selectedMode == ShapesMode.LEARN) {
            state
        } else {
            state.copy(helpCount = state.helpCount + 1)
        }

    fun nextRound(state: ShapesState, random: Random = Random.Default): ShapesState {
        if (state.roundState != ShapesRoundState.COMPLETED || state.sessionComplete) return state
        return createQuizRound(
            mode = state.selectedMode,
            previousTarget = state.currentTarget,
            completedRoundCount = state.completedRoundCount,
            sessionStars = state.sessionStars,
            attemptIdentity = state.attemptIdentity,
            random = random,
        )
    }

    fun newSession(state: ShapesState, random: Random = Random.Default): ShapesState {
        if (!state.sessionComplete || state.selectedMode == ShapesMode.LEARN) return state
        return createQuizRound(
            mode = state.selectedMode,
            previousTarget = state.currentTarget,
            completedRoundCount = 0,
            sessionStars = 0,
            attemptIdentity = state.attemptIdentity,
            random = random,
        )
    }

    private fun learnState(index: Int): ShapesState = ShapesState(
        selectedMode = ShapesMode.LEARN,
        currentTarget = learnSequence[index],
        previousTarget = null,
        answerChoices = emptyList(),
        selectedChoiceId = null,
        sessionStars = 0,
        completedRoundCount = 0,
        roundState = ShapesRoundState.READY,
        helpCount = 0,
        attemptIdentity = 0,
        awardedAttemptIdentity = null,
        sessionComplete = false,
        learnIndex = index,
    )

    private fun createQuizRound(
        mode: ShapesMode,
        previousTarget: ShapeChoice?,
        completedRoundCount: Int,
        sessionStars: Int,
        attemptIdentity: Long,
        random: Random,
    ): ShapesState {
        require(mode != ShapesMode.LEARN)
        val targetPool = ShapeId.entries.flatMap { shape ->
            ColorId.entries.map { color -> ShapeChoice(shape, color) }
        }.filter {
            if (mode == ShapesMode.FIND_SHAPE) it.shape != previousTarget?.shape
            else it != previousTarget
        }
        val target = targetPool.random(random)
        val otherShapes = ShapeId.entries.filter { it != target.shape }.shuffled(random)
        val distractors = if (mode == ShapesMode.FIND_SHAPE) {
            otherShapes.take(2).map { ShapeChoice(it, target.color) }
        } else {
            listOf(
                ShapeChoice(otherShapes.first(), target.color),
                ShapeChoice(target.shape, ColorId.entries.filter { it != target.color }.random(random)),
            )
        }
        val choices = (distractors + target).shuffled(random)
        return ShapesState(
            selectedMode = mode,
            currentTarget = target,
            previousTarget = previousTarget,
            answerChoices = choices,
            selectedChoiceId = null,
            sessionStars = sessionStars,
            completedRoundCount = completedRoundCount,
            roundState = ShapesRoundState.READY,
            helpCount = 0,
            attemptIdentity = attemptIdentity,
            awardedAttemptIdentity = null,
            sessionComplete = false,
            learnIndex = 0,
        )
    }
}

object ShapesSpeech {
    private fun targetName(state: ShapesState): String =
        if (state.selectedMode == ShapesMode.FIND_SHAPE) state.currentTarget.shape.spokenName
        else "${state.currentTarget.color.spokenName} ${state.currentTarget.shape.spokenName}"

    fun prompt(state: ShapesState): String = if (state.selectedMode == ShapesMode.LEARN) {
        "${state.currentTarget.color.displayName} ${state.currentTarget.shape.spokenName}!"
    } else "Can you find the ${targetName(state)}?"

    fun correct(state: ShapesState): String = "You found the ${targetName(state)}!"

    fun help(state: ShapesState): String = when {
        state.selectedMode == ShapesMode.LEARN || state.helpCount <= 1 -> prompt(state)
        state.selectedMode == ShapesMode.FIND_SHAPE -> "Look carefully at the shape."
        else -> "Look for the color and the shape."
    }

    const val INCORRECT = "Almost! Try again."
    const val SESSION_COMPLETE = "Great job, Olivia!"
}
