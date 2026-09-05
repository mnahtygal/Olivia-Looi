package com.nahtygal.olivialooi.games.shapes

/** Compact saveable payload; no disk persistence or platform types. */
object ShapesStateCodec {
    private fun choice(id: String): ShapeChoice {
        val parts = id.split(':')
        return ShapeChoice(requireNotNull(ShapeCatalog.findById(parts[0])), requireNotNull(ColorCatalog.findById(parts[1])))
    }

    fun encode(state: ShapesState): String =
        listOf(
            state.selectedMode.name,
            state.currentTarget.stableId,
            state.previousTarget?.stableId.orEmpty(),
            state.answerChoices.joinToString(",", transform = ShapeChoice::stableId),
            state.selectedChoiceId.orEmpty(),
            state.sessionStars,
            state.completedRoundCount,
            state.roundState.name,
            state.helpCount,
            state.attemptIdentity,
            state.awardedAttemptIdentity?.toString().orEmpty(),
            state.sessionComplete,
            state.learnIndex,
        ).joinToString("|")

    fun decode(saved: String): ShapesState? =
        runCatching {
            val fields = saved.split('|')
            ShapesState(
                selectedMode = enumValueOf<ShapesMode>(fields[0]),
                currentTarget = choice(fields[1]),
                previousTarget = fields[2]
                    .takeIf(String::isNotEmpty)
                    ?.let { choice(it) },
                answerChoices = fields[3]
                    .takeIf(String::isNotEmpty)
                    ?.split(',')
                    ?.map { choice(it) }
                    .orEmpty(),
                selectedChoiceId = fields[4].takeIf(String::isNotEmpty),
                sessionStars = fields[5].toInt(),
                completedRoundCount = fields[6].toInt(),
                roundState = enumValueOf<ShapesRoundState>(fields[7]),
                helpCount = fields[8].toInt(),
                attemptIdentity = fields[9].toLong(),
                awardedAttemptIdentity = fields[10].toLongOrNull(),
                sessionComplete = fields[11].toBoolean(),
                learnIndex = fields[12].toInt(),
            )
        }.getOrNull()
}
