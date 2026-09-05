package com.nahtygal.olivialooi.ui.games.shapes

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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.games.shapes.ShapesMode
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.games.WinterNavigationButton
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.SnowWhite

@Composable
fun ShapesAdventureScreen(
    onModeSelected: (ShapesMode) -> Unit,
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
                    text = stringResource(R.string.shapes_name),
                    color = SnowWhite,
                    fontSize = if (tabletLayout) 40.sp else 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.shapes_choose_adventure),
                    color = SnowWhite.copy(alpha = 0.92f),
                    fontSize = if (tabletLayout) 20.sp else 17.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(if (tabletLayout) 22.dp else 14.dp))

                if (tabletLayout) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 920.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        ShapesMode.entries.forEach { mode ->
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
                            .widthIn(max = 440.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ShapesMode.entries.forEach { mode ->
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
    mode: ShapesMode,
    tabletLayout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(mode.titleResource())
    val subtitle = stringResource(mode.subtitleResource())
    val description = stringResource(R.string.shapes_mode_description, title, subtitle)
    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = if (tabletLayout) 270.dp else 142.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
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
            ShapesGameGlyph(Modifier.fillMaxWidth().height(if (tabletLayout) 80.dp else 60.dp))
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
private fun ShapesMode.titleResource(): Int = when (this) {
    ShapesMode.LEARN -> R.string.shapes_mode_learn
    ShapesMode.FIND_SHAPE -> R.string.shapes_mode_find
    ShapesMode.FIND_COLOR_SHAPE -> R.string.shapes_mode_starts
}

@StringRes
private fun ShapesMode.subtitleResource(): Int = when (this) {
    ShapesMode.LEARN -> R.string.shapes_mode_learn_subtitle
    ShapesMode.FIND_SHAPE -> R.string.shapes_mode_find_subtitle
    ShapesMode.FIND_COLOR_SHAPE -> R.string.shapes_mode_starts_subtitle
}

private fun ShapesMode.cardColor(): Color = when (this) {
    ShapesMode.LEARN -> Color(0xFFF2F7FF)
    ShapesMode.FIND_SHAPE -> Color(0xFFFFEFF8)
    ShapesMode.FIND_COLOR_SHAPE -> Color(0xFFFFF3CC)
}
