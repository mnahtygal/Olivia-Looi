package com.nahtygal.olivialooi.ui.games

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.FrostBlue
import com.nahtygal.olivialooi.ui.theme.SnowWhite

@Composable
fun GamesScreen(
    onTicTacToeClick: () -> Unit,
    onMemoryMatchClick: () -> Unit,
    onColoringClick: () -> Unit,
    onSpeakAndSpellClick: () -> Unit,
    onAnimalSoundsClick: () -> Unit,
    onCountingClick: () -> Unit,
    onMathClick: () -> Unit,
    onAbcClick: () -> Unit,
    onHomeClick: () -> Unit,
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
                    .padding(
                        start = 22.dp,
                        top = if (tabletLayout) 8.dp else 16.dp,
                        end = 22.dp,
                        bottom = 16.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.games_heading),
                    color = SnowWhite,
                    fontSize = if (tabletLayout) 40.sp else 34.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.games_subtitle),
                    color = SnowWhite.copy(alpha = 0.9f),
                    fontSize = if (tabletLayout) 20.sp else 17.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(if (tabletLayout) 12.dp else 14.dp))

                if (tabletLayout) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 760.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            GameMenuCard(
                                labelResource = R.string.tic_tac_toe_name,
                                subtitleResource = R.string.tic_tac_toe_card_subtitle,
                                accessibilityResource = R.string.open_tic_tac_toe_description,
                                onClick = onTicTacToeClick,
                                tabletLayout = true,
                                modifier = Modifier.weight(1f),
                            ) { TicTacToeGameGlyph(Modifier.fillMaxSize()) }
                            GameMenuCard(
                                labelResource = R.string.memory_match_name,
                                subtitleResource = R.string.memory_match_card_subtitle,
                                accessibilityResource = R.string.open_memory_match_description,
                                onClick = onMemoryMatchClick,
                                tabletLayout = true,
                                modifier = Modifier.weight(1f),
                            ) { MemoryMatchGameGlyph(Modifier.fillMaxSize()) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            GameMenuCard(
                                labelResource = R.string.coloring_name,
                                subtitleResource = R.string.coloring_card_subtitle,
                                accessibilityResource = R.string.open_coloring_description,
                                onClick = onColoringClick,
                                tabletLayout = true,
                                modifier = Modifier.weight(1f),
                            ) { ColoringGameGlyph(Modifier.fillMaxSize()) }
                            GameMenuCard(
                                labelResource = R.string.speak_spell_name,
                                subtitleResource = R.string.speak_spell_card_subtitle,
                                accessibilityResource = R.string.open_speak_spell_description,
                                onClick = onSpeakAndSpellClick,
                                tabletLayout = true,
                                modifier = Modifier.weight(1f),
                            ) { SpeakAndSpellGameGlyph(Modifier.fillMaxSize()) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            GameMenuCard(
                                labelResource = R.string.animal_sounds_name,
                                subtitleResource = R.string.animal_sounds_card_subtitle,
                                accessibilityResource = R.string.open_animal_sounds_description,
                                onClick = onAnimalSoundsClick,
                                tabletLayout = true,
                                modifier = Modifier.weight(1f),
                            ) { AnimalSoundsGameGlyph(Modifier.fillMaxSize()) }
                            GameMenuCard(
                                labelResource = R.string.counting_name,
                                subtitleResource = R.string.counting_card_subtitle,
                                accessibilityResource = R.string.open_counting_description,
                                onClick = onCountingClick,
                                tabletLayout = true,
                                modifier = Modifier.weight(1f),
                            ) { CountingGameGlyph(Modifier.fillMaxSize()) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            GameMenuCard(
                                labelResource = R.string.math_name,
                                subtitleResource = R.string.math_card_subtitle,
                                accessibilityResource = R.string.open_math_description,
                                onClick = onMathClick,
                                tabletLayout = true,
                                modifier = Modifier.weight(1f),
                            ) { MathGameGlyph(Modifier.fillMaxSize()) }
                            GameMenuCard(
                                labelResource = R.string.abc_name,
                                subtitleResource = R.string.abc_card_subtitle,
                                accessibilityResource = R.string.open_abc_description,
                                onClick = onAbcClick,
                                tabletLayout = true,
                                modifier = Modifier.weight(1f),
                            ) { AbcGameGlyph(Modifier.fillMaxSize()) }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sizeIn(maxWidth = 440.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        GameMenuCard(
                            labelResource = R.string.tic_tac_toe_name,
                            subtitleResource = R.string.tic_tac_toe_card_subtitle,
                            accessibilityResource = R.string.open_tic_tac_toe_description,
                            onClick = onTicTacToeClick,
                            tabletLayout = false,
                            modifier = Modifier.fillMaxWidth(),
                        ) { TicTacToeGameGlyph(Modifier.fillMaxSize()) }
                        GameMenuCard(
                            labelResource = R.string.memory_match_name,
                            subtitleResource = R.string.memory_match_card_subtitle,
                            accessibilityResource = R.string.open_memory_match_description,
                            onClick = onMemoryMatchClick,
                            tabletLayout = false,
                            modifier = Modifier.fillMaxWidth(),
                        ) { MemoryMatchGameGlyph(Modifier.fillMaxSize()) }
                        GameMenuCard(
                            labelResource = R.string.coloring_name,
                            subtitleResource = R.string.coloring_card_subtitle,
                            accessibilityResource = R.string.open_coloring_description,
                            onClick = onColoringClick,
                            tabletLayout = false,
                            modifier = Modifier.fillMaxWidth(),
                        ) { ColoringGameGlyph(Modifier.fillMaxSize()) }
                        GameMenuCard(
                            labelResource = R.string.speak_spell_name,
                            subtitleResource = R.string.speak_spell_card_subtitle,
                            accessibilityResource = R.string.open_speak_spell_description,
                            onClick = onSpeakAndSpellClick,
                            tabletLayout = false,
                            modifier = Modifier.fillMaxWidth(),
                        ) { SpeakAndSpellGameGlyph(Modifier.fillMaxSize()) }
                        GameMenuCard(
                            labelResource = R.string.animal_sounds_name,
                            subtitleResource = R.string.animal_sounds_card_subtitle,
                            accessibilityResource = R.string.open_animal_sounds_description,
                            onClick = onAnimalSoundsClick,
                            tabletLayout = false,
                            modifier = Modifier.fillMaxWidth(),
                        ) { AnimalSoundsGameGlyph(Modifier.fillMaxSize()) }
                        GameMenuCard(
                            labelResource = R.string.counting_name,
                            subtitleResource = R.string.counting_card_subtitle,
                            accessibilityResource = R.string.open_counting_description,
                            onClick = onCountingClick,
                            tabletLayout = false,
                            modifier = Modifier.fillMaxWidth(),
                        ) { CountingGameGlyph(Modifier.fillMaxSize()) }
                        GameMenuCard(
                            labelResource = R.string.math_name,
                            subtitleResource = R.string.math_card_subtitle,
                            accessibilityResource = R.string.open_math_description,
                            onClick = onMathClick,
                            tabletLayout = false,
                            modifier = Modifier.fillMaxWidth(),
                        ) { MathGameGlyph(Modifier.fillMaxSize()) }
                        GameMenuCard(
                            labelResource = R.string.abc_name,
                            subtitleResource = R.string.abc_card_subtitle,
                            accessibilityResource = R.string.open_abc_description,
                            onClick = onAbcClick,
                            tabletLayout = false,
                            modifier = Modifier.fillMaxWidth(),
                        ) { AbcGameGlyph(Modifier.fillMaxSize()) }
                    }
                }

                Spacer(modifier = Modifier.height(if (tabletLayout) 12.dp else 14.dp))
                WinterNavigationButton(
                    labelResource = R.string.back_to_home,
                    onClick = onHomeClick,
                )
            }
        }
    }
}

