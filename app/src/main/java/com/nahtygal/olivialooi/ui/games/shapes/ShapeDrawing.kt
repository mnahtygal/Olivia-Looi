package com.nahtygal.olivialooi.ui.games.shapes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.nahtygal.olivialooi.games.shapes.ColorId
import com.nahtygal.olivialooi.games.shapes.ShapeChoice
import com.nahtygal.olivialooi.games.shapes.ShapeId
import kotlin.math.cos
import kotlin.math.sin

// Instructional colors stay constant through selection and feedback.
private fun ColorId.paint(): Color = when (this) {
    ColorId.RED -> Color(0xFFE52232)
    ColorId.BLUE -> Color(0xFF1769E8)
    ColorId.GREEN -> Color(0xFF159447)
    ColorId.YELLOW -> Color(0xFFFFD928)
    ColorId.PURPLE -> Color(0xFF823CC8)
    ColorId.PINK -> Color(0xFFF36BAA)
}

/** Original normalized geometry, cached until size or choice changes. */
@Composable
fun ShapeDrawing(choice: ShapeChoice, modifier: Modifier = Modifier) {
    Canvas(modifier.drawWithCache {
        val side = size.minDimension * 0.86f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val path = Path()
        fun move(x: Float, y: Float) = path.moveTo(left + x * side, top + y * side)
        fun line(x: Float, y: Float) = path.lineTo(left + x * side, top + y * side)
        fun curve(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) =
            path.cubicTo(left + x1 * side, top + y1 * side, left + x2 * side, top + y2 * side, left + x3 * side, top + y3 * side)
        when (choice.shape) {
            ShapeId.CIRCLE -> path.addOval(androidx.compose.ui.geometry.Rect(left, top, left + side, top + side))
            ShapeId.SQUARE -> { move(0f, 0f); line(1f, 0f); line(1f, 1f); line(0f, 1f) }
            ShapeId.RECTANGLE -> { move(0f, .22f); line(1f, .22f); line(1f, .78f); line(0f, .78f) }
            ShapeId.TRIANGLE -> { move(.5f, 0f); line(1f, 1f); line(0f, 1f) }
            ShapeId.STAR -> repeat(10) { index ->
                val angle = -Math.PI / 2 + index * Math.PI / 5
                val radius = if (index % 2 == 0) .5 else .22
                val x = (.5 + cos(angle) * radius).toFloat()
                val y = (.5 + sin(angle) * radius).toFloat()
                if (index == 0) move(x, y) else line(x, y)
            }
            ShapeId.HEART -> {
                move(.5f, .95f)
                curve(.40f, .82f, 0f, .55f, 0f, .28f)
                curve(0f, -.02f, .36f, -.06f, .5f, .22f)
                curve(.64f, -.06f, 1f, -.02f, 1f, .28f)
                curve(1f, .55f, .60f, .82f, .5f, .95f)
            }
        }
        path.close()
        onDrawBehind {
            drawPath(path, choice.color.paint())
            // A neutral edge keeps yellow recognizable on the light card background.
            drawPath(path, Color(0xFF29304E), style = Stroke(2.dp.toPx()))
        }
    }) { }
}

@Composable
fun ShapesGameGlyph(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ShapeDrawing(ShapeChoice(ShapeId.CIRCLE, ColorId.RED), Modifier.weight(1f).fillMaxHeight())
        ShapeDrawing(ShapeChoice(ShapeId.TRIANGLE, ColorId.BLUE), Modifier.weight(1f).fillMaxHeight())
        ShapeDrawing(ShapeChoice(ShapeId.SQUARE, ColorId.YELLOW), Modifier.weight(1f).fillMaxHeight())
    }
}
