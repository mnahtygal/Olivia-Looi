package com.nahtygal.olivialooi.games.animals

import kotlin.random.Random

enum class AnimalPlayMode { FREE_PLAY, WHO_MAKES_THIS_SOUND, FIND_THE_ANIMAL }

data class AnimalQuizState(
    val mode: AnimalPlayMode,
    val target: AnimalId,
    val choices: List<AnimalId>,
    val round: Int = 0,
    val lastAnswerCorrect: Boolean? = null,
)

object AnimalQuizEngine {
    fun newRound(mode: AnimalPlayMode, target: AnimalId? = null, random: Random = Random.Default): AnimalQuizState {
        val answer = target ?: AnimalSoundsCatalog.animals.random(random).id
        val others = AnimalSoundsCatalog.animals.map { it.id }.filterNot { it == answer }.shuffled(random).take(3)
        return AnimalQuizState(mode, answer, (others + answer).shuffled(random))
    }
    fun answer(state: AnimalQuizState, choice: AnimalId): AnimalQuizState =
        state.copy(lastAnswerCorrect = choice == state.target)
    fun advance(state: AnimalQuizState, random: Random = Random.Default): AnimalQuizState =
        newRound(state.mode, random = random).copy(round = state.round + 1)
}
