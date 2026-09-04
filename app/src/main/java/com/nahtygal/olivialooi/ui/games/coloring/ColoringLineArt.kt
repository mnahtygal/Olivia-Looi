package com.nahtygal.olivialooi.ui.games.coloring

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.nahtygal.olivialooi.games.coloring.ColoringPicture
import com.nahtygal.olivialooi.ui.theme.SnowWhite

@Composable
internal fun ColoringPagePreview(
    picture: ColoringPicture,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.background(SnowWhite)) {
        drawColoringLineArt(picture)
    }
}

internal fun DrawScope.drawColoringLineArt(picture: ColoringPicture) {
    when (picture) {
        ColoringPicture.Butterfly -> drawButterfly()
        ColoringPicture.Flower -> drawFlower()
        ColoringPicture.Puppy -> drawPuppy()
        ColoringPicture.Snowman -> drawSnowman()
    }
}

private val LineArtColor = Color(0xFF29263A)

private fun DrawScope.outlineStyle(multiplier: Float = 1f) = Stroke(
    width = size.minDimension * 0.022f * multiplier,
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
)

private fun DrawScope.point(x: Float, y: Float) = Offset(size.width * x, size.height * y)

private fun DrawScope.drawButterfly() {
    val leftWing = Path().apply {
        moveTo(size.width * 0.48f, size.height * 0.40f)
        cubicTo(
            size.width * 0.25f,
            size.height * 0.08f,
            size.width * 0.05f,
            size.height * 0.22f,
            size.width * 0.18f,
            size.height * 0.49f,
        )
        cubicTo(
            size.width * 0.03f,
            size.height * 0.75f,
            size.width * 0.30f,
            size.height * 0.91f,
            size.width * 0.48f,
            size.height * 0.61f,
        )
        close()
    }
    val rightWing = Path().apply {
        moveTo(size.width * 0.52f, size.height * 0.40f)
        cubicTo(
            size.width * 0.75f,
            size.height * 0.08f,
            size.width * 0.95f,
            size.height * 0.22f,
            size.width * 0.82f,
            size.height * 0.49f,
        )
        cubicTo(
            size.width * 0.97f,
            size.height * 0.75f,
            size.width * 0.70f,
            size.height * 0.91f,
            size.width * 0.52f,
            size.height * 0.61f,
        )
        close()
    }
    drawPath(leftWing, LineArtColor, style = outlineStyle())
    drawPath(rightWing, LineArtColor, style = outlineStyle())
    drawOval(
        color = LineArtColor,
        topLeft = point(0.45f, 0.27f),
        size = Size(size.width * 0.10f, size.height * 0.48f),
        style = outlineStyle(),
    )
    drawLine(LineArtColor, point(0.48f, 0.28f), point(0.39f, 0.14f), outlineStyle().width)
    drawLine(LineArtColor, point(0.52f, 0.28f), point(0.61f, 0.14f), outlineStyle().width)
    drawCircle(
        LineArtColor,
        size.minDimension * 0.055f,
        point(0.30f, 0.43f),
        style = outlineStyle(0.8f),
    )
    drawCircle(
        LineArtColor,
        size.minDimension * 0.055f,
        point(0.70f, 0.43f),
        style = outlineStyle(0.8f),
    )
}

private fun DrawScope.drawFlower() {
    val center = point(0.5f, 0.38f)
    val petalRadius = size.minDimension * 0.13f
    listOf(
        point(0.5f, 0.18f),
        point(0.69f, 0.30f),
        point(0.62f, 0.52f),
        point(0.38f, 0.52f),
        point(0.31f, 0.30f),
    ).forEach { petalCenter ->
        drawCircle(LineArtColor, petalRadius, petalCenter, style = outlineStyle())
    }
    drawCircle(LineArtColor, size.minDimension * 0.12f, center, style = outlineStyle())
    drawLine(LineArtColor, point(0.5f, 0.50f), point(0.5f, 0.90f), outlineStyle().width)
    val leftLeaf = Path().apply {
        moveTo(size.width * 0.49f, size.height * 0.72f)
        cubicTo(
            size.width * 0.34f,
            size.height * 0.58f,
            size.width * 0.24f,
            size.height * 0.72f,
            size.width * 0.49f,
            size.height * 0.80f,
        )
    }
    val rightLeaf = Path().apply {
        moveTo(size.width * 0.51f, size.height * 0.79f)
        cubicTo(
            size.width * 0.66f,
            size.height * 0.65f,
            size.width * 0.78f,
            size.height * 0.78f,
            size.width * 0.51f,
            size.height * 0.86f,
        )
    }
    drawPath(leftLeaf, LineArtColor, style = outlineStyle())
    drawPath(rightLeaf, LineArtColor, style = outlineStyle())
}

