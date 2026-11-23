package com.nahtygal.olivialooi.games.tracing

import java.util.Locale

data class TracingPoint(val x: Float, val y: Float) {
    val isValid: Boolean
        get() = x.isFinite() && y.isFinite() && x in 0f..1f && y in 0f..1f
}

data class TracingStroke(val points: List<TracingPoint>)

/** A normalized guide made of separate pen paths. */
data class LetterGuide(val letter: Char, val paths: List<List<TracingPoint>>)

data class TracingName(val displayName: String, val letters: List<Char>)

enum class TracingMode { NAME, ALPHABET, LOWERCASE }

data class LetterTracingState(
    val mode: TracingMode,
    val letters: List<Char>,
    val letterPosition: Int = 0,
    val strokesByPosition: List<List<TracingStroke>> = List(letters.size) { emptyList() },
    val completionCelebrated: Boolean = false,
    val celebrationPending: Boolean = false,
) {
    val currentLetter: Char get() = letters[letterPosition]
    val currentStrokes: List<TracingStroke> get() = strokesByPosition[letterPosition]
    val isComplete: Boolean get() = strokesByPosition.all(List<TracingStroke>::isNotEmpty)
}

val OLIVIA_SEQUENCE: List<Char> = listOf('O', 'L', 'I', 'V', 'I', 'A')
val ELIANA_SEQUENCE: List<Char> = listOf('E', 'L', 'I', 'A', 'N', 'A')
val ALPHABET_SEQUENCE: List<Char> = ('A'..'Z').toList()
val LOWERCASE_SEQUENCE: List<Char> = ('a'..'z').toList()

private fun line(vararg coordinates: Float): List<TracingPoint> {
    require(coordinates.size >= 4 && coordinates.size % 2 == 0)
    return coordinates.toList().chunked(2).map { (x, y) -> TracingPoint(x, y) }
}

private fun arc(
    centerX: Double,
    centerY: Double,
    radiusX: Double,
    radiusY: Double,
    startDegrees: Double,
    endDegrees: Double,
    steps: Int = 28,
): List<TracingPoint> = (0..steps).map { step ->
    val degrees = startDegrees + (endDegrees - startDegrees) * step / steps
    val radians = Math.toRadians(degrees)
    TracingPoint(
        (centerX + radiusX * kotlin.math.cos(radians)).toFloat(),
        (centerY + radiusY * kotlin.math.sin(radians)).toFloat(),
    )
}

private fun cubic(
    start: TracingPoint,
    control1: TracingPoint,
    control2: TracingPoint,
    end: TracingPoint,
    steps: Int = 18,
): List<TracingPoint> = (0..steps).map { step ->
    val t = step.toFloat() / steps
    val inverse = 1f - t
    TracingPoint(
        inverse * inverse * inverse * start.x +
            3f * inverse * inverse * t * control1.x +
            3f * inverse * t * t * control2.x + t * t * t * end.x,
        inverse * inverse * inverse * start.y +
            3f * inverse * inverse * t * control1.y +
            3f * inverse * t * t * control2.y + t * t * t * end.y,
    )
}

private fun join(first: List<TracingPoint>, second: List<TracingPoint>): List<TracingPoint> =
    first + second.drop(1)

private val I_GUIDE = LetterGuide(
    letter = 'I',
    paths = listOf(
        listOf(TracingPoint(.27f, .18f), TracingPoint(.73f, .18f)),
        listOf(TracingPoint(.50f, .18f), TracingPoint(.50f, .82f)),
        listOf(TracingPoint(.27f, .82f), TracingPoint(.73f, .82f)),
    ),
)

