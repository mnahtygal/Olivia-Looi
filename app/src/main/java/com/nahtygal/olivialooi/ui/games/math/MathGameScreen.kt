package com.nahtygal.olivialooi.ui.games.math

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.games.math.MathEngine
import com.nahtygal.olivialooi.games.math.MathLevel
import com.nahtygal.olivialooi.games.math.MathOperation
import com.nahtygal.olivialooi.games.math.MathProblem
import com.nahtygal.olivialooi.games.math.MathRoundState
import com.nahtygal.olivialooi.games.math.MathSpeech
import com.nahtygal.olivialooi.games.math.MathState
import com.nahtygal.olivialooi.games.math.MathVisualHelp
import com.nahtygal.olivialooi.speech.AndroidTextToSpeech
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.FrostBlue
import com.nahtygal.olivialooi.ui.theme.SnowWhite
import kotlinx.coroutines.delay

private val MathStateSaver = Saver<MathState, String>(
    save = { state ->
        listOf(
            state.selectedLevel.name,
            state.effectiveLevel.name,
            state.currentProblem.encode(),
            state.previousProblem?.encode().orEmpty(),
            state.selectedAnswer?.toString().orEmpty(),
            state.roundState.name,
            state.helpCount,
            state.completedProblemCount,
            state.sessionStars,
            state.sessionComplete,
            state.attemptIdentity,
            state.awardedAttemptIdentity?.toString().orEmpty(),
        ).joinToString("|")
    },
    restore = { saved ->
        runCatching {
            val fields = saved.split('|')
            MathState(
                selectedLevel = enumValueOf<MathLevel>(fields[0]),
                effectiveLevel = enumValueOf<MathLevel>(fields[1]),
                currentProblem = fields[2].decodeProblem(),
                previousProblem = fields[3].takeIf(String::isNotEmpty)?.decodeProblem(),
                selectedAnswer = fields[4].toIntOrNull(),
                roundState = enumValueOf<MathRoundState>(fields[5]),
                helpCount = fields[6].toInt(),
                completedProblemCount = fields[7].toInt(),
                sessionStars = fields[8].toInt(),
                sessionComplete = fields[9].toBoolean(),
                attemptIdentity = fields[10].toLong(),
                awardedAttemptIdentity = fields[11].toLongOrNull(),
            )
        }.getOrNull()
    },
)

