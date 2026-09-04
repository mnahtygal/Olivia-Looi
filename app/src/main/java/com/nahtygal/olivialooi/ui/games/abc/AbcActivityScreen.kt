package com.nahtygal.olivialooi.ui.games.abc

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
import com.nahtygal.olivialooi.games.abc.AbcEngine
import com.nahtygal.olivialooi.games.abc.AbcMode
import com.nahtygal.olivialooi.games.abc.AbcRoundState
import com.nahtygal.olivialooi.games.abc.AbcSpeech
import com.nahtygal.olivialooi.games.abc.AbcState
import com.nahtygal.olivialooi.games.abc.AlphabetCatalog
import com.nahtygal.olivialooi.games.abc.AlphabetEntry
import com.nahtygal.olivialooi.speech.AndroidTextToSpeech
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.FrostBlue
import com.nahtygal.olivialooi.ui.theme.SnowWhite
import kotlinx.coroutines.delay

private val AbcStateSaver = Saver<AbcState, String>(
    save = { state ->
        listOf(
            state.selectedMode.name,
            state.currentTarget.stableId,
            state.previousTarget?.stableId.orEmpty(),
            state.answerChoices.joinToString(",", transform = AlphabetEntry::stableId),
            state.selectedChoiceId.orEmpty(),
            state.sessionStars,
            state.completedRoundCount,
            state.roundState.name,
            state.helpCount,
            state.attemptIdentity,
            state.awardedAttemptIdentity?.toString().orEmpty(),
            state.sessionComplete,
            state.alphabetIndex,
        ).joinToString("|")
    },
    restore = { saved ->
        runCatching {
            val fields = saved.split('|')
            AbcState(
                selectedMode = enumValueOf<AbcMode>(fields[0]),
                currentTarget = requireNotNull(AlphabetCatalog.findById(fields[1])),
                previousTarget = fields[2]
                    .takeIf(String::isNotEmpty)
                    ?.let { requireNotNull(AlphabetCatalog.findById(it)) },
                answerChoices = fields[3]
                    .takeIf(String::isNotEmpty)
                    ?.split(',')
                    ?.map { requireNotNull(AlphabetCatalog.findById(it)) }
                    .orEmpty(),
                selectedChoiceId = fields[4].takeIf(String::isNotEmpty),
                sessionStars = fields[5].toInt(),
                completedRoundCount = fields[6].toInt(),
                roundState = enumValueOf<AbcRoundState>(fields[7]),
                helpCount = fields[8].toInt(),
                attemptIdentity = fields[9].toLong(),
                awardedAttemptIdentity = fields[10].toLongOrNull(),
                sessionComplete = fields[11].toBoolean(),
                alphabetIndex = fields[12].toInt(),
            )
        }.getOrNull()
    },
)

