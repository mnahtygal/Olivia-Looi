package com.nahtygal.olivialooi.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.ui.theme.AuroraPurple
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.FrostBlue
import com.nahtygal.olivialooi.ui.theme.IceBlue
import com.nahtygal.olivialooi.ui.theme.Lavender
import com.nahtygal.olivialooi.ui.theme.OliviaLooiTheme
import com.nahtygal.olivialooi.ui.theme.ReadyMint
import com.nahtygal.olivialooi.ui.theme.SkyBlue
import com.nahtygal.olivialooi.ui.theme.SparklePurple
import com.nahtygal.olivialooi.ui.theme.SnowWhite

@Composable
fun LooLooHomeScreen(
    onTalkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepIndigo, AuroraPurple, SkyBlue, DeepIndigo),
                ),
            ),
    ) {
        WinterBackdrop(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ReadyStatus(modifier = Modifier.align(Alignment.End))

            Text(
                text = stringResource(R.string.looloo_name),
                color = SnowWhite,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                lineHeight = 52.sp,
            )
            Text(
                text = stringResource(
                    R.string.looloo_greeting,
                    stringResource(R.string.child_name_olivia),
                ),
                color = IceBlue,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                LooLooAvatar(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.88f),
                )
            }

            TalkToLooLooButton(
                onClick = onTalkClick,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.looloo_tap_prompt),
                modifier = Modifier
                    .background(DeepIndigo, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                color = SnowWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ReadyStatus(modifier: Modifier = Modifier) {
    val readyDescription = stringResource(R.string.looloo_ready_content_description)

    Row(
        modifier = modifier
            .background(
                color = SnowWhite.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clearAndSetSemantics { contentDescription = readyDescription },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(ReadyMint, CircleShape),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.looloo_ready),
            color = SnowWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TalkToLooLooButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 88.dp),
        shape = RoundedCornerShape(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SnowWhite,
            contentColor = DeepIndigo,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 10.dp,
            pressedElevation = 4.dp,
        ),
    ) {
        SparkleIcon(modifier = Modifier.size(30.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.looloo_talk_action),
            textAlign = TextAlign.Center,
            fontSize = 23.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun SparkleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val color = SparklePurple
        drawLine(color, Offset(center.x, 1f), Offset(center.x, size.height - 1f), 4f, StrokeCap.Round)
        drawLine(color, Offset(1f, center.y), Offset(size.width - 1f, center.y), 4f, StrokeCap.Round)
        drawLine(color, Offset(5f, 5f), Offset(size.width - 5f, size.height - 5f), 3f, StrokeCap.Round)
        drawLine(color, Offset(size.width - 5f, 5f), Offset(5f, size.height - 5f), 3f, StrokeCap.Round)
    }
}

@Composable
private fun WinterBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(IceBlue.copy(alpha = 0.25f), Color.Transparent),
                center = Offset(size.width * 0.15f, size.height * 0.28f),
                radius = size.width * 0.65f,
            ),
            radius = size.width * 0.65f,
            center = Offset(size.width * 0.15f, size.height * 0.28f),
        )

        val snowflakes = listOf(
            Offset(0.08f, 0.15f) to 5f,
            Offset(0.88f, 0.20f) to 7f,
            Offset(0.16f, 0.43f) to 4f,
            Offset(0.92f, 0.48f) to 5f,
            Offset(0.08f, 0.68f) to 7f,
            Offset(0.84f, 0.72f) to 4f,
            Offset(0.20f, 0.88f) to 3f,
            Offset(0.72f, 0.08f) to 3f,
        )
        snowflakes.forEach { (position, radius) ->
            drawCircle(
                color = SnowWhite.copy(alpha = 0.52f),
                radius = radius,
                center = Offset(size.width * position.x, size.height * position.y),
            )
        }
    }
}

