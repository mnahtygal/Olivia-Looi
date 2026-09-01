package com.nahtygal.olivialooi.ui.games

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nahtygal.olivialooi.ui.theme.AuroraPurple
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.FrostBlue
import com.nahtygal.olivialooi.ui.theme.IceBlue
import com.nahtygal.olivialooi.ui.theme.SkyBlue
import com.nahtygal.olivialooi.ui.theme.SnowWhite

@Composable
fun WinterGamesBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color(0xFF303185),
                    0.48f to AuroraPurple,
                    1f to SkyBlue,
                ),
            ),
        ),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(IceBlue.copy(alpha = 0.34f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.width * 0.72f,
                ),
                radius = size.width * 0.72f,
                center = Offset(size.width * 0.5f, size.height * 0.5f),
            )
            val flakes = listOf(
                Offset(0.06f, 0.10f) to 9f,
                Offset(0.92f, 0.13f) to 8f,
                Offset(0.12f, 0.45f) to 6f,
                Offset(0.89f, 0.52f) to 7f,
                Offset(0.07f, 0.86f) to 8f,
                Offset(0.94f, 0.88f) to 6f,
            )
            flakes.forEach { (position, radius) ->
                val center = Offset(size.width * position.x, size.height * position.y)
                repeat(3) { spoke ->
                    rotate(spoke * 60f, center) {
                        drawLine(
                            color = SnowWhite.copy(alpha = 0.44f),
                            start = Offset(center.x - radius, center.y),
                            end = Offset(center.x + radius, center.y),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
            listOf(Offset(0.22f, 0.22f), Offset(0.81f, 0.36f), Offset(0.28f, 0.82f)).forEach {
                drawCircle(
                    color = FrostBlue.copy(alpha = 0.72f),
                    radius = 2.5.dp.toPx(),
                    center = Offset(size.width * it.x, size.height * it.y),
                )
            }
        }
        content()
    }
}

@Composable
fun WinterNavigationButton(
    @StringRes labelResource: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SnowWhite.copy(alpha = 0.94f),
            contentColor = DeepIndigo,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 7.dp),
    ) {
        Row {
            HomeGlyph(modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(labelResource),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun HomeGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 2.4.dp.toPx()
        drawLine(
            DeepIndigo,
            Offset(size.width * 0.1f, size.height * 0.46f),
            Offset(size.width * 0.5f, size.height * 0.1f),
            stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            DeepIndigo,
            Offset(size.width * 0.5f, size.height * 0.1f),
            Offset(size.width * 0.9f, size.height * 0.46f),
            stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            DeepIndigo,
            Offset(size.width * 0.23f, size.height * 0.38f),
            Offset(size.width * 0.23f, size.height * 0.9f),
            stroke,
        )
        drawLine(
            DeepIndigo,
            Offset(size.width * 0.77f, size.height * 0.38f),
            Offset(size.width * 0.77f, size.height * 0.9f),
            stroke,
        )
        drawLine(
            DeepIndigo,
            Offset(size.width * 0.23f, size.height * 0.9f),
            Offset(size.width * 0.77f, size.height * 0.9f),
            stroke,
        )
    }
}