@Composable
fun AbcActivityScreen(
    mode: AbcMode,
    sessionId: Int,
    onPickAnotherAdventureClick: () -> Unit,
    onBackToGamesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val textToSpeech = remember(context) { AndroidTextToSpeech(context.applicationContext) }
    var state by rememberSaveable(mode.name, sessionId, stateSaver = AbcStateSaver) {
        mutableStateOf(AbcEngine.newGame(mode))
    }

    DisposableEffect(textToSpeech) {
        onDispose { textToSpeech.close() }
    }
    LaunchedEffect(
        state.selectedMode,
        state.currentTarget.stableId,
        state.completedRoundCount,
    ) {
        if (state.roundState == AbcRoundState.READY) {
            textToSpeech.speak(AbcSpeech.prompt(state))
        }
    }
    LaunchedEffect(state.roundState, state.attemptIdentity) {
        if (state.roundState == AbcRoundState.EVALUATING) {
            val attemptIdentity = state.attemptIdentity
            delay(FEEDBACK_DELAY_MILLIS)
            state = AbcEngine.finishEvaluation(state, attemptIdentity)
        }
    }

    fun selectChoice(choice: AlphabetEntry) {
        val updated = AbcEngine.selectChoice(state, choice.stableId)
        if (updated === state) return
        state = updated
        textToSpeech.speak(
            if (updated.roundComplete) {
                buildString {
                    append(AbcSpeech.correct(updated))
                    if (updated.sessionComplete) {
                        append(' ')
                        append(AbcSpeech.SESSION_COMPLETE)
                    }
                }
            } else {
                AbcSpeech.INCORRECT
            },
        )
    }

    fun requestHelp() {
        val updated = AbcEngine.requestHelp(state)
        if (updated === state) return
        state = updated
        textToSpeech.speak(AbcSpeech.help(updated))
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
                    text = stringResource(R.string.abc_name),
                    color = SnowWhite,
                    fontSize = if (tabletLayout) 36.sp else 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(if (tabletLayout) 12.dp else 8.dp))

                when {
                    state.selectedMode == AbcMode.LEARN -> LearnActivity(
                        state = state,
                        tabletLayout = tabletLayout,
                        onMainCardClick = { textToSpeech.speak(AbcSpeech.prompt(state)) },
                        onPreviousClick = {
                            textToSpeech.stop()
                            state = AbcEngine.previousLetter(state)
                        },
                        onSayAgainClick = { textToSpeech.speak(AbcSpeech.prompt(state)) },
                        onNextClick = {
                            textToSpeech.stop()
                            state = AbcEngine.nextLetter(state)
                        },
                        onBackClick = onPickAnotherAdventureClick,
                    )

                    state.sessionComplete && state.roundState == AbcRoundState.COMPLETED ->
                        AbcSessionComplete(
                            stars = state.sessionStars,
                            tabletLayout = tabletLayout,
                            onPlayAgainClick = {
                                textToSpeech.stop()
                                state = AbcEngine.newSession(state)
                            },
                            onPickAnotherAdventureClick = onPickAnotherAdventureClick,
                            onBackToGamesClick = onBackToGamesClick,
                        )

                    else -> QuizActivity(
                        state = state,
                        tabletLayout = tabletLayout,
                        onChoiceClick = ::selectChoice,
                        onSayAgainClick = { textToSpeech.speak(AbcSpeech.prompt(state)) },
                        onHelpClick = ::requestHelp,
                        onNextClick = {
                            textToSpeech.stop()
                            state = AbcEngine.nextRound(state)
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
    state: AbcState,
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
            .fillMaxWidth()
            .widthIn(max = 760.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            onClick = onMainCardClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (tabletLayout) 430.dp else 330.dp)
                .clearAndSetSemantics { contentDescription = entry.accessibilityDescription },
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
                    text = entry.letter.toString(),
                    color = Color(0xFF6554C0),
                    fontSize = if (tabletLayout) 130.sp else 100.sp,
                    lineHeight = if (tabletLayout) 138.sp else 106.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = entry.visualSymbol,
                    fontSize = if (tabletLayout) 104.sp else 80.sp,
                    lineHeight = if (tabletLayout) 112.sp else 88.sp,
                )
                Text(
                    text = entry.displayWord,
                    color = DeepIndigo,
                    fontSize = if (tabletLayout) 36.sp else 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        if (tabletLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AbcActionButton(
                    stringResource(R.string.abc_previous),
                    stringResource(R.string.abc_previous_description),
                    onPreviousClick,
                    Modifier.weight(1f),
                )
                AbcActionButton(
                    stringResource(R.string.abc_say_again),
                    stringResource(R.string.abc_say_again_description),
                    onSayAgainClick,
                    Modifier.weight(1f),
                )
                AbcActionButton(
                    stringResource(R.string.abc_next),
                    stringResource(R.string.abc_next_letter_description),
                    onNextClick,
                    Modifier.weight(1f),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AbcActionButton(
                    stringResource(R.string.abc_previous),
                    stringResource(R.string.abc_previous_description),
                    onPreviousClick,
                    Modifier.weight(1f),
                )
                AbcActionButton(
                    stringResource(R.string.abc_next),
                    stringResource(R.string.abc_next_letter_description),
                    onNextClick,
                    Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            AbcActionButton(
                stringResource(R.string.abc_say_again),
                stringResource(R.string.abc_say_again_description),
                onSayAgainClick,
                Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        AbcActionButton(
            stringResource(R.string.abc_back_to_adventures),
            stringResource(R.string.abc_back_to_adventures),
            onBackClick,
        )
    }
}

@Composable
private fun QuizActivity(
    state: AbcState,
    tabletLayout: Boolean,
    onChoiceClick: (AlphabetEntry) -> Unit,
    onSayAgainClick: () -> Unit,
    onHelpClick: () -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 860.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AbcStatusRow(state, tabletLayout)
        Spacer(modifier = Modifier.height(10.dp))
        val prompt = if (state.selectedMode == AbcMode.FIND_LETTER) {
            stringResource(R.string.abc_find_prompt, state.currentTarget.letter)
        } else {
            stringResource(R.string.abc_starts_prompt, state.currentTarget.letter)
        }
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
                if (state.selectedMode == AbcMode.STARTS_WITH) {
                    Text(
                        text = state.currentTarget.letter.toString(),
                        color = Color(0xFFE060A6),
                        fontSize = if (tabletLayout) 68.sp else 52.sp,
                        lineHeight = if (tabletLayout) 74.sp else 58.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(if (tabletLayout) 16.dp else 11.dp))

        if (state.selectedMode == AbcMode.STARTS_WITH && !tabletLayout) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.answerChoices.forEach { choice ->
                    ObjectChoiceCard(
                        choice = choice,
                        state = state,
                        tabletLayout = false,
                        onClick = { onChoiceClick(choice) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (tabletLayout) 15.dp else 8.dp),
            ) {
                state.answerChoices.forEach { choice ->
                    if (state.selectedMode == AbcMode.FIND_LETTER) {
                        LetterChoiceCard(
                            choice = choice,
                            state = state,
                            tabletLayout = tabletLayout,
                            onClick = { onChoiceClick(choice) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        ObjectChoiceCard(
                            choice = choice,
                            state = state,
                            tabletLayout = true,
                            onClick = { onChoiceClick(choice) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        QuizFeedback(state, tabletLayout)
        if (state.roundState == AbcRoundState.COMPLETED) {
            AbcActionButton(
                stringResource(R.string.abc_next_round),
                stringResource(R.string.abc_next_round_description),
                onNextClick,
                emphasized = true,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AbcActionButton(
                    stringResource(R.string.abc_say_again),
                    stringResource(R.string.abc_say_prompt_again_description),
                    onSayAgainClick,
                    Modifier.weight(1f),
                    enabled = !state.inputLocked,
                )
                AbcActionButton(
                    stringResource(R.string.abc_help),
                    stringResource(R.string.abc_help_description),
                    onHelpClick,
                    Modifier.weight(1f),
                    enabled = !state.inputLocked,
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        AbcActionButton(
            stringResource(R.string.abc_back_to_adventures),
            stringResource(R.string.abc_back_to_adventures),
            onBackClick,
        )
    }
}

@Composable
private fun AbcStatusRow(state: AbcState, tabletLayout: Boolean) {
    val roundNumber = if (state.roundComplete) {
        state.completedRoundCount
    } else {
        state.completedRoundCount + 1
    }.coerceIn(1, AbcEngine.ROUNDS_PER_SESSION)
    val progress = stringResource(R.string.abc_round_progress, roundNumber, AbcEngine.ROUNDS_PER_SESSION)
    val stars = stringResource(R.string.abc_stars, state.sessionStars)
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
private fun LetterChoiceCard(
    choice: AlphabetEntry,
    state: AbcState,
    tabletLayout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.selectedChoiceId == choice.stableId
    val correct = selected && choice.stableId == state.currentTarget.stableId
    val description = choiceDescription("Letter ${choice.letter}", selected, correct)
    ChoiceCard(
        modifier = modifier,
        selected = selected,
        correct = correct,
        enabled = !state.inputLocked,
        description = description,
        onClick = onClick,
    ) {
        Text(
            text = choice.letter.toString(),
            color = DeepIndigo,
            fontSize = if (tabletLayout) 76.sp else 58.sp,
            fontWeight = FontWeight.Black,
        )
        SelectionLabel(selected, correct, tabletLayout)
    }
}

@Composable
private fun ObjectChoiceCard(
    choice: AlphabetEntry,
    state: AbcState,
    tabletLayout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.selectedChoiceId == choice.stableId
    val correct = selected && choice.stableId == state.currentTarget.stableId
    val description = choiceDescription("${choice.displayWord}. Choose ${choice.displayWord}", selected, correct)
    ChoiceCard(
        modifier = modifier,
        selected = selected,
        correct = correct,
        enabled = !state.inputLocked,
        description = description,
        onClick = onClick,
        minimumHeight = if (tabletLayout) 185.dp else 100.dp,
    ) {
        if (tabletLayout) {
            Text(text = choice.visualSymbol, fontSize = 66.sp, lineHeight = 72.sp)
            Text(
                text = choice.displayWord,
                color = DeepIndigo,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(text = choice.visualSymbol, fontSize = 48.sp)
                Text(
                    text = choice.displayWord,
                    modifier = Modifier.padding(start = 18.dp),
                    color = DeepIndigo,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        SelectionLabel(selected, correct, tabletLayout)
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
            .clearAndSetSemantics { contentDescription = description },
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
            "✓ ${stringResource(R.string.abc_found_it)}"
        } else {
            "↻ ${stringResource(R.string.abc_try_again)}"
        },
        color = if (correct) Color(0xFF317A4C) else Color(0xFF9B5B19),
        fontSize = if (tabletLayout) 16.sp else 13.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun QuizFeedback(state: AbcState, tabletLayout: Boolean) {
    val message = when {
        state.roundComplete -> stringResource(R.string.abc_correct_feedback)
        state.roundState == AbcRoundState.EVALUATING -> stringResource(R.string.abc_incorrect_feedback)
        state.helpCount > 0 -> if (state.selectedMode == AbcMode.FIND_LETTER) {
            stringResource(R.string.abc_find_help_visible, state.currentTarget.letter)
        } else {
            stringResource(R.string.abc_starts_help_visible)
        }
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
private fun AbcSessionComplete(
    stars: Int,
    tabletLayout: Boolean,
    onPlayAgainClick: () -> Unit,
    onPickAnotherAdventureClick: () -> Unit,
    onBackToGamesClick: () -> Unit,
) {
    val earnedStars = pluralStringResource(R.plurals.abc_session_stars, stars, stars)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
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
            Text(text = "🎉  A B C  ⭐", fontSize = if (tabletLayout) 45.sp else 32.sp)
            Text(
                text = stringResource(R.string.abc_session_complete),
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
            AbcActionButton(
                stringResource(R.string.abc_play_again),
                stringResource(R.string.abc_play_again_description),
                onPlayAgainClick,
                emphasized = true,
            )
            AbcActionButton(
                stringResource(R.string.abc_pick_another),
                stringResource(R.string.abc_pick_another_description),
                onPickAnotherAdventureClick,
            )
            AbcActionButton(
                stringResource(R.string.back_to_games),
                stringResource(R.string.back_to_games),
                onBackToGamesClick,
            )
        }
    }
}

@Composable
private fun AbcActionButton(
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
            .clearAndSetSemantics { contentDescription = description },
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