@Composable
private fun GameMenuCard(
    @StringRes labelResource: Int,
    @StringRes subtitleResource: Int,
    @StringRes accessibilityResource: Int,
    onClick: () -> Unit,
    tabletLayout: Boolean,
    modifier: Modifier = Modifier,
    glyph: @Composable () -> Unit,
) {
    val description = stringResource(accessibilityResource)
    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = if (tabletLayout) 250.dp else 148.dp)
            .clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(if (tabletLayout) 30.dp else 24.dp),
        colors = CardDefaults.cardColors(containerColor = SnowWhite.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
    ) {
        if (tabletLayout) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sizeIn(maxWidth = 145.dp)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center,
                ) { glyph() }
                Spacer(modifier = Modifier.height(7.dp))
                GameMenuText(labelResource, subtitleResource, tabletLayout = true)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(112.dp), contentAlignment = Alignment.Center) { glyph() }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                ) {
                    GameMenuText(labelResource, subtitleResource, tabletLayout = false)
                }
                Text(
                    text = stringResource(R.string.open_game_mode_symbol),
                    color = DeepIndigo.copy(alpha = 0.72f),
                    fontSize = 34.sp,
                )
            }
        }
    }
}

@Composable
private fun GameMenuText(
    @StringRes labelResource: Int,
    @StringRes subtitleResource: Int,
    tabletLayout: Boolean,
) {
    Text(
        text = stringResource(labelResource),
        color = DeepIndigo,
        fontSize = if (tabletLayout) 29.sp else 23.sp,
        lineHeight = if (tabletLayout) 34.sp else 27.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = if (tabletLayout) TextAlign.Center else TextAlign.Start,
    )
    Text(
        text = stringResource(subtitleResource),
        color = DeepIndigo.copy(alpha = 0.76f),
        fontSize = if (tabletLayout) 17.sp else 15.sp,
        lineHeight = 20.sp,
        textAlign = if (tabletLayout) TextAlign.Center else TextAlign.Start,
    )
}

