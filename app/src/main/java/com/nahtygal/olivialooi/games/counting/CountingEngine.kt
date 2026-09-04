package com.nahtygal.olivialooi.games.counting

import kotlin.random.Random

enum class CountingLevel(val maximumTarget: Int) {
    Level1(3),
    Level2(5),
    Level3(10),
}

enum class CountingObject(
    val displayName: String,
    val pluralName: String,
    val visualSymbol: String,
) {
    Star("star", "stars", "⭐"),
    Heart("heart", "hearts", "💗"),
    Snowflake("snowflake", "snowflakes", "❄️"),
    Flower("flower", "flowers", "🌼"),
    Apple("apple", "apples", "🍎"),
    Duck("duck", "ducks", "🦆"),
    Ball("ball", "balls", "⚽"),
    Butterfly("butterfly", "butterflies", "🦋"),
    ;

    fun quantityName(quantity: Int): String = if (quantity == 1) displayName else pluralName
}

data class CountingObjectInstance(
    val id: Int,
    val identity: CountingObject,
)

data class CountingState(
    val level: CountingLevel,
    val targetCount: Int,
    val previousTargetCount: Int?,
    val objectIdentity: CountingObject,
    val previousObjectIdentity: CountingObject?,
    val displayedObjects: List<CountingObjectInstance>,
    val selectedObjectIds: Set<Int>,
    val helpCount: Int,
    val completedRoundCount: Int,
    val roundComplete: Boolean,
    val sessionComplete: Boolean,
) {
    val currentCount: Int get() = selectedObjectIds.size
    val inputLocked: Boolean get() = roundComplete || sessionComplete
}

object CountingEngine {
    const val ROUNDS_PER_SESSION = 5
    const val EXTRA_OBJECT_COUNT = 2

    fun newGame(level: CountingLevel, random: Random = Random.Default): CountingState =
        createRound(
            level = level,
            previousTargetCount = null,
            previousObjectIdentity = null,
            completedRoundCount = 0,
            random = random,
        )

    fun selectObject(state: CountingState, objectId: Int): CountingState {
        if (state.inputLocked || objectId in state.selectedObjectIds) return state
        if (state.displayedObjects.none { it.id == objectId }) return state

        val selectedIds = state.selectedObjectIds + objectId
        val completed = selectedIds.size == state.targetCount
        val completedRounds = state.completedRoundCount + if (completed) 1 else 0
        return state.copy(
            selectedObjectIds = selectedIds,
            completedRoundCount = completedRounds,
            roundComplete = completed,
            sessionComplete = completedRounds == ROUNDS_PER_SESSION,
        )
    }

    fun startOver(state: CountingState): CountingState {
        if (state.inputLocked || state.selectedObjectIds.isEmpty() && state.helpCount == 0) return state
        return state.copy(
            selectedObjectIds = emptySet(),
            helpCount = 0,
            roundComplete = false,
            sessionComplete = false,
        )
    }

    fun requestHelp(state: CountingState): CountingState =
        if (state.inputLocked) state else state.copy(helpCount = state.helpCount + 1)

    fun nextRound(state: CountingState, random: Random = Random.Default): CountingState {
        if (!state.roundComplete || state.sessionComplete) return state
        return createRound(
            level = state.level,
            previousTargetCount = state.targetCount,
            previousObjectIdentity = state.objectIdentity,
            completedRoundCount = state.completedRoundCount,
            random = random,
        )
    }

    fun newSession(state: CountingState, random: Random = Random.Default): CountingState {
        if (!state.sessionComplete) return state
        return createRound(
            level = state.level,
            previousTargetCount = state.targetCount,
            previousObjectIdentity = state.objectIdentity,
            completedRoundCount = 0,
            random = random,
        )
    }

    private fun createRound(
        level: CountingLevel,
        previousTargetCount: Int?,
        previousObjectIdentity: CountingObject?,
        completedRoundCount: Int,
        random: Random,
    ): CountingState {
        val targets = (1..level.maximumTarget).filter { candidate ->
            level.maximumTarget == 1 || candidate != previousTargetCount
        }
        val objects = CountingObject.entries.filter { candidate ->
            CountingObject.entries.size == 1 || candidate != previousObjectIdentity
        }
        val target = targets[random.nextInt(targets.size)]
        val objectIdentity = objects[random.nextInt(objects.size)]
        val displayedObjects = List(target + EXTRA_OBJECT_COUNT) { index ->
            CountingObjectInstance(id = index + 1, identity = objectIdentity)
        }.shuffled(random)

        return CountingState(
            level = level,
            targetCount = target,
            previousTargetCount = previousTargetCount,
            objectIdentity = objectIdentity,
            previousObjectIdentity = previousObjectIdentity,
            displayedObjects = displayedObjects,
            selectedObjectIds = emptySet(),
            helpCount = 0,
            completedRoundCount = completedRoundCount,
            roundComplete = false,
            sessionComplete = false,
        )
    }
}

object CountingSpeech {
    fun roundRequest(state: CountingState): String =
        "Can you find ${state.targetCount} ${state.objectIdentity.quantityName(state.targetCount)}?"

    fun spokenCount(count: Int): String? = when (count) {
        1 -> "One."
        2 -> "Two."
        3 -> "Three."
        4 -> "Four."
        5 -> "Five."
        6 -> "Six."
        7 -> "Seven."
        8 -> "Eight."
        9 -> "Nine."
        10 -> "Ten."
        else -> null
    }

    fun success(state: CountingState): String =
        "You did it! You found ${state.targetCount} " +
            "${state.objectIdentity.quantityName(state.targetCount)}! Great job, Olivia!"

    fun help(state: CountingState): String =
        "Tap the ${state.objectIdentity.pluralName} one at a time. Let's count together!"

    const val SESSION_CELEBRATION = "Great counting, Olivia!"
}
