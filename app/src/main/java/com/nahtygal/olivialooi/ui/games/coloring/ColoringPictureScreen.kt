package com.nahtygal.olivialooi.ui.games.coloring

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.games.coloring.ColoringPicture
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.games.WinterNavigationButton
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.SnowWhite

@Composable
fun ColoringPictureScreen(
    onPictureSelected: (ColoringPicture) -> Unit,
    onGamesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WinterGamesBackground(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabletLayout = maxWidth >= 600.dp
            val columnCount = if (tabletLayout) 4 else 2
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = if (tabletLayout) 24.dp else 14.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.coloring_name),
                    color = SnowWhite,
                    fontSize = if (tabletLayout) 40.sp else 33.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.coloring_pick_picture),
                    color = SnowWhite.copy(alpha = 0.92f),
                    fontSize = if (tabletLayout) 20.sp else 17.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(if (tabletLayout) 22.dp else 14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 920.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ColoringPicture.entries.chunked(columnCount).forEach { pictures ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            pictures.forEach { picture ->
                                PictureCard(
                                    picture = picture,
                                    tabletLayout = tabletLayout,
                                    onClick = { onPictureSelected(picture) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
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
private fun PictureCard(
    picture: ColoringPicture,
    tabletLayout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(picture.labelResource())
    val description = stringResource(R.string.coloring_picture_description, name)
    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = if (tabletLayout) 235.dp else 190.dp)
            .clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SnowWhite.copy(alpha = 0.97f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ColoringPagePreview(
                picture = picture,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(18.dp)),
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = name,
                color = DeepIndigo,
                fontSize = if (tabletLayout) 20.sp else 18.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

internal fun ColoringPicture.labelResource(): Int = when (this) {
    ColoringPicture.Butterfly -> R.string.coloring_butterfly
    ColoringPicture.Flower -> R.string.coloring_flower
    ColoringPicture.Puppy -> R.string.coloring_puppy
    ColoringPicture.Snowman -> R.string.coloring_snowman
}
