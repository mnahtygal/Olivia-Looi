package com.nahtygal.olivialooi.games.puzzles

/** Versioned primitive payload for rememberSaveable; no disk storage. */
object PuzzleStateCodec {
    fun encode(state: PuzzleState): String = listOf(
        "1",
        state.picture.stableId,
        state.difficulty.name,
        state.pieces.filter { it.isPlaced }.joinToString(",") { it.id.toString() },
        state.trayOrder.joinToString(","),
        state.selectedPieceId?.toString().orEmpty(),
        state.attemptIdentity.toString(),
        state.spokenMilestones.sortedBy { it.ordinal }.joinToString(",") { it.name },
    ).joinToString("|")

    fun decode(payload: String): PuzzleState? = runCatching {
        val fields = payload.split('|')
        require(fields.size == 8 && fields[0] == "1")
        val picture = requireNotNull(PuzzleCatalog.findById(fields[1]))
        val difficulty = PuzzleDifficulty.valueOf(fields[2])
        fun ids(value: String): List<Int> = if (value.isEmpty()) emptyList() else value.split(',').map { it.toInt() }
        val placed = ids(fields[3])
        val tray = ids(fields[4])
        val all = placed + tray
        require(all.size == difficulty.pieceCount && all.toSet() == (0 until difficulty.pieceCount).toSet())
        val selected = fields[5].takeIf { it.isNotEmpty() }?.toInt()
        require(selected == null || selected in tray)
        val attempt = fields[6].toLong()
        require(attempt >= 0)
        val milestones = if (fields[7].isEmpty()) emptySet() else fields[7].split(',').map { PuzzleMilestone.valueOf(it) }.toSet()
        PuzzleState(
            picture, difficulty,
            PuzzleEngine.pieces(difficulty).map { it.copy(isPlaced = it.id in placed) },
            tray, selected, attempt, milestones,
        ).also { state ->
            require(milestones.all { it in state.copy(spokenMilestones = emptySet()).pendingMilestones })
        }
    }.getOrNull()
}