@Composable
fun MathGameScreen(
    level: MathLevel,
    sessionId: Int,
    onPickAnotherLevelClick: () -> Unit,
    onBackToGamesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val textToSpeech = remember(context) { AndroidTextToSpeech(context.applicationContext) }
    var state by rememberSaveable(level.name, sessionId, stateSaver = MathStateSaver) {
        mutableStateOf(MathEngine.newGame(level))
    }

    DisposableEffect(textToSpeech) {
        onDispose { textToSpeech.close() }
    }
    LaunchedEffect(
        state.currentProblem.operation,
        state.currentProblem.leftOperand,
        state.currentProblem.rightOperand,
        state.completedProblemCount,
    ) {
        if (state.roundState == MathRoundState.READY) {
            textToSpeech.speak(MathSpeech.problem(state))
        }
    }
    LaunchedEffect(state.roundState, state.attemptIdentity) {
        if (state.roundState == MathRoundState.EVALUATING) {
            val attemptIdentity = state.attemptIdentity
            delay(FEEDBACK_DELAY_MILLIS)
            state = MathEngine.finishEvaluation(state, attemptIdentity)
        }
    }

    fun selectAnswer(answer: Int) {
        val updated = MathEngine.selectAnswer(state, answer)
        if (updated === state) return
        state = updated
        textToSpeech.speak(
            if (updated.roundComplete) {
                buildString {
                    append(MathSpeech.correct(updated))
                    if (updated.sessionComplete) {
                        append(' ')
                        append(MathSpeech.sessionComplete(updated.sessionStars))
                    }
                }
            } else {
                MathSpeech.INCORRECT
            },
        )
    }

    fun requestHelp() {
        val updated = MathEngine.requestHelp(state)
        if (updated === state) return
        state = updated
        textToSpeech.speak(MathSpeech.help(updated))
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
                    text = stringResource(R.string.math_name),
                    color = SnowWhite,
                    fontSize = if (tabletLayout) 36.sp else 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                MathStatusRow(state, tabletLayout)
                Spacer(modifier = Modifier.height(if (tabletLayout) 14.dp else 9.dp))

                if (state.sessionComplete && state.roundState == MathRoundState.COMPLETED) {
                    MathSessionComplete(
                        stars = state.sessionStars,
                        tabletLayout = tabletLayout,
                        onMathMoreClick = {
                            textToSpeech.stop()
                            state = MathEngine.newSession(state)
                        },
                        onPickAnotherLevelClick = onPickAnotherLevelClick,
                        onBackToGamesClick = onBackToGamesClick,
                    )
                } else {
                    MathRound(
                        state = state,
                        tabletLayout = tabletLayout,
                        onAnswerClick = ::selectAnswer,
                        onSayAgainClick = { textToSpeech.speak(MathSpeech.problem(state)) },
                        onHelpClick = ::requestHelp,
                        onNextProblemClick = {
                            textToSpeech.stop()
                            state = MathEngine.nextProblem(state)
                        },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MathActionButton(
                        label = stringResource(R.string.back_to_games),
                        description = stringResource(R.string.back_to_games),
                        onClick = onBackToGamesClick,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MathStatusRow(state: MathState, tabletLayout: Boolean) {
    val problemNumber = if (state.roundComplete) {
        state.completedProblemCount
    } else {
        state.completedProblemCount + 1
    }.coerceIn(1, MathEngine.PROBLEMS_PER_SESSION)
    val progress = stringResource(
        R.string.math_problem_progress,
        problemNumber,
        MathEngine.PROBLEMS_PER_SESSION,
    )
    val stars = stringResource(R.string.math_stars, state.sessionStars)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = progress,
            modifier = Modifier.clearAndSetSemantics { contentDescription = progress },
            color = SnowWhite.copy(alpha = 0.95f),
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
private fun MathRound(
    state: MathState,
    tabletLayout: Boolean,
    onAnswerClick: (Int) -> Unit,
    onSayAgainClick: () -> Unit,
    onHelpClick: () -> Unit,
    onNextProblemClick: () -> Unit,
) {
    val problem = state.currentProblem
    val problemDescription = stringResource(
        R.string.math_problem_description,
        problem.leftOperand,
        problem.operation.spokenName,
        problem.rightOperand,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 820.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier.clearAndSetSemantics { contentDescription = problemDescription },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = SnowWhite.copy(alpha = 0.97f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 9.dp),
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = if (tabletLayout) 58.dp else 36.dp,
                    vertical = if (tabletLayout) 20.dp else 13.dp,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${problem.leftOperand} ${problem.operation.symbol} ${problem.rightOperand}",
                    color = DeepIndigo,
                    fontSize = if (tabletLayout) 55.sp else 40.sp,
                    lineHeight = if (tabletLayout) 61.sp else 45.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "= ?",
                    color = Color(0xFFE060A6),
                    fontSize = if (tabletLayout) 43.sp else 32.sp,
                    lineHeight = if (tabletLayout) 48.sp else 36.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Spacer(modifier = Modifier.height(if (tabletLayout) 18.dp else 12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (tabletLayout) 16.dp else 8.dp),
        ) {
            problem.answerChoices.forEach { answer ->
                MathAnswerCard(
                    answer = answer,
                    state = state,
                    tabletLayout = tabletLayout,
                    onClick = { onAnswerClick(answer) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        MathFeedback(state, tabletLayout)

        MathEngine.visualHelp(state)?.let { help ->
            Spacer(modifier = Modifier.height(10.dp))
            LittleMathVisualHelp(help, tabletLayout)
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (state.roundState == MathRoundState.COMPLETED) {
            MathActionButton(
                label = stringResource(R.string.math_next_problem),
                description = stringResource(R.string.math_next_problem_description),
                onClick = onNextProblemClick,
                emphasized = true,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MathActionButton(
                    label = stringResource(R.string.math_say_again),
                    description = stringResource(R.string.math_say_again_description),
                    onClick = onSayAgainClick,
                    enabled = !state.inputLocked,
                    modifier = Modifier.weight(1f),
                )
                MathActionButton(
                    label = stringResource(R.string.math_help),
                    description = stringResource(R.string.math_help_description),
                    onClick = onHelpClick,
                    enabled = !state.inputLocked,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MathAnswerCard(
    answer: Int,
    state: MathState,
    tabletLayout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.selectedAnswer == answer
    val correct = selected && answer == state.currentProblem.correctAnswer
    val feedback = when {
        correct -> stringResource(R.string.math_answer_correct_state)
        selected -> stringResource(R.string.math_answer_try_again_state)
        else -> stringResource(R.string.math_answer_not_selected_state)
    }
    val description = stringResource(R.string.math_answer_description, answer, feedback)
    Card(
        onClick = onClick,
        enabled = !state.inputLocked,
        modifier = modifier
            .heightIn(min = if (tabletLayout) 130.dp else 96.dp)
            .clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(if (tabletLayout) 28.dp else 22.dp),
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
            width = if (selected) 6.dp else 2.dp,
            color = when {
                correct -> Color(0xFF3D965E)
                selected -> Color(0xFFF09A3E)
                else -> FrostBlue
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = answer.toString(),
                color = DeepIndigo,
                fontSize = if (tabletLayout) 43.sp else 32.sp,
                fontWeight = FontWeight.Black,
            )
            if (selected) {
                Text(
                    text = if (correct) "✓ ${stringResource(R.string.math_correct)}" else "↻ ${stringResource(R.string.math_try_again)}",
                    color = if (correct) Color(0xFF317A4C) else Color(0xFF9B5B19),
                    fontSize = if (tabletLayout) 17.sp else 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MathFeedback(state: MathState, tabletLayout: Boolean) {
    val message = when {
        state.roundComplete -> stringResource(R.string.math_correct_feedback)
        state.roundState == MathRoundState.EVALUATING -> stringResource(R.string.math_incorrect_feedback)
        else -> null
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (tabletLayout) 60.dp else 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        message?.let {
            Text(
                text = it,
                color = SnowWhite,
                fontSize = if (tabletLayout) 24.sp else 18.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LittleMathVisualHelp(help: MathVisualHelp, tabletLayout: Boolean) {
    val description = when (help) {
        is MathVisualHelp.Addition -> stringResource(
            R.string.math_visual_addition_description,
            help.startingCount,
            help.addedCount,
        )
        is MathVisualHelp.Subtraction -> stringResource(
            R.string.math_visual_subtraction_description,
            help.startingCount,
            help.removedCount,
        )
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EEFF)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (help) {
                is MathVisualHelp.Addition -> {
                    DotGroup(help.startingCount, tabletLayout)
                    Text(" + ", color = DeepIndigo, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    DotGroup(help.addedCount, tabletLayout)
                }
                is MathVisualHelp.Subtraction -> {
                    DotGroup(
                        count = help.startingCount,
                        tabletLayout = tabletLayout,
                        crossedOutCount = help.removedCount,
                    )
                }
            }
        }
    }
}

@Composable
private fun DotGroup(count: Int, tabletLayout: Boolean, crossedOutCount: Int = 0) {
    Row(horizontalArrangement = Arrangement.spacedBy(if (tabletLayout) 7.dp else 4.dp)) {
        repeat(count) { index ->
            val removed = index >= count - crossedOutCount
            Text(
                text = "●",
                modifier = if (removed) Modifier.alpha(0.42f) else Modifier,
                color = if (removed) Color(0xFFE060A6) else Color(0xFF6554C0),
                fontSize = if (tabletLayout) 27.sp else 21.sp,
                fontWeight = FontWeight.Black,
                textDecoration = if (removed) TextDecoration.LineThrough else null,
            )
        }
    }
}

@Composable
private fun MathSessionComplete(
    stars: Int,
    tabletLayout: Boolean,
    onMathMoreClick: () -> Unit,
    onPickAnotherLevelClick: () -> Unit,
    onBackToGamesClick: () -> Unit,
) {
    val earnedStars = pluralStringResource(R.plurals.math_session_stars, stars, stars)
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
            Text(text = "🎉  ⭐  🎉", fontSize = if (tabletLayout) 48.sp else 35.sp)
            Text(
                text = stringResource(R.string.math_session_complete),
                color = DeepIndigo,
                fontSize = if (tabletLayout) 32.sp else 25.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = earnedStars,
                modifier = Modifier.clearAndSetSemantics { contentDescription = earnedStars },
                color = DeepIndigo,
                fontSize = if (tabletLayout) 25.sp else 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            MathActionButton(
                label = stringResource(R.string.math_more),
                description = stringResource(R.string.math_more_description),
                onClick = onMathMoreClick,
                emphasized = true,
            )
            MathActionButton(
                label = stringResource(R.string.math_pick_another_level),
                description = stringResource(R.string.math_pick_another_level_description),
                onClick = onPickAnotherLevelClick,
            )
            MathActionButton(
                label = stringResource(R.string.back_to_games),
                description = stringResource(R.string.back_to_games),
                onClick = onBackToGamesClick,
            )
        }
    }
}

@Composable
private fun MathActionButton(
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

private fun MathProblem.encode(): String = listOf(
    operation.name,
    leftOperand,
    rightOperand,
    correctAnswer,
    answerChoices.joinToString(","),
).joinToString(";")

private fun String.decodeProblem(): MathProblem {
    val fields = split(';')
    return MathProblem(
        operation = enumValueOf<MathOperation>(fields[0]),
        leftOperand = fields[1].toInt(),
        rightOperand = fields[2].toInt(),
        correctAnswer = fields[3].toInt(),
        answerChoices = fields[4].split(',').map(String::toInt),
    )
}

private const val FEEDBACK_DELAY_MILLIS = 900L