/** An original, friendly snow-sprite made entirely from Compose drawing primitives. */
@Composable
private fun LooLooAvatar(modifier: Modifier = Modifier) {
    val avatarDescription = stringResource(R.string.looloo_avatar_content_description)

    Canvas(
        modifier = modifier.semantics {
            contentDescription = avatarDescription
        },
    ) {
        val scale = minOf(size.width, size.height) / 300f
        val center = Offset(size.width / 2f, size.height / 2f)
        fun point(x: Float, y: Float) = Offset(
            x = center.x + x * scale,
            y = center.y + y * scale,
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(IceBlue.copy(alpha = 0.42f), Color.Transparent),
                center = center,
                radius = 148f * scale,
            ),
            center = center,
            radius = 148f * scale,
        )

        listOf(point(-112f, -74f), point(112f, -42f), point(-104f, 72f)).forEachIndexed { index, crystal ->
            val arm = (11f - index * 2f) * scale
            repeat(3) { spoke ->
                rotate(degrees = spoke * 60f, pivot = crystal) {
                    drawLine(
                        color = SnowWhite.copy(alpha = 0.82f),
                        start = Offset(crystal.x - arm, crystal.y),
                        end = Offset(crystal.x + arm, crystal.y),
                        strokeWidth = 2.5f * scale,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        drawOval(
            color = DeepIndigo.copy(alpha = 0.22f),
            topLeft = point(-79f, 91f),
            size = Size(158f * scale, 24f * scale),
        )
        drawOval(
            brush = Brush.linearGradient(
                colors = listOf(SnowWhite, Color(0xFFDDEEFF)),
                start = point(-70f, -115f),
                end = point(75f, 105f),
            ),
            topLeft = point(-88f, -106f),
            size = Size(176f * scale, 210f * scale),
        )

        drawCircle(SnowWhite, 39f * scale, point(-75f, -47f))
        drawCircle(SnowWhite, 39f * scale, point(75f, -47f))
        drawArc(
            color = Lavender,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = point(-71f, -111f),
            size = Size(142f * scale, 112f * scale),
            style = Stroke(width = 13f * scale, cap = StrokeCap.Round),
        )
        drawCircle(Lavender, 23f * scale, point(-82f, -38f))
        drawCircle(Lavender, 23f * scale, point(82f, -38f))
        drawCircle(FrostBlue, 12f * scale, point(-82f, -38f))
        drawCircle(FrostBlue, 12f * scale, point(82f, -38f))

        drawCircle(DeepIndigo, 10f * scale, point(-32f, -26f))
        drawCircle(DeepIndigo, 10f * scale, point(32f, -26f))
        drawCircle(SnowWhite, 3.5f * scale, point(-29f, -30f))
        drawCircle(SnowWhite, 3.5f * scale, point(35f, -30f))
        drawCircle(Color(0xFFFFA9C5).copy(alpha = 0.65f), 13f * scale, point(-53f, 4f))
        drawCircle(Color(0xFFFFA9C5).copy(alpha = 0.65f), 13f * scale, point(53f, 4f))
        drawArc(
            color = DeepIndigo,
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = point(-24f, -2f),
            size = Size(48f * scale, 39f * scale),
            style = Stroke(width = 6f * scale, cap = StrokeCap.Round),
        )

        val heart = Path().apply {
            moveTo(point(0f, 67f).x, point(0f, 67f).y)
            cubicTo(
                point(-38f, 45f).x, point(-38f, 45f).y,
                point(-27f, 20f).x, point(-27f, 20f).y,
                point(0f, 40f).x, point(0f, 40f).y,
            )
            cubicTo(
                point(27f, 20f).x, point(27f, 20f).y,
                point(38f, 45f).x, point(38f, 45f).y,
                point(0f, 67f).x, point(0f, 67f).y,
            )
            close()
        }
        drawPath(
            path = heart,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF9DF2F1), Color(0xFF8174DF)),
                start = point(-20f, 30f),
                end = point(20f, 65f),
            ),
        )
    }
}

@Preview(
    name = "LooLoo home",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    fontScale = 1.3f,
)
@Composable
private fun LooLooHomeScreenPreview() {
    OliviaLooiTheme {
        LooLooHomeScreen(onTalkClick = {})
    }
}
