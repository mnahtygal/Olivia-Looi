package com.nahtygal.olivialooi.ui.games.shapes

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.games.shapes.ShapesEngine
import com.nahtygal.olivialooi.games.shapes.ShapesMode
import com.nahtygal.olivialooi.games.shapes.ShapesRoundState
import com.nahtygal.olivialooi.games.shapes.ShapesSpeech
import com.nahtygal.olivialooi.games.shapes.ShapesState
import com.nahtygal.olivialooi.games.shapes.ShapesStateCodec
import androidx.compose.ui.semantics.semantics
import com.nahtygal.olivialooi.games.shapes.ShapeChoice
import com.nahtygal.olivialooi.speech.AndroidTextToSpeech
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.FrostBlue
import com.nahtygal.olivialooi.ui.theme.SnowWhite
import kotlinx.coroutines.delay

private val ShapesStateSaver = Saver<ShapesState, String>(
    save = { ShapesStateCodec.encode(it) },
    restore = ShapesStateCodec::decode,
)

@Composable
fun ShapesActivityScreen(
    mode: ShapesMode,
    sessionId: Int,
    onPickAnotherAdventureClick: () -> Unit,
    onBackToGamesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val textToSpeech = remember(context) { AndroidTextToSpeech(context.applicationContext) }
    var state by rememberSaveable(mode.name, sessionId, stateSaver = ShapesStateSaver) {
        mutableStateOf(ShapesEngine.newGame(mode))
    }

    DisposableEffect(textToSpeech) {
        onDispose { textToSpeech.close() }
    }
    LaunchedEffect(
        state.selectedMode,
        state.currentTarget.stableId,
        state.completedRoundCount,
    ) {
        if (state.roundState == ShapesRoundState.READY) {
            textToSpeech.speak(ShapesSpeech.prompt(state))
        }
    }
    LaunchedEffect(state.roundState, state.attemptIdentity) {
        if (state.roundState == ShapesRoundState.EVALUATING) {
            val attemptIdentity = state.attemptIdentity
            delay(FEEDBACK_DELAY_MILLIS)
            state = ShapesEngine.finishEvaluation(state, attemptIdentity)
        }
    }

    fun selectChoice(choice: ShapeChoice, expectedAttemptIdentity: Long) {
        val updated = ShapesEngine.selectChoice(state, choice.stableId, expectedAttemptIdentity)
        if (updated === state) return
        state = updated
        textToSpeech.speak(
            if (updated.roundComplete) {
                buildString {
                    append(ShapesSpeech.correct(updated))
                    if (updated.sessionComplete) {
                        append(' ')
                        append(ShapesSpeech.SESSION_COMPLETE)
                    }
                }
            } else {
                ShapesSpeech.INCORRECT
            },
        )
    }

    fun requestHelp() {
        val updated = ShapesEngine.requestHelp(state)
        if (updated === state) return
        state = updated
        textToSpeech.speak(ShapesSpeech.help(updated))
    }

    WinterGamesBackground(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabletLayout = maxWidth >= 600.dp
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
                    text = stringResource(R.string.shapes_name),
                    color = SnowWhite,
                    fontSize = if (tabletLayout) 36.sp else 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(if (tabletLayout) 12.dp else 8.dp))

                when {
                    state.selectedMode == ShapesMode.LEARN -> LearnActivity(
                        state = state,
                        tabletLayout = tabletLayout,
                        onMainCardClick = { textToSpeech.speak(ShapesSpeech.prompt(state)) },
                        onPreviousClick = {
                            textToSpeech.stop()
                            state = ShapesEngine.previousShape(state)
                        },
                        onSayAgainClick = { textToSpeech.speak(ShapesSpeech.prompt(state)) },
                        onNextClick = {
                            textToSpeech.stop()
                            state = ShapesEngine.nextShape(state)
                        },
                        onBackClick = onPickAnotherAdventureClick,
                    )

                    state.sessionComplete && state.roundState == ShapesRoundState.COMPLETED ->
                        ShapesSessionComplete(
                            stars = state.sessionStars,
                            tabletLayout = tabletLayout,
                            onPlayAgainClick = {
                                textToSpeech.stop()
                                state = ShapesEngine.newSession(state)
                            },
                            onPickAnotherAdventureClick = onPickAnotherAdventureClick,
                            onBackToGamesClick = onBackToGamesClick,
                        )

                    else -> QuizActivity(
                        state = state,
                        tabletLayout = tabletLayout,
                        onChoiceClick = ::selectChoice,
                        onSayAgainClick = { textToSpeech.speak(ShapesSpeech.prompt(state)) },
                        onHelpClick = ::requestHelp,
                        onNextClick = {
                            textToSpeech.stop()
                            state = ShapesEngine.nextRound(state)
                        },
                        onBackClick = onPickAnotherAdventureClick,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun LearnActivity(
    state: ShapesState,
    tabletLayout: Boolean,
    onMainCardClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSayAgainClick: () -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    val entry = state.currentTarget
    Column(
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            onClick = onMainCardClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (tabletLayout) 430.dp else 330.dp)
                .semantics(mergeDescendants = true) { contentDescription = entry.accessibilityName },
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = SnowWhite.copy(alpha = 0.97f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 11.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = entry.color.displayName,
                    color = DeepIndigo,
                    fontSize = if (tabletLayout) 32.sp else 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                ShapeDrawing(entry, Modifier.fillMaxWidth().height(if (tabletLayout) 300.dp else 220.dp))
                Text(
                    text = entry.shape.displayName,
                    color = DeepIndigo,
                    fontSize = if (tabletLayout) 36.sp else 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        if (tabletLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ShapesActionButton(
                    stringResource(R.string.shapes_previous),
                    stringResource(R.string.shapes_previous_description),
                    onPreviousClick,
                    Modifier.weight(1f),
                )
                ShapesActionButton(
                    stringResource(R.string.shapes_say_again),
                    stringResource(R.string.shapes_say_again_description),
                    onSayAgainClick,
                    Modifier.weight(1f),
                )
                ShapesActionButton(
                    stringResource(R.string.shapes_next),
                    stringResource(R.string.shapes_next_shape_description),
                    onNextClick,
                    Modifier.weight(1f),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShapesActionButton(
                    stringResource(R.string.shapes_previous),
                    stringResource(R.string.shapes_previous_description),
                    onPreviousClick,
                    Modifier.weight(1f),
                )
                ShapesActionButton(
                    stringResource(R.string.shapes_next),
                    stringResource(R.string.shapes_next_shape_description),
                    onNextClick,
                    Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            ShapesActionButton(
                stringResource(R.string.shapes_say_again),
                stringResource(R.string.shapes_say_again_description),
                onSayAgainClick,
                Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        ShapesActionButton(
            stringResource(R.string.shapes_back_to_adventures),
            stringResource(R.string.shapes_back_to_adventures),
            onBackClick,
        )
    }
}

@Composable
private fun QuizActivity(
    state: ShapesState,
    tabletLayout: Boolean,
    onChoiceClick: (ShapeChoice, Long) -> Unit,
    onSayAgainClick: () -> Unit,
    onHelpClick: () -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 860.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ShapesStatusRow(state, tabletLayout)
        Spacer(modifier = Modifier.height(10.dp))
        val prompt = ShapesSpeech.prompt(state)
        Card(
            modifier = Modifier.clearAndSetSemantics { contentDescription = prompt },
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = SnowWhite.copy(alpha = 0.97f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 34.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = prompt,
                    color = DeepIndigo,
                    fontSize = if (tabletLayout) 25.sp else 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(modifier = Modifier.height(if (tabletLayout) 16.dp else 11.dp))

        if (tabletLayout) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                state.answerChoices.forEach { choice ->
                    ShapeChoiceCard(choice, state, true, { onChoiceClick(choice, state.attemptIdentity) }, Modifier.weight(1f))
                }
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.answerChoices.forEach { choice ->
                    ShapeChoiceCard(choice, state, false, { onChoiceClick(choice, state.attemptIdentity) }, Modifier.fillMaxWidth())
                }
            }
        }

        QuizFeedback(state, tabletLayout)
        if (state.roundState == ShapesRoundState.COMPLETED) {
            ShapesActionButton(
                stringResource(R.string.shapes_next_round),
                stringResource(R.string.shapes_next_round_description),
                onNextClick,
                emphasized = true,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ShapesActionButton(
                    stringResource(R.string.shapes_say_again),
                    stringResource(R.string.shapes_say_prompt_again_description),
                    onSayAgainClick,
                    Modifier.weight(1f),
                    enabled = !state.inputLocked,
                )
                ShapesActionButton(
                    stringResource(R.string.shapes_help),
                    stringResource(R.string.shapes_help_description),
                    onHelpClick,
                    Modifier.weight(1f),
                    enabled = !state.inputLocked,
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        ShapesActionButton(
            stringResource(R.string.shapes_back_to_adventures),
            stringResource(R.string.shapes_back_to_adventures),
            onBackClick,
        )
    }
}

@Composable
private fun ShapesStatusRow(state: ShapesState, tabletLayout: Boolean) {
    val roundNumber = if (state.roundComplete) {
        state.completedRoundCount
    } else {
        state.completedRoundCount + 1
    }.coerceIn(1, ShapesEngine.ROUNDS_PER_SESSION)
    val progress = stringResource(R.string.shapes_round_progress, roundNumber, ShapesEngine.ROUNDS_PER_SESSION)
    val stars = stringResource(R.string.shapes_stars, state.sessionStars)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = progress,
            modifier = Modifier.clearAndSetSemantics { contentDescription = progress },
            color = SnowWhite,
            fontSize = if (tabletLayout) 18.sp else 15.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stars,
            modifier = Modifier.clearAndSetSemantics { contentDescription = stars },
            color = Color(0xFFFFF0A6),
            fontSize = if (tabletLayout) 20.sp else 16.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun ShapeChoiceCard(
    choice: ShapeChoice,
    state: ShapesState,
    tabletLayout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.selectedChoiceId == choice.stableId
    val correct = selected && ShapesEngine.isCorrect(state, choice)
    ChoiceCard(
        modifier = modifier,
        selected = selected,
        correct = correct,
        enabled = !state.inputLocked,
        description = choiceDescription(choice.accessibilityName, selected, correct),
        onClick = onClick,
    ) {
        ShapeDrawing(choice, Modifier.fillMaxWidth().height(if (tabletLayout) 220.dp else 170.dp))
        Box(Modifier.heightIn(min = 28.dp)) { SelectionLabel(selected, correct, tabletLayout) }
    }
}

@Composable
private fun ChoiceCard(
    selected: Boolean,
    correct: Boolean,
    enabled: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minimumHeight: androidx.compose.ui.unit.Dp = 126.dp,
    content: @Composable () -> Unit,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = minimumHeight)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                correct -> Color(0xFFE3F7E9)
                selected -> Color(0xFFFFF0D5)
                else -> SnowWhite
            },
            disabledContainerColor = when {
                correct -> Color(0xFFE3F7E9)
                selected -> Color(0xFFFFF0D5)
                else -> SnowWhite
            },
        ),
        border = BorderStroke(
            if (selected) 6.dp else 2.dp,
            when {
                correct -> Color(0xFF3D965E)
                selected -> Color(0xFFF09A3E)
                else -> FrostBlue
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) { content() }
    }
}

@Composable
private fun SelectionLabel(selected: Boolean, correct: Boolean, tabletLayout: Boolean) {
    if (!selected) return
    Text(
        text = if (correct) {
            "✓ ${stringResource(R.string.shapes_found_it)}"
        } else {
            "↻ ${stringResource(R.string.shapes_try_again)}"
        },
        color = if (correct) Color(0xFF317A4C) else Color(0xFF9B5B19),
        fontSize = if (tabletLayout) 16.sp else 13.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun QuizFeedback(state: ShapesState, tabletLayout: Boolean) {
    val message = when {
        state.roundComplete -> stringResource(R.string.shapes_correct_feedback)
        state.roundState == ShapesRoundState.EVALUATING -> stringResource(R.string.shapes_incorrect_feedback)
        state.helpCount > 0 -> ShapesSpeech.help(state)
        else -> null
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (tabletLayout) 58.dp else 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        message?.let {
            Text(
                text = it,
                color = SnowWhite,
                fontSize = if (tabletLayout) 22.sp else 17.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ShapesSessionComplete(
    stars: Int,
    tabletLayout: Boolean,
    onPlayAgainClick: () -> Unit,
    onPickAnotherAdventureClick: () -> Unit,
    onBackToGamesClick: () -> Unit,
) {
    val earnedStars = pluralStringResource(R.plurals.shapes_session_stars, stars, stars)
    Card(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth(),
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
            Text(text = "🎉  ⭐", fontSize = if (tabletLayout) 45.sp else 32.sp)
            Text(
                text = stringResource(R.string.shapes_session_complete),
                color = DeepIndigo,
                fontSize = if (tabletLayout) 32.sp else 25.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = earnedStars,
                modifier = Modifier.clearAndSetSemantics { contentDescription = earnedStars },
                color = DeepIndigo,
                fontSize = if (tabletLayout) 24.sp else 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            ShapesActionButton(
                stringResource(R.string.shapes_play_again),
                stringResource(R.string.shapes_play_again_description),
                onPlayAgainClick,
                emphasized = true,
            )
            ShapesActionButton(
                stringResource(R.string.shapes_pick_another),
                stringResource(R.string.shapes_pick_another_description),
                onPickAnotherAdventureClick,
            )
            ShapesActionButton(
                stringResource(R.string.back_to_games),
                stringResource(R.string.back_to_games),
                onBackToGamesClick,
            )
        }
    }
}

@Composable
private fun ShapesActionButton(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = 56.dp)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (emphasized) Color(0xFFE060A6) else SnowWhite,
            contentColor = if (emphasized) SnowWhite else DeepIndigo,
            disabledContainerColor = SnowWhite.copy(alpha = 0.72f),
            disabledContentColor = DeepIndigo.copy(alpha = 0.55f),
        ),
    ) {
        Text(text = label, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private fun choiceDescription(base: String, selected: Boolean, correct: Boolean): String = when {
    correct -> "$base. Selected and correct."
    selected -> "$base. Selected, try again."
    else -> base
}

private const val FEEDBACK_DELAY_MILLIS = 900L