@Composable
private fun TicTacToeGameGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
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

@Composable
private fun MemoryMatchGameGlyph(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .size(width = 82.dp, height = 108.dp)
                        .background(FrostBlue, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "★",
                        color = Color(0xFFE060A6),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColoringGameGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val paletteCenter = Offset(size.width * 0.48f, size.height * 0.52f)
        drawOval(
            color = FrostBlue,
            topLeft = Offset(size.width * 0.10f, size.height * 0.18f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.66f, size.height * 0.66f),
        )
        listOf(
            Color(0xFFE85E9F) to Offset(-0.17f, -0.15f),
            Color(0xFFFFA13D) to Offset(0.04f, -0.22f),
            Color(0xFF47A86C) to Offset(0.20f, -0.05f),
            Color(0xFF4A8DE0) to Offset(0.10f, 0.18f),
            Color(0xFF7864D6) to Offset(-0.15f, 0.18f),
        ).forEach { (color, position) ->
            drawCircle(
                color = color,
                radius = size.minDimension * 0.075f,
                center = Offset(
                    paletteCenter.x + size.width * position.x,
                    paletteCenter.y + size.height * position.y,
                ),
            )
        }
        drawLine(
            color = DeepIndigo,
            start = Offset(size.width * 0.66f, size.height * 0.78f),
            end = Offset(size.width * 0.91f, size.height * 0.16f),
            strokeWidth = size.minDimension * 0.055f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color(0xFFE060A6),
            start = Offset(size.width * 0.88f, size.height * 0.23f),
            end = Offset(size.width * 0.93f, size.height * 0.10f),
            strokeWidth = size.minDimension * 0.075f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun SpeakAndSpellGameGlyph(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            "A" to Color(0xFFE060A6),
            "B" to Color(0xFF6554C0),
            "C" to Color(0xFF3A91C7),
        ).forEach { (letter, color) ->
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(color, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter,
                    color = SnowWhite,
                    fontSize = 31.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun AnimalSoundsGameGlyph(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = "🐮",
            fontSize = 86.sp,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(Color(0xFFFFEAF5), RoundedCornerShape(18.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text = "MOO!",
                color = DeepIndigo,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun CountingGameGlyph(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            "1" to Color(0xFFE060A6),
            "2" to Color(0xFF6554C0),
            "3" to Color(0xFF3A91C7),
        ).forEach { (number, color) ->
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(color, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number,
                    color = SnowWhite,
                    fontSize = 31.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun MathGameGlyph(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "3 + 2",
                color = Color(0xFF6554C0),
                fontSize = 38.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "= ?  ⭐",
                color = Color(0xFFE060A6),
                fontSize = 31.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun AbcGameGlyph(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            "A" to Color(0xFFE060A6),
            "B" to Color(0xFF6554C0),
            "C" to Color(0xFF3A91C7),
        ).forEach { (letter, color) ->
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(color, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter,
                    color = SnowWhite,
                    fontSize = 31.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}
