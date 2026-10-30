package com.nahtygal.olivialooi.games.tracing

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * A forgiving participation check, not handwriting recognition or accuracy grading.
 * Each vector-guide path is treated as one required preschool pen component.
 */
object LetterCompletionPolicy {
    private const val PROXIMITY_TOLERANCE = .085f
    private const val MINIMUM_DIRECTION_COSINE = .45f
    private const val MINIMUM_COMPONENT_COVERAGE = .16f
    private const val SAMPLE_SPACING = .025f

    fun requiredComponentCount(letter: Char): Int =
        checkNotNull(LETTER_GUIDES[letter.uppercaseChar()]).paths.size

    fun participatingComponents(letter: Char, strokes: List<TracingStroke>): Set<Int> {
        val guide = checkNotNull(LETTER_GUIDES[letter.uppercaseChar()])
        val userSegments = strokes.flatMap { stroke -> stroke.points.toSegments() }
        if (userSegments.isEmpty()) return emptySet()
        return guide.paths.mapIndexedNotNull { index, component ->
            if (componentParticipated(component, userSegments)) index else null
        }.toSet()
    }

    fun isComplete(letter: Char, strokes: List<TracingStroke>): Boolean =
        participatingComponents(letter, strokes).size == requiredComponentCount(letter)

    private fun componentParticipated(
        component: List<TracingPoint>,
        userSegments: List<NormalizedSegment>,
    ): Boolean {
        val guideSegments = component.toSegments()
        val totalLength = guideSegments.sumOf { it.length.toDouble() }.toFloat()
        if (totalLength <= 0f) return false
        var coveredLength = 0f
        guideSegments.forEach { guideSegment ->
            val sampleCount = ceil(guideSegment.length / SAMPLE_SPACING).toInt().coerceAtLeast(1)
            val sampleLength = guideSegment.length / sampleCount
            repeat(sampleCount) { sampleIndex ->
                val fraction = (sampleIndex + .5f) / sampleCount
                val sample = guideSegment.pointAt(fraction)
                if (userSegments.any { userSegment ->
                        guideSegment.directionMatches(userSegment, MINIMUM_DIRECTION_COSINE) &&
                            sample.distanceTo(userSegment) <= PROXIMITY_TOLERANCE
                    }
                ) {
                    coveredLength += sampleLength
                }
            }
        }
        return coveredLength / totalLength >= MINIMUM_COMPONENT_COVERAGE
    }
}

private data class NormalizedSegment(
    val start: TracingPoint,
    val end: TracingPoint,
) {
    val dx: Float = end.x - start.x
    val dy: Float = end.y - start.y
    val length: Float = sqrt(dx * dx + dy * dy)

    fun pointAt(fraction: Float): TracingPoint = TracingPoint(
        x = start.x + dx * fraction,
        y = start.y + dy * fraction,
    )

    fun directionMatches(other: NormalizedSegment, minimumCosine: Float): Boolean {
        if (length <= 0f || other.length <= 0f) return false
        val cosine = abs(dx * other.dx + dy * other.dy) / (length * other.length)
        return cosine >= minimumCosine
    }
}

private fun List<TracingPoint>.toSegments(): List<NormalizedSegment> =
    zipWithNext(::NormalizedSegment).filter { it.length > 0f }

private fun TracingPoint.distanceTo(segment: NormalizedSegment): Float {
    val lengthSquared = segment.dx * segment.dx + segment.dy * segment.dy
    if (lengthSquared <= 0f) return distanceTo(segment.start)
    val projection = (
        (x - segment.start.x) * segment.dx + (y - segment.start.y) * segment.dy
        ) / lengthSquared
    return distanceTo(segment.pointAt(projection.coerceIn(0f, 1f)))
}

private fun TracingPoint.distanceTo(other: TracingPoint): Float {
    val dx = x - other.x
    val dy = y - other.y
    return sqrt(dx * dx + dy * dy)
}
