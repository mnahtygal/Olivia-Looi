package com.nahtygal.olivialooi.ui.games.counting

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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
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
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.games.counting.CountingEngine
import com.nahtygal.olivialooi.games.counting.CountingLevel
import com.nahtygal.olivialooi.games.counting.CountingObject
import com.nahtygal.olivialooi.games.counting.CountingObjectInstance
import com.nahtygal.olivialooi.games.counting.CountingSpeech
import com.nahtygal.olivialooi.games.counting.CountingState
import com.nahtygal.olivialooi.speech.AndroidTextToSpeech
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.FrostBlue
import com.nahtygal.olivialooi.ui.theme.SnowWhite

private val CountingStateSaver = Saver<CountingState, String>(
    save = { state ->
        listOf(
            state.level.name,
            state.targetCount,
            state.previousTargetCount.orEmptyValue(),
            state.objectIdentity.name,
            state.previousObjectIdentity?.name.orEmpty(),
            state.displayedObjects.joinToString(",") { it.id.toString() },
            state.selectedObjectIds.joinToString(","),
            state.helpCount,
            state.completedRoundCount,
            state.roundComplete,
            state.sessionComplete,
        ).joinToString("|")
    },
    restore = { saved ->
        runCatching {
            val fields = saved.split('|')
            val identity = enumValueOf<CountingObject>(fields[3])
            CountingState(
                level = enumValueOf<CountingLevel>(fields[0]),
                targetCount = fields[1].toInt(),
                previousTargetCount = fields[2].toIntOrNull(),
                objectIdentity = identity,
                previousObjectIdentity = fields[4]
                    .takeIf(String::isNotEmpty)
                    ?.let { enumValueOf<CountingObject>(it) },
                displayedObjects = fields[5].split(',').map { id ->
                    CountingObjectInstance(id.toInt(), identity)
                },
                selectedObjectIds = fields[6]
                    .takeIf(String::isNotEmpty)
                    ?.split(',')
                    ?.map(String::toInt)
                    ?.toSet()
                    .orEmpty(),
                helpCount = fields[7].toInt(),
                completedRoundCount = fields[8].toInt(),
                roundComplete = fields[9].toBoolean(),
                sessionComplete = fields[10].toBoolean(),
            )
        }.getOrNull()
    },
)

