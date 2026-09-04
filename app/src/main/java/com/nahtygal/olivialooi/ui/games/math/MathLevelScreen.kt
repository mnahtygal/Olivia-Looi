package com.nahtygal.olivialooi.ui.games.math

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.games.math.MathLevel
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.games.WinterNavigationButton
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.SnowWhite

@Composable
fun MathLevelScreen(
    onLevelSelected: (MathLevel) -> Unit,
    onGamesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WinterGamesBackground(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabletLayout = maxWidth >= 600.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.math_name),
                    color = SnowWhite,
                    fontSize = if (tabletLayout) 40.sp else 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.math_pick_level),
                    color = SnowWhite.copy(alpha = 0.92f),
                    fontSize = if (tabletLayout) 20.sp else 17.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(if (tabletLayout) 20.dp else 14.dp))

                if (tabletLayout) {
                    Column(
                        modifier = Modifier.widthIn(max = 820.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        MathLevel.entries.chunked(2).forEach { levels ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                            ) {
                                levels.forEach { level ->
                                    MathLevelCard(
                                        level = level,
                                        tabletLayout = true,
                                        onClick = { onLevelSelected(level) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 440.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MathLevel.entries.forEach { level ->
                            MathLevelCard(
                                level = level,
                                tabletLayout = false,
                                onClick = { onLevelSelected(level) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (tabletLayout) 20.dp else 14.dp))
                WinterNavigationButton(
                    labelResource = R.string.back_to_games,
                    onClick = onGamesClick,
                )
            }
        }
    }
}

@Composable
private fun MathLevelCard(
    level: MathLevel,
    tabletLayout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(level.titleResource())
    val subtitle = stringResource(level.subtitleResource())
    val description = stringResource(R.string.math_level_description, title, subtitle)
    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = if (tabletLayout) 190.dp else 112.dp)
            .clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(if (tabletLayout) 30.dp else 24.dp),
        colors = CardDefaults.cardColors(containerColor = level.cardColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (tabletLayout) 20.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = level.glyph(),
                color = Color(0xFF7568D7),
                fontSize = if (tabletLayout) 34.sp else 27.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = title,
                color = DeepIndigo,
                fontSize = if (tabletLayout) 26.sp else 21.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                color = DeepIndigo.copy(alpha = 0.78f),
                fontSize = if (tabletLayout) 18.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@StringRes
private fun MathLevel.titleResource(): Int = when (this) {
    MathLevel.SINGLE_DIGIT -> R.string.math_level_little
    MathLevel.DOUBLE_DIGIT -> R.string.math_level_big
    MathLevel.TRIPLE_DIGIT -> R.string.math_level_super
    MathLevel.MIXED -> R.string.math_level_challenge
}

@StringRes
private fun MathLevel.subtitleResource(): Int = when (this) {
    MathLevel.SINGLE_DIGIT -> R.string.math_level_single_subtitle
    MathLevel.DOUBLE_DIGIT -> R.string.math_level_double_subtitle
    MathLevel.TRIPLE_DIGIT -> R.string.math_level_triple_subtitle
    MathLevel.MIXED -> R.string.math_level_mixed_subtitle
}

private fun MathLevel.glyph(): String = when (this) {
    MathLevel.SINGLE_DIGIT -> "2 + 3"
    MathLevel.DOUBLE_DIGIT -> "47 − 28"
    MathLevel.TRIPLE_DIGIT -> "247 + 135"
    MathLevel.MIXED -> "+  −  ?"
}

private fun MathLevel.cardColor(): Color = when (this) {
    MathLevel.SINGLE_DIGIT -> Color(0xFFF2F7FF)
    MathLevel.DOUBLE_DIGIT -> Color(0xFFFFEFF8)
    MathLevel.TRIPLE_DIGIT -> Color(0xFFF2EEFF)
    MathLevel.MIXED -> Color(0xFFFFF3CC)
}