private val UPPERCASE_LETTER_GUIDES: Map<Char, LetterGuide> = mapOf(
    'O' to LetterGuide(
        'O',
        listOf(
            (0..48).map { step ->
                val angle = Math.PI * 2.0 * step / 48.0
                TracingPoint(
                    x = (.5 + .28 * kotlin.math.sin(angle)).toFloat(),
                    y = (.5 - .34 * kotlin.math.cos(angle)).toFloat(),
                )
            },
        ),
    ),
    'L' to LetterGuide(
        'L',
        listOf(
            listOf(TracingPoint(.34f, .17f), TracingPoint(.34f, .82f), TracingPoint(.72f, .82f)),
        ),
    ),
    'I' to I_GUIDE,
    'V' to LetterGuide(
        'V',
        listOf(
            listOf(TracingPoint(.25f, .17f), TracingPoint(.50f, .83f), TracingPoint(.75f, .17f)),
        ),
    ),
    'A' to LetterGuide(
        'A',
        listOf(
            listOf(TracingPoint(.22f, .83f), TracingPoint(.50f, .17f), TracingPoint(.78f, .83f)),
            listOf(TracingPoint(.34f, .59f), TracingPoint(.66f, .59f)),
        ),
    ),
    'E' to LetterGuide(
        'E',
        listOf(
            listOf(TracingPoint(.67f, .18f), TracingPoint(.32f, .18f), TracingPoint(.32f, .82f), TracingPoint(.70f, .82f)),
            listOf(TracingPoint(.32f, .50f), TracingPoint(.62f, .50f)),
        ),
    ),
    'N' to LetterGuide(
        'N',
        listOf(
            listOf(
                TracingPoint(.27f, .82f),
                TracingPoint(.27f, .18f),
                TracingPoint(.73f, .82f),
                TracingPoint(.73f, .18f),
            ),
        ),
    ),
    'B' to LetterGuide(
        'B',
        listOf(
            line(.30f, .18f, .30f, .82f),
            join(arc(.30, .34, .34, .16, -90.0, 90.0), arc(.30, .66, .34, .16, -90.0, 90.0)),
        ),
    ),
    'C' to LetterGuide('C', listOf(arc(.50, .50, .30, .34, -52.0, -308.0, 36))),
    'D' to LetterGuide(
        'D',
        listOf(line(.30f, .18f, .30f, .82f), arc(.30, .50, .38, .32, -90.0, 90.0, 34)),
    ),
    'F' to LetterGuide(
        'F',
        listOf(line(.68f, .18f, .31f, .18f, .31f, .82f), line(.31f, .49f, .61f, .49f)),
    ),
    'G' to LetterGuide(
        'G',
        listOf(arc(.50, .50, .30, .34, -52.0, -308.0, 36), line(.50f, .55f, .72f, .55f, .72f, .70f)),
    ),
    'H' to LetterGuide(
        'H',
        listOf(line(.28f, .18f, .28f, .82f), line(.72f, .18f, .72f, .82f), line(.28f, .50f, .72f, .50f)),
    ),
    'J' to LetterGuide(
        'J',
        listOf(
            line(.28f, .18f, .73f, .18f, .73f, .62f),
            cubic(TracingPoint(.73f, .62f), TracingPoint(.73f, .88f), TracingPoint(.30f, .90f), TracingPoint(.26f, .66f)),
        ),
    ),
    'K' to LetterGuide(
        'K',
        listOf(line(.29f, .18f, .29f, .82f), line(.72f, .18f, .29f, .53f, .73f, .82f)),
    ),
    'M' to LetterGuide(
        'M',
        listOf(line(.20f, .82f, .20f, .18f, .50f, .55f, .80f, .18f, .80f, .82f)),
    ),
    'P' to LetterGuide(
        'P',
        listOf(line(.30f, .82f, .30f, .18f), arc(.30, .35, .35, .17, -90.0, 90.0, 28)),
    ),
    'Q' to LetterGuide(
        'Q',
        listOf(
            arc(.50, .50, .28, .34, -90.0, 270.0, 44),
            line(.54f, .64f, .78f, .86f),
        ),
    ),
    'R' to LetterGuide(
        'R',
        listOf(
            line(.30f, .82f, .30f, .18f),
            arc(.30, .35, .35, .17, -90.0, 90.0, 28),
            line(.45f, .52f, .74f, .82f),
        ),
    ),
    'S' to LetterGuide(
        'S',
        listOf(
            join(
                join(
                    cubic(
                        TracingPoint(.72f, .25f), TracingPoint(.59f, .12f),
                        TracingPoint(.29f, .14f), TracingPoint(.28f, .37f),
                    ),
                    cubic(
                        TracingPoint(.28f, .37f), TracingPoint(.28f, .54f),
                        TracingPoint(.72f, .46f), TracingPoint(.72f, .67f),
                    ),
                ),
                cubic(
                    TracingPoint(.72f, .67f), TracingPoint(.72f, .88f),
                    TracingPoint(.37f, .90f), TracingPoint(.24f, .75f),
                ),
            ),
        ),
    ),
    'T' to LetterGuide('T', listOf(line(.22f, .18f, .78f, .18f), line(.50f, .18f, .50f, .82f))),
    'U' to LetterGuide(
        'U',
        listOf(
            join(
                join(line(.25f, .18f, .25f, .58f), arc(.50, .58, .25, .25, 180.0, 0.0, 28)),
                line(.75f, .58f, .75f, .18f),
            ),
        ),
    ),
    'W' to LetterGuide('W', listOf(line(.18f, .18f, .32f, .82f, .50f, .48f, .68f, .82f, .82f, .18f))),
    'X' to LetterGuide('X', listOf(line(.25f, .18f, .75f, .82f), line(.75f, .18f, .25f, .82f))),
    'Y' to LetterGuide('Y', listOf(line(.24f, .18f, .50f, .50f, .76f, .18f), line(.50f, .50f, .50f, .82f))),
    'Z' to LetterGuide('Z', listOf(line(.24f, .18f, .76f, .18f, .24f, .82f, .76f, .82f))),
)