@Composable
fun CountingGameScreen(
    level: CountingLevel,
    onPickAnotherLevelClick: () -> Unit,
    onBackToGamesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val textToSpeech = remember(context) { AndroidTextToSpeech(context.applicationContext) }
    var state by rememberSaveable(level.name, stateSaver = CountingStateSaver) {
        mutableStateOf(CountingEngine.newGame(level))
    }

    DisposableEffect(textToSpeech) {
        onDispose { textToSpeech.close() }
    }
    LaunchedEffect(state.targetCount, state.objectIdentity, state.completedRoundCount) {
        if (!state.roundComplete) textToSpeech.speak(CountingSpeech.roundRequest(state))
    }

    fun selectObject(objectId: Int) {
        val updated = CountingEngine.selectObject(state, objectId)
        if (updated == state) return
        state = updated
        val countSpeech = CountingSpeech.spokenCount(updated.currentCount).orEmpty()
        val speech = if (updated.roundComplete) {
            buildString {
                append(countSpeech)
                append(' ')
                append(CountingSpeech.success(updated))
                if (updated.sessionComplete) {
                    append(' ')
                    append(CountingSpeech.SESSION_CELEBRATION)
                }
            }
        } else {
            countSpeech
        }
        textToSpeech.speak(speech)
    }

    fun requestHelp() {
        val updated = CountingEngine.requestHelp(state)
        if (updated == state) return
        state = updated
        textToSpeech.speak(
            if (updated.helpCount == 1) {
                CountingSpeech.roundRequest(updated)
            } else {
                CountingSpeech.help(updated)
            },
        )
    }

    WinterGamesBackground(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabletLayout = maxWidth >= 600.dp
            val objectColumns = when {
                tabletLayout && state.displayedObjects.size <= 6 -> 3
                tabletLayout -> 4
                maxWidth >= 390.dp -> 3
                else -> 2
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = if (tabletLayout) 24.dp else 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.counting_name),
                    color = SnowWhite,
                    fontSize = if (tabletLayout) 36.sp else 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(
                        R.string.counting_progress,
                        state.completedRoundCount,
                    ),
                    color = SnowWhite.copy(alpha = 0.92f),
                    fontSize = if (tabletLayout) 18.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (state.sessionComplete) {
                    CountingSessionComplete(
                        tabletLayout = tabletLayout,
                        onCountMoreClick = {
                            textToSpeech.stop()
                            state = CountingEngine.newSession(state)
                        },
                        onPickAnotherLevelClick = onPickAnotherLevelClick,
                        onBackToGamesClick = onBackToGamesClick,
                    )
                } else {
                    CountingRound(
                        state = state,
                        tabletLayout = tabletLayout,
                        columns = objectColumns,
                        onObjectClick = ::selectObject,
                        onSayAgainClick = {
                            textToSpeech.speak(CountingSpeech.roundRequest(state))
                        },
                        onStartOverClick = {
                            val reset = CountingEngine.startOver(state)
                            if (reset != state) {
                                state = reset
                                textToSpeech.speak(CountingSpeech.roundRequest(reset))
                            }
                        },
                        onHelpClick = ::requestHelp,
                        onNextCountClick = {
                            textToSpeech.stop()
                            state = CountingEngine.nextRound(state)
                        },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CountingActionButton(
                        label = stringResource(R.string.back_to_games),
                        accessibilityDescription = stringResource(R.string.back_to_games),
                        onClick = onBackToGamesClick,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CountingRound(
    state: CountingState,
    tabletLayout: Boolean,
    columns: Int,
    onObjectClick: (Int) -> Unit,
    onSayAgainClick: () -> Unit,
    onStartOverClick: () -> Unit,
    onHelpClick: () -> Unit,
    onNextCountClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 920.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = SnowWhite.copy(alpha = 0.96f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.counting_find_objects,
                    state.targetCount,
                    state.objectIdentity.quantityName(state.targetCount),
                ),
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 10.dp),
                color = DeepIndigo,
                fontSize = if (tabletLayout) 28.sp else 21.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        CountingObjectGrid(
            state = state,
            tabletLayout = tabletLayout,
            columns = columns,
            onObjectClick = onObjectClick,
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (state.roundComplete) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3B5)),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(4.dp, Color(0xFFE060A6)),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = "🎉  ⭐  🎉", fontSize = if (tabletLayout) 38.sp else 30.sp)
                    Text(
                        text = stringResource(R.string.counting_success),
                        color = DeepIndigo,
                        fontSize = if (tabletLayout) 25.sp else 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CountingActionButton(
                        label = stringResource(R.string.counting_next),
                        accessibilityDescription = stringResource(R.string.counting_next_description),
                        onClick = onNextCountClick,
                        emphasized = true,
                    )
                }
            }
        } else {
            CountingControls(
                tabletLayout = tabletLayout,
                onSayAgainClick = onSayAgainClick,
                onStartOverClick = onStartOverClick,
                onHelpClick = onHelpClick,
            )
        }
    }
}

