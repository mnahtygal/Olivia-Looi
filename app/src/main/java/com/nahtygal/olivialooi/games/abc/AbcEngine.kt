package com.nahtygal.olivialooi.games.abc

import kotlin.random.Random

enum class AbcMode {
    LEARN,
    FIND_LETTER,
    STARTS_WITH,
}

enum class AbcRoundState {
    READY,
    EVALUATING,
    COMPLETED,
}

data class AbcState(
    val selectedMode: AbcMode,
    val currentTarget: AlphabetEntry,
    val previousTarget: AlphabetEntry?,
    val answerChoices: List<AlphabetEntry>,
    val selectedChoiceId: String?,
    val sessionStars: Int,
    val completedRoundCount: Int,
    val roundState: AbcRoundState,
    val helpCount: Int,
    val attemptIdentity: Long,
    val awardedAttemptIdentity: Long?,
    val sessionComplete: Boolean,
    val alphabetIndex: Int,
) {
    val usesStars: Boolean get() = selectedMode != AbcMode.LEARN
    val inputLocked: Boolean
        get() = selectedMode == AbcMode.LEARN || roundState != AbcRoundState.READY || sessionComplete
    val roundComplete: Boolean
        get() = selectedChoiceId == currentTarget.stableId && roundState != AbcRoundState.READY
}

object AbcEngine {
    const val ROUNDS_PER_SESSION = 5
    const val CORRECT_STARS = 2
    const val INCORRECT_STARS = 1

    fun newGame(mode: AbcMode, random: Random = Random.Default): AbcState =
        if (mode == AbcMode.LEARN) {
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
    fun nextLetter(state: AbcState): AbcState {
        if (state.selectedMode != AbcMode.LEARN) return state
        return learnState((state.alphabetIndex + 1) % AlphabetCatalog.entries.size)
    }

    fun previousLetter(state: AbcState): AbcState {
        if (state.selectedMode != AbcMode.LEARN) return state
        val previousIndex = (state.alphabetIndex - 1 + AlphabetCatalog.entries.size) %
            AlphabetCatalog.entries.size
        return learnState(previousIndex)
    }

    fun selectChoice(state: AbcState, choiceId: String): AbcState {
        if (state.inputLocked || state.answerChoices.none { it.stableId == choiceId }) return state
        val attempt = state.attemptIdentity + 1
        val correct = choiceId == state.currentTarget.stableId
        val completedRounds = state.completedRoundCount + if (correct) 1 else 0
        return state.copy(
            selectedChoiceId = choiceId,
            sessionStars = state.sessionStars + if (correct) CORRECT_STARS else INCORRECT_STARS,
            completedRoundCount = completedRounds,
            roundState = AbcRoundState.EVALUATING,
            attemptIdentity = attempt,
            awardedAttemptIdentity = attempt,
            sessionComplete = completedRounds == ROUNDS_PER_SESSION,
        )
    }

    fun finishEvaluation(state: AbcState, attemptIdentity: Long): AbcState {
        if (
            state.roundState != AbcRoundState.EVALUATING ||
            state.attemptIdentity != attemptIdentity ||
            state.awardedAttemptIdentity != attemptIdentity
        ) {
            return state
        }
        return if (state.selectedChoiceId == state.currentTarget.stableId) {
            state.copy(roundState = AbcRoundState.COMPLETED)
        } else {
            state.copy(
                selectedChoiceId = null,
                roundState = AbcRoundState.READY,
                awardedAttemptIdentity = null,
            )
        }
    }

    fun requestHelp(state: AbcState): AbcState =
        if (state.inputLocked || state.selectedMode == AbcMode.LEARN) {
            state
        } else {
            state.copy(helpCount = state.helpCount + 1)
        }

    fun nextRound(state: AbcState, random: Random = Random.Default): AbcState {
        if (state.roundState != AbcRoundState.COMPLETED || state.sessionComplete) return state
        return createQuizRound(
            mode = state.selectedMode,
            previousTarget = state.currentTarget,
            completedRoundCount = state.completedRoundCount,
            sessionStars = state.sessionStars,
            attemptIdentity = state.attemptIdentity,
            random = random,
        )
    }

    fun newSession(state: AbcState, random: Random = Random.Default): AbcState {
        if (!state.sessionComplete || state.selectedMode == AbcMode.LEARN) return state
        return createQuizRound(
            mode = state.selectedMode,
            previousTarget = state.currentTarget,
            completedRoundCount = 0,
            sessionStars = 0,
            attemptIdentity = state.attemptIdentity,
            random = random,
        )
    }

    private fun learnState(index: Int): AbcState = AbcState(
        selectedMode = AbcMode.LEARN,
        currentTarget = AlphabetCatalog.entries[index],
        previousTarget = null,
        answerChoices = emptyList(),
        selectedChoiceId = null,
        sessionStars = 0,
        completedRoundCount = 0,
        roundState = AbcRoundState.READY,
        helpCount = 0,
        attemptIdentity = 0,
        awardedAttemptIdentity = null,
        sessionComplete = false,
        alphabetIndex = index,
    )

    private fun createQuizRound(
        mode: AbcMode,
        previousTarget: AlphabetEntry?,
        completedRoundCount: Int,
        sessionStars: Int,
        attemptIdentity: Long,
        random: Random,
    ): AbcState {
        require(mode != AbcMode.LEARN)
        val targetPool = AlphabetCatalog.entries.filter { it.stableId != previousTarget?.stableId }
        val target = targetPool[random.nextInt(targetPool.size)]
        val distractors = AlphabetCatalog.entries
            .filter { it.stableId != target.stableId }
            .shuffled(random)
            .take(2)
        val choices = (distractors + target).shuffled(random)
        return AbcState(
            selectedMode = mode,
            currentTarget = target,
            previousTarget = previousTarget,
            answerChoices = choices,
            selectedChoiceId = null,
            sessionStars = sessionStars,
            completedRoundCount = completedRoundCount,
            roundState = AbcRoundState.READY,
            helpCount = 0,
            attemptIdentity = attemptIdentity,
            awardedAttemptIdentity = null,
            sessionComplete = false,
            alphabetIndex = AlphabetCatalog.entries.indexOf(target),
        )
    }
}

object AbcSpeech {
    fun prompt(state: AbcState): String = when (state.selectedMode) {
        AbcMode.LEARN -> with(state.currentTarget) {
            "$letter! $letter is for $spokenWord!"
        }
        AbcMode.FIND_LETTER -> "Can you find the letter ${state.currentTarget.letter}?"
        AbcMode.STARTS_WITH -> "What starts with ${state.currentTarget.letter}?"
    }

    fun correct(state: AbcState): String = when (state.selectedMode) {
        AbcMode.LEARN -> prompt(state)
        AbcMode.FIND_LETTER -> "You found it! ${state.currentTarget.letter}!"
        AbcMode.STARTS_WITH -> with(state.currentTarget) {
            "That's right! $spokenWord starts with $letter!"
        }
    }

    fun help(state: AbcState): String = when (state.selectedMode) {
        AbcMode.LEARN -> prompt(state)
        AbcMode.FIND_LETTER -> if (state.helpCount == 1) {
            prompt(state)
        } else {
            "Look for the letter ${state.currentTarget.letter}."
        }
        AbcMode.STARTS_WITH -> if (state.helpCount == 1) {
            prompt(state)
        } else {
            "Listen to the first sound in the word."
        }
    }

    const val INCORRECT = "Almost! Try again."
    const val SESSION_COMPLETE = "Great job, Olivia!"
}
