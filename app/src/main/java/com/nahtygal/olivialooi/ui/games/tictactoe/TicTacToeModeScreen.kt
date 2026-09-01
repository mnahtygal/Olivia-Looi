package com.nahtygal.olivialooi.ui.games.tictactoe

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.games.tictactoe.TicTacToeGameMode
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.games.WinterNavigationButton
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.SnowWhite

@Composable
fun TicTacToeModeScreen(
    onModeSelected: (TicTacToeGameMode) -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WinterGamesBackground(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            val tabletLayout = maxWidth >= 600.dp
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.tic_tac_toe_mode_heading),
                    color = SnowWhite,
                    fontSize = if (tabletLayout) 34.sp else 25.sp,
                    lineHeight = if (tabletLayout) 40.sp else 29.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.tic_tac_toe_mode_subtitle),
                    color = SnowWhite.copy(alpha = 0.92f),
                    fontSize = if (tabletLayout) 19.sp else 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(if (tabletLayout) 24.dp else 10.dp))

                if (tabletLayout) {
                    Row(
                        modifier = Modifier.widthIn(max = 720.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        ModeCard(
                            imageResource = R.drawable.ic_mode_uncle_moo,
                            imageDescriptionResource = R.string.play_with_uncle_moo,
                            labelResource = R.string.play_with_uncle_moo,
                            subtitleResource = R.string.play_with_uncle_moo_subtitle,
                            containerColor = Color(0xFFF2F7FF),
                            imageScale = ContentScale.Fit,
                            tabletLayout = true,
                            onClick = {
                                onModeSelected(TicTacToeGameMode.PersonVsPerson)
                            },
                            modifier = Modifier.weight(1f),
                        )
                        ModeCard(
                            imageResource = R.drawable.ic_mode_play_with_looloo,
                            imageDescriptionResource = R.string.play_with_looloo,
                            labelResource = R.string.play_with_looloo,
                            subtitleResource = R.string.play_with_looloo_subtitle,
                            containerColor = Color(0xFFFFEFF8),
                            imageScale = ContentScale.Crop,
                            tabletLayout = true,
                            onClick = {
                                onModeSelected(TicTacToeGameMode.PersonVsComputer)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ModeCard(
                            imageResource = R.drawable.ic_mode_uncle_moo,
                            imageDescriptionResource = R.string.play_with_uncle_moo,
                            labelResource = R.string.play_with_uncle_moo,
                            subtitleResource = R.string.play_with_uncle_moo_subtitle,
                            containerColor = Color(0xFFF2F7FF),
                            imageScale = ContentScale.Fit,
                            tabletLayout = false,
                            onClick = {
                                onModeSelected(TicTacToeGameMode.PersonVsPerson)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        ModeCard(
                            imageResource = R.drawable.ic_mode_play_with_looloo,
                            imageDescriptionResource = R.string.play_with_looloo,
                            labelResource = R.string.play_with_looloo,
                            subtitleResource = R.string.play_with_looloo_subtitle,
                            containerColor = Color(0xFFFFEFF8),
                            imageScale = ContentScale.Crop,
                            tabletLayout = false,
                            onClick = {
                                onModeSelected(TicTacToeGameMode.PersonVsComputer)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (tabletLayout) 20.dp else 10.dp))
                WinterNavigationButton(
                    labelResource = R.string.back_to_home,
                    onClick = onHomeClick,
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    @DrawableRes imageResource: Int,
    @StringRes imageDescriptionResource: Int,
    @StringRes labelResource: Int,
    @StringRes subtitleResource: Int,
    containerColor: Color,
    imageScale: ContentScale,
    tabletLayout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = if (tabletLayout) 390.dp else 122.dp),
        shape = RoundedCornerShape(if (tabletLayout) 28.dp else 22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
    ) {
        if (tabletLayout) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(imageResource),
                    contentDescription = stringResource(imageDescriptionResource),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                        .clip(RoundedCornerShape(22.dp)),
                    contentScale = imageScale,
                )
                Spacer(modifier = Modifier.height(8.dp))
                ModeCardText(labelResource, subtitleResource, tabletLayout = true)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(imageResource),
                    contentDescription = stringResource(imageDescriptionResource),
                    modifier = Modifier
                        .size(106.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = imageScale,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                ) {
                    ModeCardText(labelResource, subtitleResource, tabletLayout = false)
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
private fun ModeCardText(
    @StringRes labelResource: Int,
    @StringRes subtitleResource: Int,
    tabletLayout: Boolean,
) {
    Text(
        text = stringResource(labelResource),
        color = DeepIndigo,
        fontSize = if (tabletLayout) 28.sp else 20.sp,
        lineHeight = if (tabletLayout) 31.sp else 23.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = if (tabletLayout) TextAlign.Center else TextAlign.Start,
    )
    Text(
        text = stringResource(subtitleResource),
        color = DeepIndigo.copy(alpha = 0.76f),
        fontSize = if (tabletLayout) 17.sp else 14.sp,
        lineHeight = if (tabletLayout) 21.sp else 18.sp,
        textAlign = if (tabletLayout) TextAlign.Center else TextAlign.Start,
    )
}
