package com.nahtygal.olivialooi.games.coloring

enum class ColoringPicture {
    Butterfly,
    Flower,
    Puppy,
    Snowman,
}

enum class ColoringColor(val argb: Int) {
    Red(0xFFE53935.toInt()),
    Orange(0xFFFF8C24.toInt()),
    Yellow(0xFFFFD93D.toInt()),
    Green(0xFF43A047.toInt()),
    Blue(0xFF3185E1.toInt()),
    Purple(0xFF7B61D1.toInt()),
    Pink(0xFFEC5FA6.toInt()),
    Brown(0xFF8D5A3B.toInt()),
    Black(0xFF20202A.toInt()),
    White(0xFFFFFFFF.toInt()),
}

enum class ColoringBrush(val widthDp: Float) {
    Small(10f),
    Medium(20f),
    Big(34f),
}

data class ColoringPoint(
    val x: Float,
    val y: Float,
) {
    val isValid: Boolean
        get() = x.isFinite() && y.isFinite() && x in 0f..1f && y in 0f..1f
}

data class ColoringStroke(
    val color: ColoringColor,
    val brush: ColoringBrush,
    val points: List<ColoringPoint>,
)

data class ColoringState(
    val picture: ColoringPicture,
    val selectedColor: ColoringColor = ColoringColor.Purple,
    val selectedBrush: ColoringBrush = ColoringBrush.Medium,
    val strokes: List<ColoringStroke> = emptyList(),
    val activeStroke: ColoringStroke? = null,
)

object ColoringEngine {
    fun newPicture(picture: ColoringPicture): ColoringState = ColoringState(picture = picture)

    fun selectColor(state: ColoringState, color: ColoringColor): ColoringState =
        state.copy(selectedColor = color)

    fun selectBrush(state: ColoringState, brush: ColoringBrush): ColoringState =
        state.copy(selectedBrush = brush)

    fun startStroke(state: ColoringState, point: ColoringPoint): ColoringState {
        if (state.activeStroke != null || !point.isValid) return state
        return state.copy(
            activeStroke = ColoringStroke(
                color = state.selectedColor,
                brush = state.selectedBrush,
                points = listOf(point),
            ),
        )
    }

    fun appendPoint(state: ColoringState, point: ColoringPoint): ColoringState {
        val active = state.activeStroke ?: return state
        if (!point.isValid) return state
        return state.copy(activeStroke = active.copy(points = active.points + point))
    }

    fun finishStroke(state: ColoringState): ColoringState {
        val active = state.activeStroke ?: return state
        if (active.points.isEmpty()) return state.copy(activeStroke = null)
        return state.copy(
            strokes = state.strokes + active,
            activeStroke = null,
        )
    }

    /** Efficient UI path: validates and commits one completed gesture in a single state update. */
    fun addCompletedStroke(
        state: ColoringState,
        points: List<ColoringPoint>,
    ): ColoringState {
        if (state.activeStroke != null || points.isEmpty() || points.any { !it.isValid }) return state
        return state.copy(
            strokes = state.strokes + ColoringStroke(
                color = state.selectedColor,
                brush = state.selectedBrush,
                points = points.toList(),
            ),
        )
    }

    fun undo(state: ColoringState): ColoringState = when {
        state.activeStroke != null -> state.copy(activeStroke = null)
        state.strokes.isEmpty() -> state
        else -> state.copy(strokes = state.strokes.dropLast(1))
    }

    fun clear(state: ColoringState): ColoringState {
        if (state.strokes.isEmpty() && state.activeStroke == null) return state
        return state.copy(strokes = emptyList(), activeStroke = null)
    }

    fun resetPicture(state: ColoringState): ColoringState = newPicture(state.picture)
}