@Composable
private fun CountingObjectGrid(
    state: CountingState,
    tabletLayout: Boolean,
    columns: Int,
    onObjectClick: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (tabletLayout) 12.dp else 8.dp),
    ) {
        state.displayedObjects.chunked(columns).forEach { rowObjects ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (tabletLayout) 14.dp else 8.dp),
            ) {
                rowObjects.forEach { objectInstance ->
                    CountingObjectTile(
                        objectInstance = objectInstance,
                        selected = objectInstance.id in state.selectedObjectIds,
                        enabled = !state.inputLocked,
                        tabletLayout = tabletLayout,
                        onClick = { onObjectClick(objectInstance.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - rowObjects.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CountingObjectTile(
    objectInstance: CountingObjectInstance,
    selected: Boolean,
    enabled: Boolean,
    tabletLayout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(1f) }
    val countedState = stringResource(
        if (selected) R.string.counting_object_counted else R.string.counting_object_not_counted,
    )
    val description = stringResource(
        R.string.counting_object_description,
        objectInstance.identity.displayName.replaceFirstChar { it.uppercase() },
        countedState,
    )
    LaunchedEffect(selected) {
        if (selected) {
            scale.snapTo(0.93f)
            scale.animateTo(1.06f, tween(90))
            scale.animateTo(1f, tween(120))
        } else {
            scale.snapTo(1f)
        }
    }

    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = if (tabletLayout) 150.dp else 108.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(if (tabletLayout) 28.dp else 21.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFFFEAF5) else SnowWhite.copy(alpha = 0.96f),
            disabledContainerColor = if (selected) Color(0xFFFFEAF5) else SnowWhite.copy(alpha = 0.96f),
        ),
        border = BorderStroke(
            width = if (selected) 6.dp else 2.dp,
            color = if (selected) Color(0xFFE060A6) else FrostBlue,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = objectInstance.identity.visualSymbol,
                fontSize = if (tabletLayout) 72.sp else 51.sp,
                textAlign = TextAlign.Center,
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .sizeIn(minWidth = 36.dp, minHeight = 36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Card(shape = CircleShape, colors = CardDefaults.cardColors(Color(0xFF4A9B68))) {
                        Text(
                            text = "✓",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = SnowWhite,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountingControls(
    tabletLayout: Boolean,
    onSayAgainClick: () -> Unit,
    onStartOverClick: () -> Unit,
    onHelpClick: () -> Unit,
) {
    if (tabletLayout) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CountingActionButton(
                label = stringResource(R.string.counting_say_again),
                accessibilityDescription = stringResource(R.string.counting_say_again_description),
                onClick = onSayAgainClick,
            )
            CountingActionButton(
                label = stringResource(R.string.counting_start_over),
                accessibilityDescription = stringResource(R.string.counting_start_over_description),
                onClick = onStartOverClick,
            )
            CountingActionButton(
                label = stringResource(R.string.counting_help),
                accessibilityDescription = stringResource(R.string.counting_help_description),
                onClick = onHelpClick,
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CountingActionButton(
                label = stringResource(R.string.counting_say_again),
                accessibilityDescription = stringResource(R.string.counting_say_again_description),
                onClick = onSayAgainClick,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountingActionButton(
                    label = stringResource(R.string.counting_start_over),
                    accessibilityDescription = stringResource(R.string.counting_start_over_description),
                    onClick = onStartOverClick,
                    modifier = Modifier.weight(1f),
                )
                CountingActionButton(
                    label = stringResource(R.string.counting_help),
                    accessibilityDescription = stringResource(R.string.counting_help_description),
                    onClick = onHelpClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CountingSessionComplete(
    tabletLayout: Boolean,
    onCountMoreClick: () -> Unit,
    onPickAnotherLevelClick: () -> Unit,
    onBackToGamesClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3B5)),
        border = BorderStroke(5.dp, Color(0xFFE060A6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (tabletLayout) 34.dp else 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "🎉  1  2  3  🎉", fontSize = if (tabletLayout) 48.sp else 34.sp)
            Text(
                text = stringResource(R.string.counting_session_complete),
                color = DeepIndigo,
                fontSize = if (tabletLayout) 32.sp else 25.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            CountingActionButton(
                label = stringResource(R.string.counting_more),
                accessibilityDescription = stringResource(R.string.counting_more),
                onClick = onCountMoreClick,
                emphasized = true,
            )
            CountingActionButton(
                label = stringResource(R.string.counting_pick_another_level),
                accessibilityDescription = stringResource(R.string.counting_pick_another_level),
                onClick = onPickAnotherLevelClick,
            )
            CountingActionButton(
                label = stringResource(R.string.back_to_games),
                accessibilityDescription = stringResource(R.string.back_to_games),
                onClick = onBackToGamesClick,
            )
        }
    }
}

@Composable
private fun CountingActionButton(
    label: String,
    accessibilityDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 56.dp)
            .clearAndSetSemantics { contentDescription = accessibilityDescription },
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (emphasized) Color(0xFFE060A6) else SnowWhite,
            contentColor = if (emphasized) SnowWhite else DeepIndigo,
        ),
    ) {
        Text(text = label, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private fun Int?.orEmptyValue(): String = this?.toString().orEmpty()