private fun oval(centerX: Double, centerY: Double, radiusX: Double, radiusY: Double): List<TracingPoint> =
    arc(centerX, centerY, radiusX, radiusY, -90.0, 270.0, 40)

/** Preschool print forms share an ascender top (.15), x-height (.38), baseline (.72), and descender (.90). */
val LOWERCASE_LETTER_GUIDES: Map<Char, LetterGuide> = mapOf(
    'a' to LetterGuide(
        'a',
        listOf(oval(.45, .55, .20, .17), line(.65f, .39f, .65f, .72f)),
    ),
    'b' to LetterGuide(
        'b',
        listOf(
            line(.32f, .15f, .32f, .72f),
            join(
                cubic(TracingPoint(.32f, .39f), TracingPoint(.72f, .35f), TracingPoint(.76f, .53f), TracingPoint(.70f, .59f)),
                cubic(TracingPoint(.70f, .59f), TracingPoint(.65f, .75f), TracingPoint(.42f, .75f), TracingPoint(.32f, .67f)),
            ),
        ),
    ),
    'c' to LetterGuide('c', listOf(arc(.50, .55, .25, .18, -45.0, -315.0, 34))),
    'd' to LetterGuide(
        'd',
        listOf(oval(.45, .55, .20, .17), line(.65f, .15f, .65f, .72f)),
    ),
    'e' to LetterGuide(
        'e',
        listOf(
            join(
                line(.28f, .55f, .70f, .55f),
                cubic(TracingPoint(.70f, .55f), TracingPoint(.69f, .34f), TracingPoint(.28f, .34f), TracingPoint(.27f, .55f)),
            ) + cubic(
                TracingPoint(.27f, .55f), TracingPoint(.27f, .76f),
                TracingPoint(.60f, .77f), TracingPoint(.70f, .66f),
            ).drop(1),
        ),
    ),
    'f' to LetterGuide(
        'f',
        listOf(
            join(
                cubic(TracingPoint(.67f, .18f), TracingPoint(.48f, .08f), TracingPoint(.38f, .24f), TracingPoint(.40f, .42f)),
                line(.40f, .42f, .40f, .72f),
            ),
            line(.24f, .42f, .64f, .42f),
        ),
    ),
    'g' to LetterGuide(
        'g',
        listOf(
            oval(.45, .55, .20, .17),
            join(
                line(.65f, .39f, .65f, .76f),
                cubic(TracingPoint(.65f, .76f), TracingPoint(.64f, .94f), TracingPoint(.34f, .94f), TracingPoint(.30f, .83f)),
            ),
        ),
    ),
    'h' to LetterGuide(
        'h',
        listOf(
            line(.31f, .15f, .31f, .72f),
            cubic(TracingPoint(.31f, .49f), TracingPoint(.39f, .34f), TracingPoint(.68f, .34f), TracingPoint(.68f, .72f)),
        ),
    ),
    'i' to LetterGuide('i', listOf(line(.50f, .38f, .50f, .72f), line(.50f, .23f, .50f, .25f))),
    'j' to LetterGuide(
        'j',
        listOf(
            join(
                line(.57f, .38f, .57f, .75f),
                cubic(TracingPoint(.57f, .75f), TracingPoint(.57f, .91f), TracingPoint(.34f, .93f), TracingPoint(.28f, .82f)),
            ),
            line(.57f, .23f, .57f, .25f),
        ),
    ),
    'k' to LetterGuide(
        'k',
        listOf(line(.31f, .15f, .31f, .72f), line(.68f, .38f, .31f, .57f, .70f, .72f)),
    ),
    'l' to LetterGuide('l', listOf(line(.50f, .15f, .50f, .72f))),
    'm' to LetterGuide(
        'm',
        listOf(
            line(.20f, .38f, .20f, .72f),
            cubic(TracingPoint(.20f, .49f), TracingPoint(.28f, .34f), TracingPoint(.48f, .34f), TracingPoint(.48f, .72f)),
            cubic(TracingPoint(.48f, .49f), TracingPoint(.56f, .34f), TracingPoint(.77f, .34f), TracingPoint(.77f, .72f)),
        ),
    ),
    'n' to LetterGuide(
        'n',
        listOf(
            line(.28f, .38f, .28f, .72f),
            cubic(TracingPoint(.28f, .49f), TracingPoint(.38f, .34f), TracingPoint(.69f, .34f), TracingPoint(.69f, .72f)),
        ),
    ),
    'o' to LetterGuide('o', listOf(oval(.50, .55, .25, .18))),
    'p' to LetterGuide(
        'p',
        listOf(
            line(.31f, .38f, .31f, .90f),
            join(
                cubic(TracingPoint(.31f, .39f), TracingPoint(.70f, .34f), TracingPoint(.76f, .51f), TracingPoint(.70f, .57f)),
                cubic(TracingPoint(.70f, .57f), TracingPoint(.64f, .73f), TracingPoint(.40f, .73f), TracingPoint(.31f, .66f)),
            ),
        ),
    ),
    'q' to LetterGuide(
        'q',
        listOf(oval(.45, .55, .20, .17), line(.65f, .39f, .65f, .90f)),
    ),
    'r' to LetterGuide(
        'r',
        listOf(
            line(.34f, .38f, .34f, .72f),
            cubic(TracingPoint(.34f, .49f), TracingPoint(.43f, .35f), TracingPoint(.62f, .35f), TracingPoint(.69f, .43f)),
        ),
    ),
    's' to LetterGuide(
        's',
        listOf(
            join(
                cubic(TracingPoint(.68f, .42f), TracingPoint(.55f, .32f), TracingPoint(.31f, .36f), TracingPoint(.31f, .50f)),
                cubic(TracingPoint(.31f, .50f), TracingPoint(.31f, .61f), TracingPoint(.69f, .57f), TracingPoint(.68f, .68f)),
            ) + cubic(
                TracingPoint(.68f, .68f), TracingPoint(.62f, .77f),
                TracingPoint(.37f, .78f), TracingPoint(.27f, .68f),
            ).drop(1),
        ),
    ),
    't' to LetterGuide('t', listOf(line(.50f, .22f, .50f, .72f), line(.31f, .43f, .68f, .43f))),
    'u' to LetterGuide(
        'u',
        listOf(
            join(
                line(.28f, .38f, .28f, .57f),
                cubic(TracingPoint(.28f, .57f), TracingPoint(.28f, .77f), TracingPoint(.67f, .77f), TracingPoint(.67f, .57f)),
            ) + line(.67f, .57f, .67f, .38f).drop(1),
        ),
    ),
    'v' to LetterGuide('v', listOf(line(.25f, .38f, .50f, .72f, .75f, .38f))),
    'w' to LetterGuide('w', listOf(line(.16f, .38f, .31f, .72f, .50f, .48f, .69f, .72f, .84f, .38f))),
    'x' to LetterGuide('x', listOf(line(.28f, .38f, .72f, .72f), line(.72f, .38f, .28f, .72f))),
    'y' to LetterGuide(
        'y',
        listOf(
            line(.25f, .38f, .48f, .72f),
            join(
                line(.75f, .38f, .48f, .72f),
                cubic(TracingPoint(.48f, .72f), TracingPoint(.40f, .86f), TracingPoint(.32f, .91f), TracingPoint(.24f, .86f)),
            ),
        ),
    ),
    'z' to LetterGuide('z', listOf(line(.27f, .38f, .73f, .38f, .27f, .72f, .73f, .72f))),
)

