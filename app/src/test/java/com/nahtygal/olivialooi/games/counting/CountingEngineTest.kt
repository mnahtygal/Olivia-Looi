package com.nahtygal.olivialooi.games.counting

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CountingEngineTest {
    @Test
    fun `targets stay within each level range`() {
        CountingLevel.entries.forEach { level ->
            repeat(200) { seed ->
                val state = CountingEngine.newGame(level, Random(seed))
                assertTrue(state.targetCount in 1..level.maximumTarget)
            }
        }
    }

    @Test
    fun `round has target plus two objects with unique IDs`() {
        repeat(100) { seed ->
            val state = CountingEngine.newGame(CountingLevel.Level3, Random(seed))
            assertEquals(state.targetCount + 2, state.displayedObjects.size)
            assertTrue(state.displayedObjects.size >= state.targetCount)
            assertEquals(
                state.displayedObjects.size,
                state.displayedObjects.map(CountingObjectInstance::id).distinct().size,
            )
        }
    }

    @Test
    fun `all displayed objects share the round identity`() {
        repeat(50) { seed ->
            val state = CountingEngine.newGame(CountingLevel.Level3, Random(seed))
            assertTrue(state.displayedObjects.all { it.identity == state.objectIdentity })
        }
    }

    @Test
    fun `selecting one valid object increments by exactly one`() {
        val state = CountingEngine.newGame(CountingLevel.Level3, Random(1))
        val selected = CountingEngine.selectObject(state, state.displayedObjects.first().id)
        assertEquals(0, state.currentCount)
        assertEquals(1, selected.currentCount)
    }

    @Test
    fun `selecting the same object twice does not double count`() {
        val state = stateForTarget(CountingLevel.Level2, 3)
        val id = state.displayedObjects.first().id
        val selected = CountingEngine.selectObject(state, id)
        assertSame(selected, CountingEngine.selectObject(selected, id))
        assertEquals(1, selected.currentCount)
    }

    @Test
    fun `arbitrary object order counts selected quantity rather than position`() {
        val state = stateForTarget(CountingLevel.Level2, 4)
        val arbitraryIds = state.displayedObjects.map(CountingObjectInstance::id).reversed().take(4)
        var current = state
        arbitraryIds.forEachIndexed { index, id ->
            current = CountingEngine.selectObject(current, id)
            assertEquals(index + 1, current.currentCount)
        }
    }

    @Test
    fun `invalid object ID is ignored safely`() {
        val state = CountingEngine.newGame(CountingLevel.Level1, Random(2))
        assertSame(state, CountingEngine.selectObject(state, -1))
        assertSame(state, CountingEngine.selectObject(state, Int.MAX_VALUE))
    }

    @Test
    fun `incomplete selection does not complete round`() {
        val state = stateForTarget(CountingLevel.Level2, 3)
        val selected = CountingEngine.selectObject(state, state.displayedObjects.first().id)
        assertFalse(selected.roundComplete)
        assertFalse(selected.inputLocked)
        assertEquals(0, selected.completedRoundCount)
    }

    @Test
    fun `reaching target completes and locks the round exactly once`() {
        val completed = completeRound(stateForTarget(CountingLevel.Level2, 3))
        assertTrue(completed.roundComplete)
        assertTrue(completed.inputLocked)
        assertEquals(3, completed.currentCount)
        assertEquals(1, completed.completedRoundCount)

        val extraId = completed.displayedObjects.first { it.id !in completed.selectedObjectIds }.id
        assertSame(completed, CountingEngine.selectObject(completed, extraId))
        assertEquals(completed.targetCount, completed.currentCount)
        assertEquals(1, completed.completedRoundCount)
    }

    @Test
    fun `start over clears interaction state and preserves the same round`() {
        var state = stateForTarget(CountingLevel.Level2, 3)
        state = CountingEngine.selectObject(state, state.displayedObjects.first().id)
        state = CountingEngine.requestHelp(state)
        val reset = CountingEngine.startOver(state)

        assertTrue(reset.selectedObjectIds.isEmpty())
        assertEquals(0, reset.helpCount)
        assertFalse(reset.roundComplete)
        assertFalse(reset.inputLocked)
        assertEquals(state.level, reset.level)
        assertEquals(state.targetCount, reset.targetCount)
        assertEquals(state.objectIdentity, reset.objectIdentity)
        assertEquals(state.displayedObjects, reset.displayedObjects)
        assertEquals(state.completedRoundCount, reset.completedRoundCount)
    }

    @Test
    fun `help increments and next round resets help`() {
        var state = stateForTarget(CountingLevel.Level2, 3)
        state = CountingEngine.requestHelp(state)
        state = CountingEngine.requestHelp(state)
        assertEquals(2, state.helpCount)

        state = completeRound(state)
        val next = CountingEngine.nextRound(state, Random(44))
        assertEquals(0, next.helpCount)
        assertTrue(next.selectedObjectIds.isEmpty())
    }

    @Test
    fun `next round retains completed progress without incrementing it again`() {
        val completed = completeRound(stateForTarget(CountingLevel.Level1, 2))
        val next = CountingEngine.nextRound(completed, Random(7))
        assertEquals(1, completed.completedRoundCount)
        assertEquals(1, next.completedRoundCount)
        assertFalse(next.roundComplete)
    }

    @Test
    fun `session completes after exactly five successful rounds`() {
        var state = CountingEngine.newGame(CountingLevel.Level1, Random(5))
        repeat(5) { round ->
            state = completeRound(state)
            assertEquals(round + 1, state.completedRoundCount)
            assertEquals(round == 4, state.sessionComplete)
            if (round < 4) state = CountingEngine.nextRound(state, Random(100 + round))
        }
        assertTrue(state.inputLocked)
    }

    @Test
    fun `count some more resets progress and opens a fresh round`() {
        val completeSession = completeSession(CountingLevel.Level1)
        val fresh = CountingEngine.newSession(completeSession, Random(77))
        assertEquals(0, fresh.completedRoundCount)
        assertTrue(fresh.selectedObjectIds.isEmpty())
        assertFalse(fresh.roundComplete)
        assertFalse(fresh.sessionComplete)
        assertFalse(fresh.inputLocked)
    }

    @Test
    fun `new rounds avoid immediate target and object repetition`() {
        CountingLevel.entries.forEach { level ->
            var state = CountingEngine.newGame(level, Random(level.ordinal))
            repeat(20) { round ->
                val previousTarget = state.targetCount
                val previousObject = state.objectIdentity
                state = completeRound(state)
                if (state.sessionComplete) {
                    state = CountingEngine.newSession(state, Random(500 + round))
                } else {
                    state = CountingEngine.nextRound(state, Random(500 + round))
                }
                assertNotEquals(previousTarget, state.targetCount)
                assertNotEquals(previousObject, state.objectIdentity)
            }
        }
    }

    @Test
    fun `injected randomness makes rounds deterministic`() {
        val first = CountingEngine.newGame(CountingLevel.Level3, Random(12345))
        val second = CountingEngine.newGame(CountingLevel.Level3, Random(12345))
        assertEquals(first, second)

        val firstNext = CountingEngine.nextRound(completeRound(first), Random(6789))
        val secondNext = CountingEngine.nextRound(completeRound(second), Random(6789))
        assertEquals(firstNext, secondNext)
    }

    @Test
    fun `rapid repeated selections cannot exceed target or corrupt state`() {
        var state = stateForTarget(CountingLevel.Level3, 7)
        repeat(100) { index ->
            val objectId = state.displayedObjects[index % state.displayedObjects.size].id
            state = CountingEngine.selectObject(state, objectId)
            assertTrue(state.currentCount <= state.targetCount)
            assertEquals(state.currentCount, state.selectedObjectIds.size)
        }
        assertEquals(7, state.currentCount)
        assertTrue(state.roundComplete)
        assertEquals(1, state.completedRoundCount)
    }

    @Test
    fun `high target ten completes correctly with bounded display count`() {
        val state = stateForTarget(CountingLevel.Level3, 10)
        assertEquals(12, state.displayedObjects.size)
        val completed = completeRound(state)
        assertEquals(10, completed.currentCount)
        assertTrue(completed.roundComplete)
    }

    @Test
    fun `randomized sessions preserve all engine invariants`() {
        repeat(50) { sessionSeed ->
            var state = CountingEngine.newGame(CountingLevel.Level3, Random(sessionSeed))
            repeat(15) { round ->
                assertInvariants(state)
                state.displayedObjects.shuffled(Random(sessionSeed + round)).forEach { objectInstance ->
                    state = CountingEngine.selectObject(state, objectInstance.id)
                    assertInvariants(state)
                }
                state = if (state.sessionComplete) {
                    CountingEngine.newSession(state, Random(sessionSeed * 100 + round))
                } else {
                    CountingEngine.nextRound(state, Random(sessionSeed * 100 + round))
                }
            }
        }
    }

    @Test
    fun `object catalog is complete cheerful and local`() {
        assertEquals(8, CountingObject.entries.size)
        assertEquals(8, CountingObject.entries.map(CountingObject::displayName).distinct().size)
        CountingObject.entries.forEach { objectIdentity ->
            assertTrue(objectIdentity.displayName.isNotBlank())
            assertTrue(objectIdentity.pluralName.isNotBlank())
            assertTrue(objectIdentity.visualSymbol.isNotBlank())
            assertFalse("://" in objectIdentity.visualSymbol)
        }
    }

    @Test
    fun `speech maps one through ten and rejects unsupported counts`() {
        assertEquals(
            listOf("One.", "Two.", "Three.", "Four.", "Five.", "Six.", "Seven.", "Eight.", "Nine.", "Ten."),
            (1..10).map(CountingSpeech::spokenCount),
        )
        assertNull(CountingSpeech.spokenCount(0))
        assertNull(CountingSpeech.spokenCount(11))
    }

    @Test
    fun `request and success speech use singular and plural names`() {
        val singular = stateForTarget(CountingLevel.Level1, 1, CountingObject.Apple)
        val plural = stateForTarget(CountingLevel.Level2, 4, CountingObject.Star)
        assertEquals("Can you find 1 apple?", CountingSpeech.roundRequest(singular))
        assertEquals("Can you find 4 stars?", CountingSpeech.roundRequest(plural))
        assertEquals(
            "You did it! You found 4 stars! Great job, Olivia!",
            CountingSpeech.success(plural),
        )
        assertEquals(
            "Tap the stars one at a time. Let's count together!",
            CountingSpeech.help(plural),
        )
    }

    private fun stateForTarget(
        level: CountingLevel,
        target: Int,
        objectIdentity: CountingObject? = null,
    ): CountingState {
        repeat(20_000) { seed ->
            val candidate = CountingEngine.newGame(level, Random(seed))
            if (
                candidate.targetCount == target &&
                (objectIdentity == null || candidate.objectIdentity == objectIdentity)
            ) {
                return candidate
            }
        }
        error("Unable to generate requested deterministic test round")
    }

    private fun completeRound(initial: CountingState): CountingState {
        var state = initial
        initial.displayedObjects.take(initial.targetCount).forEach { objectInstance ->
            state = CountingEngine.selectObject(state, objectInstance.id)
        }
        return state
    }

    private fun completeSession(level: CountingLevel): CountingState {
        var state = CountingEngine.newGame(level, Random(19))
        repeat(5) { round ->
            state = completeRound(state)
            if (round < 4) state = CountingEngine.nextRound(state, Random(200 + round))
        }
        return state
    }

    private fun assertInvariants(state: CountingState) {
        assertTrue(state.targetCount in 1..state.level.maximumTarget)
        assertEquals(state.targetCount + CountingEngine.EXTRA_OBJECT_COUNT, state.displayedObjects.size)
        assertTrue(state.displayedObjects.all { it.identity == state.objectIdentity })
        assertEquals(
            state.displayedObjects.size,
            state.displayedObjects.map(CountingObjectInstance::id).distinct().size,
        )
        assertTrue(state.selectedObjectIds.all { selectedId ->
            state.displayedObjects.any { it.id == selectedId }
        })
        assertTrue(state.currentCount <= state.targetCount)
        assertEquals(state.currentCount == state.targetCount, state.roundComplete)
        assertEquals(state.roundComplete || state.sessionComplete, state.inputLocked)
        assertEquals(state.completedRoundCount == CountingEngine.ROUNDS_PER_SESSION, state.sessionComplete)
    }
}
