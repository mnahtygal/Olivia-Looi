package com.nahtygal.olivialooi.games.memory

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryMatchEngineTest {
    @Test
    fun littleGameCreatesSixCardsAndThreePairs() {
        val state = game(MemoryGameSize.Little)

        assertEquals(6, state.cards.size)
        assertEquals(3, state.cards.distinctIdentities())
        assertEveryIdentityOccursTwice(state)
    }

    @Test
    fun bigGameCreatesTwelveCardsAndSixPairs() {
        val state = game(MemoryGameSize.Big)

        assertEquals(12, state.cards.size)
        assertEquals(6, state.cards.distinctIdentities())
        assertEveryIdentityOccursTwice(state)
    }

    @Test
    fun firstSelectionRevealsExactlyOneCard() {
        val state = game()
        val selected = MemoryMatchEngine.selectCard(state, 0)

        assertTrue(selected.cards[0].isFaceUp)
        assertEquals(1, selected.cards.count(MemoryCard::isFaceUp))
        assertEquals(0, selected.matchedPairCount)
        assertEquals(0, selected.firstSelection)
    }

    @Test
    fun selectingSameCardTwiceIsIgnored() {
        val once = MemoryMatchEngine.selectCard(game(), 0)

        assertSame(once, MemoryMatchEngine.selectCard(once, 0))
    }

    @Test
    fun matchingSecondSelectionMarksBothCardsAndIncrementsCount() {
        val state = game()
        val (first, second) = state.matchingPositions()
        val afterFirst = MemoryMatchEngine.selectCard(state, first)
        val matched = MemoryMatchEngine.selectCard(afterFirst, second)

        assertTrue(matched.cards[first].isMatched)
        assertTrue(matched.cards[second].isMatched)
        assertTrue(matched.cards[first].isFaceUp)
        assertTrue(matched.cards[second].isFaceUp)
        assertEquals(1, matched.matchedPairCount)
        assertFalse(matched.isResolvingMismatch)
    }

    @Test
    fun mismatchingSelectionLocksBoardUntilResolved() {
        val state = game()
        val (first, second) = state.mismatchingPositions()
        val afterFirst = MemoryMatchEngine.selectCard(state, first)
        val pending = MemoryMatchEngine.selectCard(afterFirst, second)
        val another = state.cards.indices.first { it != first && it != second }

        assertTrue(pending.isResolvingMismatch)
        assertTrue(pending.cards[first].isFaceUp)
        assertTrue(pending.cards[second].isFaceUp)
        assertSame(pending, MemoryMatchEngine.selectCard(pending, another))
    }

    @Test
    fun resolvingMismatchHidesBothCardsAndUnlocksBoard() {
        val state = game()
        val (first, second) = state.mismatchingPositions()
        val pending = MemoryMatchEngine.selectCard(
            MemoryMatchEngine.selectCard(state, first),
            second,
        )
        val resolved = MemoryMatchEngine.resolveMismatch(pending)

        assertFalse(resolved.cards[first].isFaceUp)
        assertFalse(resolved.cards[second].isFaceUp)
        assertFalse(resolved.isResolvingMismatch)
        assertEquals(null, resolved.firstSelection)
        assertEquals(null, resolved.secondSelection)
    }

    @Test
    fun matchedCardsCannotBeSelectedAgain() {
        val state = game()
        val (first, second) = state.matchingPositions()
        val matched = MemoryMatchEngine.selectCard(
            MemoryMatchEngine.selectCard(state, first),
            second,
        )

        assertSame(matched, MemoryMatchEngine.selectCard(matched, first))
    }

    @Test
    fun completionOccursOnlyAfterEveryPairIsMatched() {
        var state = game(MemoryGameSize.Little)
        assertFalse(state.isComplete)

        state.cards.groupBy(MemoryCard::identity).values.forEachIndexed { index, pair ->
            state = MemoryMatchEngine.selectCard(state, pair[0].position)
            state = MemoryMatchEngine.selectCard(state, pair[1].position)
            assertEquals(index + 1, state.matchedPairCount)
            assertEquals(index == 2, state.isComplete)
        }
    }

    @Test
    fun resetCreatesFreshPlayableGameOfSameSize() {
        var completed = game(MemoryGameSize.Little)
        completed.cards.groupBy(MemoryCard::identity).values.forEach { pair ->
            completed = MemoryMatchEngine.selectCard(completed, pair[0].position)
            completed = MemoryMatchEngine.selectCard(completed, pair[1].position)
        }
        val reset = MemoryMatchEngine.reset(completed, Random(99))

        assertEquals(MemoryGameSize.Little, reset.size)
        assertEquals(6, reset.cards.size)
        assertEquals(0, reset.matchedPairCount)
        assertFalse(reset.isComplete)
        assertTrue(reset.cards.none { it.isFaceUp || it.isMatched })
        assertEveryIdentityOccursTwice(reset)
    }

    @Test
    fun generatedGamesNeverContainDuplicateOrMissingPairStates() {
        MemoryGameSize.entries.forEach { size ->
            repeat(100) { seed ->
                val state = MemoryMatchEngine.newGame(size, Random(seed))
                assertEquals(state.cards.indices.toList(), state.cards.map(MemoryCard::position))
                assertEquals(size.pairCount, state.cards.distinctIdentities())
                assertEveryIdentityOccursTwice(state)
            }
        }
    }

    @Test
    fun rapidTapTransitionsRemainValid() {
        val state = game()
        val (first, mismatch) = state.mismatchingPositions()
        var current = MemoryMatchEngine.selectCard(state, first)
        current = MemoryMatchEngine.selectCard(current, first)
        current = MemoryMatchEngine.selectCard(current, mismatch)
        val lockedState = current

        state.cards.indices.forEach { position ->
            current = MemoryMatchEngine.selectCard(current, position)
        }
        assertEquals(lockedState, current)

        current = MemoryMatchEngine.resolveMismatch(current)
        assertFalse(current.isResolvingMismatch)
        assertEquals(0, current.cards.count(MemoryCard::isFaceUp))
        assertEquals(0, current.matchedPairCount)
    }

    @Test
    fun invalidPositionsAndResolutionWithoutMismatchAreIgnored() {
        val state = game()

        assertSame(state, MemoryMatchEngine.selectCard(state, -1))
        assertSame(state, MemoryMatchEngine.selectCard(state, state.cards.size))
        assertSame(state, MemoryMatchEngine.resolveMismatch(state))
    }

    @Test
    fun completedGameRejectsFurtherSelections() {
        var state = game(MemoryGameSize.Little)
        state.cards.groupBy(MemoryCard::identity).values.forEach { pair ->
            state = MemoryMatchEngine.selectCard(state, pair[0].position)
            state = MemoryMatchEngine.selectCard(state, pair[1].position)
        }

        assertTrue(state.isComplete)
        assertSame(state, MemoryMatchEngine.selectCard(state, 0))
    }

    @Test
    fun seededGamesShuffleCardPositions() {
        val first = MemoryMatchEngine.newGame(MemoryGameSize.Big, Random(1))
        val second = MemoryMatchEngine.newGame(MemoryGameSize.Big, Random(2))

        assertNotEquals(first.cards.map(MemoryCard::identity), second.cards.map(MemoryCard::identity))
    }

    private fun game(size: MemoryGameSize = MemoryGameSize.Big): MemoryMatchState =
        MemoryMatchEngine.newGame(size, Random(42))

    private fun MemoryMatchState.matchingPositions(): Pair<Int, Int> {
        val pair = cards.groupBy(MemoryCard::identity).values.first()
        return pair[0].position to pair[1].position
    }

    private fun MemoryMatchState.mismatchingPositions(): Pair<Int, Int> {
        val first = cards.first()
        val second = cards.first { it.identity != first.identity }
        return first.position to second.position
    }

    private fun List<MemoryCard>.distinctIdentities(): Int = distinctBy(MemoryCard::identity).size

    private fun assertEveryIdentityOccursTwice(state: MemoryMatchState) {
        assertTrue(state.cards.groupingBy(MemoryCard::identity).eachCount().values.all { it == 2 })
    }
}
