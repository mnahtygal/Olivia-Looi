package com.nahtygal.olivialooi.ui.games.abc

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
import com.nahtygal.olivialooi.games.abc.AbcMode
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.games.WinterNavigationButton
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.SnowWhite

@Composable
fun AbcAdventureScreen(
    onModeSelected: (AbcMode) -> Unit,
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
                    text = stringResource(R.string.abc_name),
                    color = SnowWhite,
                    fontSize = if (tabletLayout) 40.sp else 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.abc_choose_adventure),
                    color = SnowWhite.copy(alpha = 0.92f),
                    fontSize = if (tabletLayout) 20.sp else 17.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(if (tabletLayout) 22.dp else 14.dp))

                if (tabletLayout) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 920.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        AbcMode.entries.forEach { mode ->
                            AdventureCard(
                                mode = mode,
                                tabletLayout = true,
                                onClick = { onModeSelected(mode) },
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
                        AbcMode.entries.forEach { mode ->
                            AdventureCard(
                                mode = mode,
                                tabletLayout = false,
                                onClick = { onModeSelected(mode) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (tabletLayout) 22.dp else 14.dp))
                WinterNavigationButton(
                    labelResource = R.string.back_to_games,
                    onClick = onGamesClick,
                )
            }
        }
    }
}

@Composable
private fun AdventureCard(
    mode: AbcMode,
    tabletLayout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(mode.titleResource())
    val subtitle = stringResource(mode.subtitleResource())
    val description = stringResource(R.string.abc_mode_description, title, subtitle)
    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = if (tabletLayout) 270.dp else 142.dp)
            .clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(if (tabletLayout) 30.dp else 24.dp),
        colors = CardDefaults.cardColors(containerColor = mode.cardColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 11.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (tabletLayout) 20.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = mode.glyph(),
                color = Color(0xFF6554C0),
                fontSize = if (tabletLayout) 48.sp else 34.sp,
                lineHeight = if (tabletLayout) 54.sp else 39.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = DeepIndigo,
                fontSize = if (tabletLayout) 25.sp else 21.sp,
                lineHeight = if (tabletLayout) 29.sp else 25.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                color = DeepIndigo.copy(alpha = 0.78f),
                fontSize = if (tabletLayout) 17.sp else 16.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@StringRes
private fun AbcMode.titleResource(): Int = when (this) {
    AbcMode.LEARN -> R.string.abc_mode_learn
    AbcMode.FIND_LETTER -> R.string.abc_mode_find
    AbcMode.STARTS_WITH -> R.string.abc_mode_starts
}

@StringRes
private fun AbcMode.subtitleResource(): Int = when (this) {
    AbcMode.LEARN -> R.string.abc_mode_learn_subtitle
    AbcMode.FIND_LETTER -> R.string.abc_mode_find_subtitle
    AbcMode.STARTS_WITH -> R.string.abc_mode_starts_subtitle
}

private fun AbcMode.glyph(): String = when (this) {
    AbcMode.LEARN -> "A B C"
    AbcMode.FIND_LETTER -> "A  B  ?"
    AbcMode.STARTS_WITH -> "C  🐱"
}

private fun AbcMode.cardColor(): Color = when (this) {
    AbcMode.LEARN -> Color(0xFFF2F7FF)
    AbcMode.FIND_LETTER -> Color(0xFFFFEFF8)
    AbcMode.STARTS_WITH -> Color(0xFFFFF3CC)
}