val LETTER_GUIDES: Map<Char, LetterGuide> = UPPERCASE_LETTER_GUIDES + LOWERCASE_LETTER_GUIDES

object LetterTracingEngine {
    fun resolveName(displayName: String?): TracingName? {
        val original = displayName?.trim().orEmpty()
        if (original.isEmpty() || original.any { !it.isLetter() }) return null
        val letters = original.uppercase(Locale.US).toList()
        if (letters.isEmpty() || letters.any { it !in LETTER_GUIDES }) return null
        return TracingName(displayName = original, letters = letters)
    }

    fun newSession(mode: TracingMode, letters: List<Char>): LetterTracingState {
        require(letters.isNotEmpty() && letters.all { it in LETTER_GUIDES })
        require(mode != TracingMode.ALPHABET || letters == ALPHABET_SEQUENCE)
        require(mode != TracingMode.LOWERCASE || letters == LOWERCASE_SEQUENCE)
        return LetterTracingState(mode = mode, letters = letters.toList())
    }

    fun newNameSession(letters: List<Char>): LetterTracingState = newSession(TracingMode.NAME, letters)

    fun newAlphabetSession(): LetterTracingState = newSession(TracingMode.ALPHABET, ALPHABET_SEQUENCE)

    fun newLowercaseSession(): LetterTracingState = newSession(TracingMode.LOWERCASE, LOWERCASE_SEQUENCE)

