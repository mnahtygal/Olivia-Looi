package com.nahtygal.olivialooi.games.puzzles

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Every transition/invariant runs independently at 6, 9, and 12 pieces. */
@RunWith(Parameterized::class)
class PuzzleEngineTest(private val difficulty: PuzzleDifficulty) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun difficulties(): List<Array<PuzzleDifficulty>> = PuzzleDifficulty.entries.map { arrayOf(it) }
    }
    private fun fresh(seed: Int = 42, picture: PuzzlePicture = PuzzlePicture.BUTTERFLY) =
        PuzzleEngine.newPuzzle(picture, difficulty, Random(seed))
    private fun place(state: PuzzleState, id: Int) = PuzzleEngine.placePiece(state, id, state.pieces.first { it.id == id }.targetIndex)
    private fun through(count: Int): PuzzleState = (0 until count).fold(fresh()) { state, id -> place(state, id) }
    private fun complete() = through(difficulty.pieceCount)
    private fun acknowledge(state: PuzzleState) = PuzzleEngine.acknowledgeMilestones(state)
    private fun restored(state: PuzzleState) = requireNotNull(PuzzleStateCodec.decode(PuzzleStateCodec.encode(state)))
    private fun valid(state: PuzzleState) {
        assertEquals(difficulty.pieceCount, state.pieces.size)
        assertEquals(state.pieces.count { it.isPlaced }, state.completedPieceCount)
        assertTrue(state.completedPieceCount in 0..state.totalPieceCount)
        assertEquals(state.completedPieceCount == difficulty.pieceCount, state.isComplete)
        assertEquals((0 until difficulty.pieceCount).toSet(), state.pieces.map { it.targetIndex }.toSet())
        assertEquals(state.pieces.filterNot { it.isPlaced }.map { it.id }.toSet(), state.trayOrder.toSet())
        assertEquals(state.trayOrder.size, state.trayOrder.toSet().size)
        assertTrue(state.selectedPieceId == null || state.selectedPieceId in state.trayOrder)
        state.pieces.forEach {
            assertEquals(it.targetIndex / state.columns, it.sourceRow)
            assertEquals(it.targetIndex % state.columns, it.sourceColumn)
        }
    }

    @Test fun `fresh piece count exact`() { assertEquals(difficulty.pieceCount, fresh().pieces.size) }
    @Test fun `fresh IDs unique`() { assertEquals(difficulty.pieceCount, fresh().pieces.map { it.id }.toSet().size) }
    @Test fun `targets unique`() { assertEquals(difficulty.pieceCount, fresh().pieces.map { it.targetIndex }.toSet().size) }
    @Test fun `targets in range with correct source coordinates`() { valid(fresh()) }
    @Test fun `no pieces initially placed`() { assertTrue(fresh().pieces.none { it.isPlaced }) }
    @Test fun `zero initial progress`() { assertEquals(0, fresh().completedPieceCount) }
    @Test fun `initially incomplete`() { assertFalse(fresh().isComplete) }
    @Test fun `tray initially contains all pieces`() { assertEquals(fresh().pieces.map { it.id }.toSet(), fresh().trayOrder.toSet()) }
    @Test fun `injected randomness deterministic`() { assertEquals(fresh(18), fresh(18)) }
    @Test fun `different seeds vary tray ordering`() { assertTrue((0..20).map { fresh(it).trayOrder }.toSet().size > 1) }

    @Test fun `correct placement succeeds`() {
        val state = place(fresh(), 2)
        assertTrue(state.pieces.first { it.id == 2 }.isPlaced)
        assertEquals(1, state.completedPieceCount)
        assertFalse(2 in state.trayOrder)
    }
    @Test fun `duplicate placement cannot increment progress`() {
        val once = place(fresh(), 2)
        assertSame(once, place(once, 2))
        assertEquals(1, once.completedPieceCount)
    }
    @Test fun `wrong cell retains pieces targets and progress`() {
        val before = fresh()
        val after = PuzzleEngine.placePiece(before, 0, 1)
        assertEquals(before.pieces, after.pieces)
        assertEquals(before.trayOrder, after.trayOrder)
        assertEquals(0, after.completedPieceCount)
        assertEquals(before.attemptIdentity + 1, after.attemptIdentity)
    }
    @Test fun `valid piece after incorrect attempt succeeds`() {
        val wrong = PuzzleEngine.placePiece(fresh(), 0, 1)
        assertEquals(1, place(wrong, 1).completedPieceCount)
        assertEquals(1, place(wrong, 0).completedPieceCount)
    }
    @Test fun `placement order independent`() {
        var state = fresh()
        state.pieces.shuffled(Random(999)).forEach { state = place(state, it.id) }
        assertTrue(state.isComplete)
        valid(state)
    }
    @Test fun `outside board returns piece without progress`() {
        listOf(-1, difficulty.pieceCount, Int.MAX_VALUE).forEach { cell ->
            val before = fresh()
            val after = PuzzleEngine.placePiece(before, 0, cell)
            assertEquals(before.pieces, after.pieces)
            assertEquals(before.trayOrder, after.trayOrder)
            assertEquals(0, after.completedPieceCount)
        }
    }
    @Test fun `unknown piece ignored`() { val state = fresh(); assertSame(state, PuzzleEngine.placePiece(state, -1, 0)) }
    @Test fun `filled cell cannot accept another piece`() {
        val state = place(fresh(), 0)
        assertEquals(state.pieces, PuzzleEngine.placePiece(state, 1, 0).pieces)
    }

    @Test fun `only all pieces complete puzzle`() {
        var state = fresh()
        repeat(difficulty.pieceCount) { id ->
            assertFalse(state.isComplete)
            state = place(state, id)
            assertEquals(id == difficulty.pieceCount - 1, state.isComplete)
        }
        assertEquals(difficulty.pieceCount, state.completedPieceCount)
        assertTrue(state.trayOrder.isEmpty())
    }
    @Test fun `penultimate placement incomplete`() { assertFalse(through(difficulty.pieceCount - 1).isComplete) }
    @Test fun `final placement exactly once`() {
        val state = through(difficulty.pieceCount - 1)
        val final = place(state, difficulty.pieceCount - 1)
        assertTrue(final.isComplete)
        assertSame(final, place(final, difficulty.pieceCount - 1))
    }
    @Test fun `completion locks every placement and selection`() {
        val state = complete()
        state.pieces.forEach {
            assertSame(state, PuzzleEngine.placePiece(state, it.id, it.targetIndex))
            assertSame(state, PuzzleEngine.selectPiece(state, it.id))
        }
    }

    @Test fun `reset clears placed pieces and progress`() {
        val reset = PuzzleEngine.startOver(through(3), Random(8))
        assertEquals(0, reset.completedPieceCount)
        assertTrue(reset.pieces.none { it.isPlaced })
        assertFalse(reset.isComplete)
    }
    @Test fun `reset preserves picture and difficulty`() {
        PuzzlePicture.entries.forEach { picture ->
            val reset = PuzzleEngine.startOver(place(fresh(picture = picture), 1))
            assertEquals(picture, reset.picture)
            assertEquals(difficulty, reset.difficulty)
        }
    }
    @Test fun `reset restores entire tray and clears selection`() {
        val selected = PuzzleEngine.selectPiece(through(2), 3)
        val reset = PuzzleEngine.startOver(selected)
        assertNull(reset.selectedPieceId)
        assertEquals(difficulty.pieceCount, reset.trayOrder.size)
        valid(reset)
    }
    @Test fun `puzzle again valid fresh same settings`() {
        val reset = PuzzleEngine.startOver(complete(), Random(33))
        assertEquals(fresh().picture, reset.picture)
        assertEquals(difficulty, reset.difficulty)
        assertEquals(0, reset.completedPieceCount)
        valid(reset)
    }
    @Test fun `reset reshuffle uses injected random`() {
        val done = complete()
        assertEquals(PuzzleEngine.startOver(done, Random(7)), PuzzleEngine.startOver(done, Random(7)))
        assertTrue((0..10).map { PuzzleEngine.startOver(done, Random(it)).trayOrder }.toSet().size > 1)
    }

    @Test fun `select valid unplaced piece`() { assertEquals(2, PuzzleEngine.selectPiece(fresh(), 2).selectedPieceId) }
    @Test fun `change selection safely`() {
        val state = PuzzleEngine.selectPiece(PuzzleEngine.selectPiece(fresh(), 1), 2)
        assertEquals(2, state.selectedPieceId)
        assertEquals(fresh().pieces, state.pieces)
        assertEquals(fresh().trayOrder, state.trayOrder)
    }
    @Test fun `placed piece cannot be selected`() {
        val state = PuzzleEngine.selectPiece(place(fresh(), 1), 2)
        assertSame(state, PuzzleEngine.selectPiece(state, 1))
    }
    @Test fun `unknown selection ignored`() { val state = fresh(); assertSame(state, PuzzleEngine.selectPiece(state, 99)) }
    @Test fun `tap successful placement clears selection`() {
        val state = PuzzleEngine.placeSelectedPiece(PuzzleEngine.selectPiece(fresh(), 2), 2)
        assertNull(state.selectedPieceId)
        assertEquals(1, state.completedPieceCount)
    }
    @Test fun `tap incorrect placement retains selection`() {
        val before = PuzzleEngine.selectPiece(fresh(), 2)
        val after = PuzzleEngine.placeSelectedPiece(before, 3)
        assertEquals(2, after.selectedPieceId)
        assertEquals(before.pieces, after.pieces)
        assertEquals(before.trayOrder, after.trayOrder)
    }
    @Test fun `tap without selection ignored`() { val state = fresh(); assertSame(state, PuzzleEngine.placeSelectedPiece(state, 0)) }
    @Test fun `tap and drag share identical placement transition`() {
        val before = PuzzleEngine.selectPiece(fresh(), 2)
        assertEquals(PuzzleEngine.placePiece(before, 2, 2), PuzzleEngine.placeSelectedPiece(before, 2))
    }

    @Test fun `randomized transitions preserve every invariant`() {
        val random = Random(9001)
        var state = fresh()
        val targets = state.pieces.map { Triple(it.id, it.sourceRow, it.sourceColumn) to it.targetIndex }
        repeat(1000) {
            state = when (random.nextInt(5)) {
                0 -> PuzzleEngine.selectPiece(state, random.nextInt(difficulty.pieceCount))
                1 -> PuzzleEngine.placeSelectedPiece(state, random.nextInt(difficulty.pieceCount))
                2 -> PuzzleEngine.startOver(state, random)
                else -> PuzzleEngine.placePiece(state, random.nextInt(difficulty.pieceCount), random.nextInt(difficulty.pieceCount))
            }
            valid(state)
            assertEquals(targets, state.pieces.map { Triple(it.id, it.sourceRow, it.sourceColumn) to it.targetIndex })
        }
    }
    @Test fun `rapid duplicates and stale gesture do not duplicate progress`() {
        val before = fresh()
        var state = PuzzleEngine.placePiece(before, 0, 0, before.attemptIdentity)
        repeat(100) { state = PuzzleEngine.placePiece(state, 0, 0, before.attemptIdentity) }
        assertEquals(1, state.completedPieceCount)
        assertEquals(1L, state.attemptIdentity)
    }
    @Test fun `stale gesture rejected after reset`() {
        val before = fresh()
        val reset = PuzzleEngine.startOver(before)
        assertSame(reset, PuzzleEngine.placePiece(reset, 1, 1, before.attemptIdentity))
    }
    @Test fun `stale failed attempt cannot place a later piece`() {
        val before = fresh()
        val wrong = PuzzleEngine.placePiece(before, 0, 1)
        assertSame(wrong, PuzzleEngine.placePiece(wrong, 1, 1, before.attemptIdentity))
    }

    @Test fun `opening milestone once`() {
        val state = fresh()
        assertEquals(listOf(PuzzleMilestone.OPENING), state.pendingMilestones)
        val marked = acknowledge(state)
        assertTrue(marked.pendingMilestones.isEmpty())
        assertSame(marked, acknowledge(marked))
    }
    @Test fun `first piece milestone once`() {
        val first = place(acknowledge(fresh()), 0)
        assertEquals(listOf(PuzzleMilestone.FIRST_PIECE), first.pendingMilestones)
        val marked = acknowledge(first)
        assertTrue(place(marked, 1).pendingMilestones.isEmpty())
    }
    @Test fun `halfway milestone once at rounded threshold`() {
        val halfway = (difficulty.pieceCount + 1) / 2
        var state = acknowledge(fresh())
        repeat(halfway - 1) { state = acknowledge(place(state, it)) }
        assertTrue(state.pendingMilestones.isEmpty())
        state = place(state, halfway - 1)
        assertEquals(listOf(PuzzleMilestone.HALFWAY), state.pendingMilestones)
        assertTrue(place(acknowledge(state), halfway).pendingMilestones.isEmpty())
    }
    @Test fun `completion milestone once`() {
        val state = place(acknowledge(through(difficulty.pieceCount - 1)), difficulty.pieceCount - 1)
        assertEquals(listOf(PuzzleMilestone.COMPLETE), state.pendingMilestones)
        val marked = acknowledge(state)
        assertTrue(place(marked, difficulty.pieceCount - 1).pendingMilestones.isEmpty())
    }
    @Test fun `incorrect placement does not trigger encouragement`() {
        val state = acknowledge(fresh())
        assertTrue(PuzzleEngine.placePiece(state, 0, 1).pendingMilestones.isEmpty())
    }
    @Test fun `reset clears milestones`() {
        val reset = PuzzleEngine.startOver(acknowledge(complete()))
        assertTrue(reset.spokenMilestones.isEmpty())
        assertEquals(listOf(PuzzleMilestone.OPENING), reset.pendingMilestones)
    }

    @Test fun `many randomized initializations valid for all pictures`() {
        repeat(300) { seed ->
            val state = fresh(seed, PuzzlePicture.entries[seed % 6])
            valid(state)
            assertEquals(0, state.completedPieceCount)
        }
    }
    @Test fun `random tray ordering never changes target mapping`() {
        val expected = fresh().pieces
        repeat(100) { assertEquals(expected, fresh(it).pieces) }
    }
    @Test fun `save restore retains tray target selection and progress`() {
        val selected = PuzzleEngine.selectPiece(through(3), 4)
        assertEquals(selected, restored(selected))
        assertEquals(fresh(), restored(fresh()))
    }
    @Test fun `save restore completion and milestones`() {
        val state = acknowledge(complete())
        assertEquals(state, restored(state))
        assertTrue(restored(state).pendingMilestones.isEmpty())
    }
    @Test fun `restored placement cannot count twice`() {
        val state = restored(place(fresh(), 1))
        assertSame(state, place(state, 1))
        assertEquals(1, state.completedPieceCount)
    }
    @Test fun `restored first and halfway speech not repeated`() {
        listOf(1, (difficulty.pieceCount + 1) / 2).forEach { count ->
            val state = restored(acknowledge(through(count)))
            assertTrue(state.pendingMilestones.isEmpty())
        }
    }
    @Test fun `no leave confirmation without placed pieces`() {
        assertFalse(fresh().needsLeaveConfirmation)
        assertFalse(PuzzleEngine.selectPiece(fresh(), 0).needsLeaveConfirmation)
        assertFalse(PuzzleEngine.placePiece(fresh(), 0, 1).needsLeaveConfirmation)
    }
    @Test fun `active progress needs leave confirmation`() { assertTrue(through(1).needsLeaveConfirmation) }
    @Test fun `finished puzzle can leave normally`() { assertFalse(complete().needsLeaveConfirmation) }
    @Test fun `codec rejects corrupted payloads`() {
        val fields = PuzzleStateCodec.encode(fresh()).split('|')
        fun changed(index: Int, value: String) = fields.toMutableList().also { it[index] = value }.joinToString("|")
        listOf("", "broken", changed(0, "2"), changed(1, "unknown"), changed(2, "EXPERT"),
            changed(3, "0"), changed(4, "0,0"), changed(5, "999"), changed(6, "-1"), changed(7, "COMPLETE"),
        ).forEach { assertNull(it, PuzzleStateCodec.decode(it)) }
    }
}
