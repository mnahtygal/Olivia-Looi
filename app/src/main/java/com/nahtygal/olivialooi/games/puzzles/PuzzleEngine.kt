package com.nahtygal.olivialooi.games.puzzles

import kotlin.random.Random

data class PuzzlePiece(
    val id: Int,
    val sourceRow: Int,
    val sourceColumn: Int,
    val targetIndex: Int,
    val isPlaced: Boolean = false,
)

enum class PuzzleMilestone { OPENING, FIRST_PIECE, HALFWAY, COMPLETE }

data class PuzzleState(
    val picture: PuzzlePicture,
    val difficulty: PuzzleDifficulty,
    val pieces: List<PuzzlePiece>,
    val trayOrder: List<Int>,
    val selectedPieceId: Int? = null,
    val attemptIdentity: Long = 0,
    val spokenMilestones: Set<PuzzleMilestone> = emptySet(),
) {
    val rows: Int get() = difficulty.rows
    val columns: Int get() = difficulty.columns
    val totalPieceCount: Int get() = difficulty.pieceCount
    val completedPieceCount: Int get() = pieces.count { it.isPlaced }
    val isComplete: Boolean get() = completedPieceCount == totalPieceCount
    val needsLeaveConfirmation: Boolean get() = completedPieceCount > 0 && !isComplete
    val pendingMilestones: List<PuzzleMilestone>
        get() = buildList {
            add(PuzzleMilestone.OPENING)
            if (completedPieceCount >= 1) add(PuzzleMilestone.FIRST_PIECE)
            if (completedPieceCount >= (totalPieceCount + 1) / 2) add(PuzzleMilestone.HALFWAY)
            if (isComplete) add(PuzzleMilestone.COMPLETE)
        }.filterNot { it in spokenMilestones }
}

object PuzzleEngine {
    internal fun pieces(difficulty: PuzzleDifficulty): List<PuzzlePiece> =
        List(difficulty.pieceCount) { index ->
            PuzzlePiece(index, index / difficulty.columns, index % difficulty.columns, index)
        }

    fun newPuzzle(
        picture: PuzzlePicture,
        difficulty: PuzzleDifficulty,
        random: Random = Random.Default,
    ): PuzzleState {
        val pieces = pieces(difficulty)
        return PuzzleState(picture, difficulty, pieces, pieces.map { it.id }.shuffled(random))
    }

    fun selectPiece(state: PuzzleState, pieceId: Int): PuzzleState {
        if (state.isComplete || state.pieces.none { it.id == pieceId && !it.isPlaced }) return state
        return if (state.selectedPieceId == pieceId) state else state.copy(selectedPieceId = pieceId)
    }

    /** Both drag and tap use semantic identity; coordinates never enter this engine.
     * A gesture can carry its starting identity to reject stale callbacks after reset/placement.
     */
    fun placePiece(
        state: PuzzleState,
        pieceId: Int,
        targetCell: Int,
        expectedAttemptIdentity: Long = state.attemptIdentity,
    ): PuzzleState {
        if (state.isComplete || expectedAttemptIdentity != state.attemptIdentity) return state
        val piece = state.pieces.find { it.id == pieceId && !it.isPlaced } ?: return state
        if (targetCell != piece.targetIndex) {
            return state.copy(attemptIdentity = state.attemptIdentity + 1)
        }
        return state.copy(
            pieces = state.pieces.map { if (it.id == pieceId) it.copy(isPlaced = true) else it },
            trayOrder = state.trayOrder.filterNot { it == pieceId },
            selectedPieceId = null,
            attemptIdentity = state.attemptIdentity + 1,
        )
    }

    fun placeSelectedPiece(state: PuzzleState, targetCell: Int): PuzzleState =
        state.selectedPieceId?.let { placePiece(state, it, targetCell) } ?: state

    fun acknowledgeMilestones(state: PuzzleState): PuzzleState =
        if (state.pendingMilestones.isEmpty()) state
        else state.copy(spokenMilestones = state.spokenMilestones + state.pendingMilestones)

    fun startOver(state: PuzzleState, random: Random = Random.Default): PuzzleState =
        newPuzzle(state.picture, state.difficulty, random).copy(attemptIdentity = state.attemptIdentity + 1)
}

object PuzzleSpeech {
    fun phrase(picture: PuzzlePicture, milestone: PuzzleMilestone): String = when (milestone) {
        PuzzleMilestone.OPENING -> "Let’s build the ${picture.spokenName} puzzle!"
        PuzzleMilestone.FIRST_PIECE -> "Great start!"
        PuzzleMilestone.HALFWAY -> "You’re doing great!"
        PuzzleMilestone.COMPLETE -> "You did it, Olivia! Great puzzle!"
    }
    const val PREVIEW = "Here’s the picture!"
}
