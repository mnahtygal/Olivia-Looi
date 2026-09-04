package com.nahtygal.olivialooi.games.memory

import kotlin.random.Random

enum class MemoryGameSize(val pairCount: Int, val columnCount: Int) {
    Little(pairCount = 3, columnCount = 3),
    Big(pairCount = 6, columnCount = 4),
}

enum class MemoryCardIdentity {
    Cow,
    Butterfly,
    Star,
    Puppy,
    Flower,
    Car,
}

data class MemoryCard(
    val position: Int,
    val identity: MemoryCardIdentity,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false,
)

data class MemoryMatchState(
    val size: MemoryGameSize,
    val cards: List<MemoryCard>,
    val firstSelection: Int? = null,
    val secondSelection: Int? = null,
    val matchedPairCount: Int = 0,
) {
    val isResolvingMismatch: Boolean
        get() = firstSelection != null && secondSelection != null

    val isComplete: Boolean
        get() = matchedPairCount == size.pairCount
}

object MemoryMatchEngine {
    fun newGame(
        size: MemoryGameSize,
        random: Random = Random.Default,
    ): MemoryMatchState {
        val identities = MemoryCardIdentity.entries
            .shuffled(random)
            .take(size.pairCount)
        val cards = (identities + identities)
            .shuffled(random)
            .mapIndexed { position, identity ->
                MemoryCard(position = position, identity = identity)
            }
        return MemoryMatchState(size = size, cards = cards)
    }

    fun selectCard(state: MemoryMatchState, position: Int): MemoryMatchState {
        if (state.isComplete || state.isResolvingMismatch || position !in state.cards.indices) {
            return state
        }
        val selectedCard = state.cards[position]
        if (selectedCard.isFaceUp || selectedCard.isMatched) return state

        val revealedCards = state.cards.update(position) { it.copy(isFaceUp = true) }
        val firstPosition = state.firstSelection
        if (firstPosition == null) {
            return state.copy(cards = revealedCards, firstSelection = position)
        }

        val firstCard = revealedCards[firstPosition]
        return if (firstCard.identity == selectedCard.identity) {
            state.copy(
                cards = revealedCards
                    .update(firstPosition) { it.copy(isMatched = true) }
                    .update(position) { it.copy(isMatched = true) },
                firstSelection = null,
                secondSelection = null,
                matchedPairCount = state.matchedPairCount + 1,
            )
        } else {
            state.copy(
                cards = revealedCards,
                firstSelection = firstPosition,
                secondSelection = position,
            )
        }
    }

    fun resolveMismatch(state: MemoryMatchState): MemoryMatchState {
        val firstPosition = state.firstSelection ?: return state
        val secondPosition = state.secondSelection ?: return state
        return state.copy(
            cards = state.cards
                .update(firstPosition) { it.copy(isFaceUp = false) }
                .update(secondPosition) { it.copy(isFaceUp = false) },
            firstSelection = null,
            secondSelection = null,
        )
    }

    fun reset(
        state: MemoryMatchState,
        random: Random = Random.Default,
    ): MemoryMatchState = newGame(state.size, random)

    private inline fun List<MemoryCard>.update(
        position: Int,
        transform: (MemoryCard) -> MemoryCard,
    ): List<MemoryCard> = toMutableList().also { cards ->
        cards[position] = transform(cards[position])
    }
}
