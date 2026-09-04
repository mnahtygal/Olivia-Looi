package com.nahtygal.olivialooi.ui.games.animals

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.games.animals.AnimalId
import com.nahtygal.olivialooi.games.animals.AnimalSound
import com.nahtygal.olivialooi.games.animals.AnimalSoundsCatalog
import com.nahtygal.olivialooi.speech.AndroidTextToSpeech
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.games.WinterNavigationButton
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.SnowWhite

@Composable
fun AnimalSoundsScreen(
    onGamesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val textToSpeech = remember(context) { AndroidTextToSpeech(context.applicationContext) }
    var selectedAnimalId by rememberSaveable { mutableStateOf<String?>(null) }
    var feedbackVersion by rememberSaveable { mutableIntStateOf(0) }
    var showSoundWord by rememberSaveable { mutableStateOf(false) }
    val selectedAnimal = selectedAnimalId?.let(AnimalSoundsCatalog::findById)

    DisposableEffect(textToSpeech) {
        onDispose { textToSpeech.close() }
    }
    LaunchedEffect(feedbackVersion) {
        if (feedbackVersion > 0) {
            showSoundWord = true
            delay(SOUND_WORD_DURATION_MILLIS)
            showSoundWord = false
        }
    }

    WinterGamesBackground(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabletLayout = maxWidth >= 600.dp
            val columns = if (tabletLayout || maxWidth >= 390.dp) 2 else 1
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(
                        start = if (tabletLayout) 24.dp else 16.dp,
                        top = if (tabletLayout) 8.dp else 12.dp,
                        end = if (tabletLayout) 24.dp else 16.dp,
                        bottom = if (tabletLayout) 24.dp else 12.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.animal_sounds_name),
                    color = SnowWhite,
                    fontSize = if (tabletLayout) 40.sp else 33.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.animal_sounds_prompt),
                    color = SnowWhite.copy(alpha = 0.92f),
                    fontSize = if (tabletLayout) 20.sp else 17.sp,
                    textAlign = TextAlign.Center,
                )
                Box(
                    modifier = Modifier.height(if (tabletLayout) 54.dp else 58.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (showSoundWord && selectedAnimal != null) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = SnowWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        ) {
                            Text(
                                text = selectedAnimal.soundWord.uppercase(),
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                                color = DeepIndigo,
                                fontSize = if (tabletLayout) 31.sp else 25.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 920.dp),
                    verticalArrangement = Arrangement.spacedBy(if (tabletLayout) 10.dp else 12.dp),
                ) {
                    AnimalSoundsCatalog.animals.chunked(columns).forEach { animalsInRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(if (tabletLayout) 18.dp else 10.dp),
                        ) {
                            animalsInRow.forEach { animal ->
                                AnimalTile(
                                    animal = animal,
                                    tabletLayout = tabletLayout,
                                    selected = selectedAnimalId == animal.id.stableId,
                                    feedbackVersion = if (selectedAnimalId == animal.id.stableId) {
                                        feedbackVersion
                                    } else {
                                        0
                                    },
                                    onClick = {
                                        selectedAnimalId = animal.id.stableId
                                        feedbackVersion += 1
                                        textToSpeech.speak(animal.spokenPhrase)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(columns - animalsInRow.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (tabletLayout) 14.dp else 16.dp))
                WinterNavigationButton(
                    labelResource = R.string.back_to_games,
                    onClick = onGamesClick,
                )
            }
        }
    }
}

@Composable
private fun AnimalTile(
    animal: AnimalSound,
    tabletLayout: Boolean,
    selected: Boolean,
    feedbackVersion: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(1f) }
    val description = stringResource(
        R.string.animal_sound_tile_description,
        animal.displayName,
        animal.displayName.lowercase(),
    )
    LaunchedEffect(feedbackVersion) {
        if (feedbackVersion == 0) {
            scale.snapTo(1f)
        } else {
            scale.snapTo(0.94f)
            scale.animateTo(1.06f, tween(90))
            scale.animateTo(1f, tween(130))
        }
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = if (tabletLayout) 182.dp else 205.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(if (tabletLayout) 32.dp else 26.dp),
        colors = CardDefaults.cardColors(containerColor = animal.tileColor()),
        border = BorderStroke(
            width = if (selected) 6.dp else 2.dp,
            color = if (selected) Color(0xFFE454A4) else SnowWhite.copy(alpha = 0.9f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (tabletLayout) 10.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = animal.visualSymbol,
                fontSize = if (tabletLayout) 94.sp else 76.sp,
                lineHeight = if (tabletLayout) 104.sp else 84.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = animal.displayName,
                color = DeepIndigo,
                fontSize = if (tabletLayout) 30.sp else 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun AnimalSound.tileColor(): Color = when (id) {
    AnimalId.Cow -> Color(0xFFF3EEFF)
    AnimalId.Dog -> Color(0xFFFFEEDA)
    AnimalId.Cat -> Color(0xFFFFEAF5)
    AnimalId.Pig -> Color(0xFFFFE4EC)
    AnimalId.Duck -> Color(0xFFFFF4C7)
    AnimalId.Sheep -> Color(0xFFEAF6FF)
    AnimalId.Horse -> Color(0xFFF2E6DC)
    AnimalId.Frog -> Color(0xFFE2F6E9)
}

private const val SOUND_WORD_DURATION_MILLIS = 1_600L
