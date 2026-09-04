package com.nahtygal.olivialooi.ui.games.spelling

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
import com.nahtygal.olivialooi.games.spelling.SpellingLevel
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.games.WinterNavigationButton
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.SnowWhite

@Composable
fun SpeakAndSpellLevelScreen(
    onLevelSelected: (SpellingLevel) -> Unit,
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
                    text = stringResource(R.string.speak_spell_name),
                    color = SnowWhite,
                    fontSize = if (tabletLayout) 40.sp else 33.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.speak_spell_pick_level),
                    color = SnowWhite.copy(alpha = 0.92f),
                    fontSize = if (tabletLayout) 20.sp else 17.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(if (tabletLayout) 24.dp else 14.dp))

                if (tabletLayout) {
                    Row(
                        modifier = Modifier.widthIn(max = 920.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        SpellingLevel.entries.forEach { level ->
                            LevelCard(
                                level = level,
                                tabletLayout = true,
                                onClick = { onLevelSelected(level) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 440.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SpellingLevel.entries.forEach { level ->
                            LevelCard(
                                level = level,
                                tabletLayout = false,
                                onClick = { onLevelSelected(level) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
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
private fun LevelCard(
    level: SpellingLevel,
    tabletLayout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(level.titleResource())
    val wordLength = stringResource(level.wordLengthResource())
    val encouragement = stringResource(level.encouragementResource())
    val description = stringResource(
        R.string.speak_spell_level_description,
        title,
        wordLength,
        encouragement,
    )
    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = if (tabletLayout) 290.dp else 145.dp)
            .clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(if (tabletLayout) 30.dp else 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (level) {
                SpellingLevel.Level1 -> Color(0xFFF2F7FF)
                SpellingLevel.Level2 -> Color(0xFFFFEFF8)
                SpellingLevel.Level3 -> Color(0xFFF2EEFF)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 11.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (tabletLayout) 24.dp else 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = when (level) {
                    SpellingLevel.Level1 -> "A  B  C"
                    SpellingLevel.Level2 -> "D  E  F"
                    SpellingLevel.Level3 -> "★  ABC  ★"
                },
                color = Color(0xFF7568D7),
                fontSize = if (tabletLayout) 34.sp else 25.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = title,
                color = DeepIndigo,
                fontSize = if (tabletLayout) 27.sp else 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = wordLength,
                color = DeepIndigo.copy(alpha = 0.82f),
                fontSize = if (tabletLayout) 18.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = encouragement,
                color = DeepIndigo.copy(alpha = 0.72f),
                fontSize = if (tabletLayout) 16.sp else 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@StringRes
private fun SpellingLevel.titleResource(): Int = when (this) {
    SpellingLevel.Level1 -> R.string.speak_spell_level_1
    SpellingLevel.Level2 -> R.string.speak_spell_level_2
    SpellingLevel.Level3 -> R.string.speak_spell_level_3
}

@StringRes
private fun SpellingLevel.wordLengthResource(): Int = when (this) {
    SpellingLevel.Level1 -> R.string.speak_spell_three_letter_words
    SpellingLevel.Level2 -> R.string.speak_spell_four_letter_words
    SpellingLevel.Level3 -> R.string.speak_spell_mixed_words
}

@StringRes
private fun SpellingLevel.encouragementResource(): Int = when (this) {
    SpellingLevel.Level1 -> R.string.speak_spell_level_1_subtitle
    SpellingLevel.Level2 -> R.string.speak_spell_level_2_subtitle
    SpellingLevel.Level3 -> R.string.speak_spell_level_3_subtitle
}
