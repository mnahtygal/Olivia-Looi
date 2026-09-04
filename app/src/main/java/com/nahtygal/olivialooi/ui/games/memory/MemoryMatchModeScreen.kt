package com.nahtygal.olivialooi.ui.games.memory

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
import com.nahtygal.olivialooi.games.memory.MemoryGameSize
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.games.WinterNavigationButton
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.SnowWhite

@Composable
fun MemoryMatchModeScreen(
    onSizeSelected: (MemoryGameSize) -> Unit,
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
                    text = stringResource(R.string.memory_match_name),
                    color = SnowWhite,
                    fontSize = if (tabletLayout) 40.sp else 34.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.memory_match_mode_subtitle),
                    color = SnowWhite.copy(alpha = 0.92f),
                    fontSize = if (tabletLayout) 20.sp else 17.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(if (tabletLayout) 24.dp else 14.dp))

                if (tabletLayout) {
                    Row(
                        modifier = Modifier.widthIn(max = 720.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        SizeCard(
                            titleResource = R.string.memory_little_game,
                            pairsResource = R.string.memory_little_pairs,
                            cardsResource = R.string.memory_little_cards,
                            descriptionResource = R.string.memory_little_description,
                            accessibilityResource = R.string.memory_little_accessibility,
                            containerColor = Color(0xFFF2F7FF),
                            tabletLayout = true,
                            onClick = { onSizeSelected(MemoryGameSize.Little) },
                            modifier = Modifier.weight(1f),
                        )
                        SizeCard(
                            titleResource = R.string.memory_big_game,
                            pairsResource = R.string.memory_big_pairs,
                            cardsResource = R.string.memory_big_cards,
                            descriptionResource = R.string.memory_big_description,
                            accessibilityResource = R.string.memory_big_accessibility,
                            containerColor = Color(0xFFFFEFF8),
                            tabletLayout = true,
                            onClick = { onSizeSelected(MemoryGameSize.Big) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 440.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SizeCard(
                            titleResource = R.string.memory_little_game,
                            pairsResource = R.string.memory_little_pairs,
                            cardsResource = R.string.memory_little_cards,
                            descriptionResource = R.string.memory_little_description,
                            accessibilityResource = R.string.memory_little_accessibility,
                            containerColor = Color(0xFFF2F7FF),
                            tabletLayout = false,
                            onClick = { onSizeSelected(MemoryGameSize.Little) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SizeCard(
                            titleResource = R.string.memory_big_game,
                            pairsResource = R.string.memory_big_pairs,
                            cardsResource = R.string.memory_big_cards,
                            descriptionResource = R.string.memory_big_description,
                            accessibilityResource = R.string.memory_big_accessibility,
                            containerColor = Color(0xFFFFEFF8),
                            tabletLayout = false,
                            onClick = { onSizeSelected(MemoryGameSize.Big) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (tabletLayout) 24.dp else 14.dp))
                WinterNavigationButton(
                    labelResource = R.string.back_to_games,
                    onClick = onGamesClick,
                )
            }
        }
    }
}

@Composable
private fun SizeCard(
    @StringRes titleResource: Int,
    @StringRes pairsResource: Int,
    @StringRes cardsResource: Int,
    @StringRes descriptionResource: Int,
    @StringRes accessibilityResource: Int,
    containerColor: Color,
    tabletLayout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accessibilityDescription = stringResource(accessibilityResource)
    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = if (tabletLayout) 300.dp else 166.dp)
            .clearAndSetSemantics {
                contentDescription = accessibilityDescription
            },
        shape = RoundedCornerShape(if (tabletLayout) 30.dp else 24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.97f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (tabletLayout) 28.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (tabletLayout) "❄  ✦  ❄" else "❄  ✦",
                color = Color(0xFF7568D7),
                fontSize = if (tabletLayout) 46.sp else 30.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(titleResource),
                color = DeepIndigo,
                fontSize = if (tabletLayout) 29.sp else 23.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "${stringResource(pairsResource)}  •  ${stringResource(cardsResource)}",
                color = DeepIndigo.copy(alpha = 0.8f),
                fontSize = if (tabletLayout) 19.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = stringResource(descriptionResource),
                color = DeepIndigo.copy(alpha = 0.75f),
                fontSize = if (tabletLayout) 17.sp else 15.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