    fun addStroke(state: LetterTracingState, points: List<TracingPoint>): LetterTracingState {
        if (!state.isStructurallyValid() || points.isEmpty() || points.any { !it.isValid }) return state
        val updated = state.strokesByPosition.toMutableList()
        updated[state.letterPosition] = updated[state.letterPosition] + TracingStroke(points.toList())
        val updatedState = state.copy(strokesByPosition = updated)
        val newlyComplete = updatedState.isComplete && !state.isComplete && !state.completionCelebrated
        return updatedState.copy(celebrationPending = state.celebrationPending || newlyComplete)
    }

    fun undo(state: LetterTracingState): LetterTracingState {
        if (!state.isStructurallyValid() || state.currentStrokes.isEmpty()) return state
        val updated = state.strokesByPosition.toMutableList()
        updated[state.letterPosition] = state.currentStrokes.dropLast(1)
        return state.copy(strokesByPosition = updated, celebrationPending = false)
    }

    fun clear(state: LetterTracingState): LetterTracingState {
        if (!state.isStructurallyValid() || state.currentStrokes.isEmpty()) return state
        val updated = state.strokesByPosition.toMutableList()
        updated[state.letterPosition] = emptyList()
        return state.copy(strokesByPosition = updated, celebrationPending = false)
    }

    fun previous(state: LetterTracingState): LetterTracingState =
        if (state.letterPosition > 0) state.copy(letterPosition = state.letterPosition - 1) else state

    fun next(state: LetterTracingState): LetterTracingState =
        if (state.letterPosition < state.letters.lastIndex) {
            state.copy(letterPosition = state.letterPosition + 1)
        } else {
            state
        }

    fun consumeCelebration(state: LetterTracingState): LetterTracingState =
        if (state.celebrationPending) {
            state.copy(completionCelebrated = true, celebrationPending = false)
        } else {
            state
        }

    fun practiceAgain(state: LetterTracingState): LetterTracingState = newSession(state.mode, state.letters)
}

internal fun LetterTracingState.isStructurallyValid(): Boolean =
    letters.isNotEmpty() && letters.all { it in LETTER_GUIDES } &&
        (mode != TracingMode.ALPHABET || letters == ALPHABET_SEQUENCE) &&
        (mode != TracingMode.LOWERCASE || letters == LOWERCASE_SEQUENCE) &&
        letterPosition in letters.indices &&
        strokesByPosition.size == letters.size &&
        (!celebrationPending || (isComplete && !completionCelebrated)) &&
        strokesByPosition.flatten().all { stroke ->
            stroke.points.isNotEmpty() && stroke.points.all(TracingPoint::isValid)
        }
