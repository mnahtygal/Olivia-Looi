package com.nahtygal.olivialooi.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.SnowWhite

@Composable
fun GamesScreen(
    onTicTacToeClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WinterGamesBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.games_heading),
                color = SnowWhite,
                fontSize = 38.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.games_subtitle),
                color = SnowWhite.copy(alpha = 0.9f),
                fontSize = 18.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(22.dp))

            Card(
                onClick = onTicTacToeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(maxWidth = 440.dp),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = SnowWhite.copy(alpha = 0.94f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TicTacToeGameGlyph(
                        modifier = Modifier
                            .sizeIn(maxWidth = 190.dp, maxHeight = 190.dp)
                            .fillMaxWidth(0.56f)
                            .aspectRatio(1f),
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.tic_tac_toe_name),
                        color = DeepIndigo,
                        fontSize = 30.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.tic_tac_toe_card_subtitle),
                        color = DeepIndigo.copy(alpha = 0.76f),
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
            WinterNavigationButton(
                labelResource = R.string.back_to_home,
                onClick = onHomeClick,
            )
        }
    }
}

@Composable
private fun TicTacToeGameGlyph(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridStroke = 5.dp.toPx()
            for (third in 1..2) {
                val offset = size.width * third / 3f
                drawLine(DeepIndigo, Offset(offset, 0f), Offset(offset, size.height), gridStroke)
                drawLine(DeepIndigo, Offset(0f, offset), Offset(size.width, offset), gridStroke)
            }
            val markStroke = 7.dp.toPx()
            drawLine(
                Color(0xFF6554C0),
                Offset(size.width * 0.08f, size.height * 0.08f),
                Offset(size.width * 0.25f, size.height * 0.25f),
                markStroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                Color(0xFF6554C0),
                Offset(size.width * 0.25f, size.height * 0.08f),
                Offset(size.width * 0.08f, size.height * 0.25f),
                markStroke,
                cap = StrokeCap.Round,
            )
            drawCircle(
                Color(0xFFE060A6),
                radius = size.width * 0.10f,
                center = Offset(size.width * 0.83f, size.height * 0.5f),
                style = Stroke(markStroke),
            )
            drawLine(
                Color(0xFF6554C0),
                Offset(size.width * 0.41f, size.height * 0.75f),
                Offset(size.width * 0.58f, size.height * 0.92f),
                markStroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                Color(0xFF6554C0),
                Offset(size.width * 0.58f, size.height * 0.75f),
                Offset(size.width * 0.41f, size.height * 0.92f),
                markStroke,
                cap = StrokeCap.Round,
            )
        }
    }
}