private fun DrawScope.drawPuppy() {
    drawOval(
        color = LineArtColor,
        topLeft = point(0.23f, 0.20f),
        size = Size(size.width * 0.54f, size.height * 0.58f),
        style = outlineStyle(),
    )
    val leftEar = Path().apply {
        moveTo(size.width * 0.30f, size.height * 0.29f)
        cubicTo(
            size.width * 0.08f,
            size.height * 0.20f,
            size.width * 0.08f,
            size.height * 0.60f,
            size.width * 0.29f,
            size.height * 0.58f,
        )
        close()
    }
    val rightEar = Path().apply {
        moveTo(size.width * 0.70f, size.height * 0.29f)
        cubicTo(
            size.width * 0.92f,
            size.height * 0.20f,
            size.width * 0.92f,
            size.height * 0.60f,
            size.width * 0.71f,
            size.height * 0.58f,
        )
        close()
    }
    drawPath(leftEar, LineArtColor, style = outlineStyle())
    drawPath(rightEar, LineArtColor, style = outlineStyle())
    drawCircle(LineArtColor, size.minDimension * 0.026f, point(0.40f, 0.43f))
    drawCircle(LineArtColor, size.minDimension * 0.026f, point(0.60f, 0.43f))
    drawOval(
        color = LineArtColor,
        topLeft = point(0.43f, 0.53f),
        size = Size(size.width * 0.14f, size.height * 0.10f),
    )
    drawArc(
        color = LineArtColor,
        startAngle = 10f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = point(0.37f, 0.57f),
        size = Size(size.width * 0.13f, size.height * 0.13f),
        style = outlineStyle(0.75f),
    )
    drawArc(
        color = LineArtColor,
        startAngle = 100f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = point(0.50f, 0.57f),
        size = Size(size.width * 0.13f, size.height * 0.13f),
        style = outlineStyle(0.75f),
    )
    drawArc(
        color = LineArtColor,
        startAngle = 15f,
        sweepAngle = 150f,
        useCenter = false,
        topLeft = point(0.36f, 0.63f),
        size = Size(size.width * 0.28f, size.height * 0.25f),
        style = outlineStyle(),
    )
}

private fun DrawScope.drawSnowman() {
    drawCircle(
        LineArtColor,
        size.minDimension * 0.20f,
        point(0.5f, 0.33f),
        style = outlineStyle(),
    )
    drawCircle(
        LineArtColor,
        size.minDimension * 0.29f,
        point(0.5f, 0.70f),
        style = outlineStyle(),
    )
    drawCircle(LineArtColor, size.minDimension * 0.022f, point(0.43f, 0.29f))
    drawCircle(LineArtColor, size.minDimension * 0.022f, point(0.57f, 0.29f))
    drawLine(LineArtColor, point(0.50f, 0.34f), point(0.66f, 0.38f), outlineStyle(0.75f).width)
    drawArc(
        color = LineArtColor,
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = point(0.40f, 0.34f),
        size = Size(size.width * 0.20f, size.height * 0.13f),
        style = outlineStyle(0.65f),
    )
    repeat(3) { index ->
        drawCircle(
            LineArtColor,
            size.minDimension * 0.021f,
            point(0.5f, 0.60f + index * 0.11f),
        )
    }
    drawLine(LineArtColor, point(0.25f, 0.62f), point(0.08f, 0.49f), outlineStyle().width)
    drawLine(LineArtColor, point(0.75f, 0.62f), point(0.92f, 0.49f), outlineStyle().width)
    drawRect(
        color = LineArtColor,
        topLeft = point(0.34f, 0.08f),
        size = Size(size.width * 0.32f, size.height * 0.13f),
        style = outlineStyle(),
    )
    drawLine(LineArtColor, point(0.28f, 0.21f), point(0.72f, 0.21f), outlineStyle().width)
}
