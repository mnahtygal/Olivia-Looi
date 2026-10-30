package com.nahtygal.olivialooi.games.tracing

/** Versioned, session-only payload used by Compose state restoration. */
object LetterTracingStateCodec {
    fun encode(state: LetterTracingState): String {
        require(state.isStructurallyValid())
        val positions = state.strokesByPosition.joinToString("~") { strokes ->
            strokes.joinToString(";") { stroke ->
                stroke.points.joinToString("/") { point -> "${point.x},${point.y}" }
            }
        }
        val celebrationHandled = state.completionCelebrated || state.celebrationPending
        return "3|${state.mode.name}|${state.letters.joinToString("")}|${state.letterPosition}|$celebrationHandled|$positions"
    }

    fun decode(payload: String): LetterTracingState? = when {
        payload.startsWith("3|") -> decodeVersion3(payload)
        payload.startsWith("2|") -> decodeVersion2NameState(payload)
        else -> null
    }

    private fun decodeVersion3(payload: String): LetterTracingState? = runCatching {
        val fields = payload.split('|', limit = 6)
        require(fields.size == 6)
        val mode = TracingMode.valueOf(fields[1])
        val letters = fields[2].toList()
        val positions = fields[5].split('~')
        require(positions.size == letters.size)
        val state = LetterTracingState(
            mode = mode,
            letters = letters,
            letterPosition = fields[3].toInt(),
            completionCelebrated = fields[4].toBooleanStrict(),
            celebrationPending = false,
            strokesByPosition = decodePositions(positions),
        )
        require(state.isStructurallyValid())
        state
    }.getOrNull()

    /** Build 1 payloads had no mode and always represented personalized name practice. */
    private fun decodeVersion2NameState(payload: String): LetterTracingState? = runCatching {
        val fields = payload.split('|', limit = 5)
        require(fields.size == 5)
        val letters = fields[1].toList()
        val positions = fields[4].split('~')
        require(positions.size == letters.size)
        val state = LetterTracingState(
            mode = TracingMode.NAME,
            letters = letters,
            letterPosition = fields[2].toInt(),
            completionCelebrated = fields[3].toBooleanStrict(),
            strokesByPosition = decodePositions(positions),
        )
        require(state.isStructurallyValid())
        state
    }.getOrNull()

    private fun decodePositions(positions: List<String>): List<List<TracingStroke>> =
        positions.map { savedPosition ->
            if (savedPosition.isEmpty()) emptyList() else savedPosition.split(';').map { savedStroke ->
                val points = savedStroke.split('/').map { savedPoint ->
                    val coordinates = savedPoint.split(',', limit = 2)
                    require(coordinates.size == 2)
                    TracingPoint(coordinates[0].toFloat(), coordinates[1].toFloat())
                }
                TracingStroke(points)
            }
        }
}
