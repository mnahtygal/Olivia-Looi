package com.nahtygal.olivialooi.ui.games.tracing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.games.tracing.LetterVocabulary
import com.nahtygal.olivialooi.games.tracing.LetterTracingState
import com.nahtygal.olivialooi.games.tracing.TraceWord
import com.nahtygal.olivialooi.games.tracing.TracingMode
import com.nahtygal.olivialooi.games.tracing.TracingPoint
import com.nahtygal.olivialooi.games.tracing.WordCatalog
import com.nahtygal.olivialooi.games.tracing.WordTracingEngine
import com.nahtygal.olivialooi.games.tracing.WordTracingSpeech
import com.nahtygal.olivialooi.games.tracing.WordTracingState
import com.nahtygal.olivialooi.speech.AndroidTextToSpeech
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.SnowWhite

@Composable
internal fun WordCategoryMenu(
    onCategory: (String) -> Unit,
    onWritingMenu: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WinterGamesBackground(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WritingTopNavigation(onHomeClick = onHomeClick, onBack = onWritingMenu)
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.tracing_words_category_title),
                color = SnowWhite,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            WordCatalog.categories.forEachIndexed { index, category ->
                Button(
                    onClick = { onCategory(category.stableId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .heightIn(min = 94.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SnowWhite.copy(alpha = .97f),
                        contentColor = DeepIndigo,
                    ),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Text(
                        text = "${category.emoji}  ${category.title}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                }
                if (index < WordCatalog.categories.lastIndex) Spacer(Modifier.height(14.dp))
            }
            Spacer(Modifier.height(18.dp))
            TraceButton(
                stringResource(R.string.tracing_back_to_writing_menu),
                onWritingMenu,
                true,
                Modifier.fillMaxWidth().widthIn(max = 560.dp),
            )
        }
    }
}

@Composable
internal fun WordPracticeScreen(
    state: WordTracingState,
    speech: AndroidTextToSpeech,
    onStateChange: (WordTracingState) -> Unit,
    onComplete: (TraceWord) -> Unit,
    onCategories: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activePoints = remember(state.profileId, state.categoryId) { mutableStateListOf<TracingPoint>() }
    val category = checkNotNull(WordCatalog.category(state.categoryId))
    val canvasState = LetterTracingState(
        mode = TracingMode.NAME,
        letters = state.currentWord.value.toList(),
        letterPosition = state.letterPosition,
        strokesByPosition = state.currentProgress.strokesByLetter,
    )

    WinterGamesBackground(modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val tablet = maxWidth >= 600.dp
            val canvasHeight = when {
                maxHeight < 560.dp -> 220.dp
                tablet -> 360.dp
                else -> 310.dp
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("wordPracticeViewport")
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = if (tablet) 24.dp else 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WritingTopNavigation(onHomeClick = onHomeClick, onBack = onCategories)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.tracing_trace_words).uppercase(),
                    color = SnowWhite,
                    fontSize = if (tablet) 34.sp else 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "${category.emoji}  ${state.currentWord.value}",
                    color = SnowWhite,
                    fontSize = if (tablet) 27.sp else 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(6.dp))
                ActiveWordLetters(state)
                Spacer(Modifier.height(5.dp))
                Text(
                    text = stringResource(
                        R.string.tracing_word_progress,
                        state.wordPosition + 1,
                        state.words.size,
                        state.letterPosition + 1,
                        state.currentWord.value.length,
                    ),
                    color = SnowWhite.copy(alpha = .94f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                TracingCanvas(
                    state = canvasState,
                    activePoints = activePoints,
                    onStrokeFinished = { points ->
                        val updated = WordTracingEngine.addStroke(state, points)
                        if (updated.currentProgress.celebrationPending) {
                            onStateChange(WordTracingEngine.consumeCelebration(updated))
                            onComplete(updated.currentWord)
                        } else {
                            onStateChange(updated)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = if (tablet) 560.dp else 440.dp)
                        .height(canvasHeight),
                )
                Spacer(Modifier.height(12.dp))
                WordControls(
                    state = state,
                    onHearLetter = {
                        speech.speak(LetterVocabulary.hearLetterPhrase(TracingMode.ALPHABET, state.currentLetter))
                    },
                    onHearWord = { speech.speak(WordTracingSpeech.hearWord(state.currentWord)) },
                    onUndo = { onStateChange(WordTracingEngine.undo(state)) },
                    onClear = {
                        activePoints.clear()
                        onStateChange(WordTracingEngine.clear(state))
                    },
                    onPreviousLetter = {
                        activePoints.clear()
                        onStateChange(WordTracingEngine.previousLetter(state))
                    },
                    onNextLetter = {
                        activePoints.clear()
                        onStateChange(WordTracingEngine.nextLetter(state))
                    },
                    onPreviousWord = {
                        activePoints.clear()
                        onStateChange(WordTracingEngine.previousWord(state))
                    },
                    onNextWord = {
                        activePoints.clear()
                        onStateChange(WordTracingEngine.nextWord(state))
                    },
                    modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                )
            }
        }
    }
}

@Composable
private fun ActiveWordLetters(state: WordTracingState) {
    Row(
        modifier = Modifier.semantics {
            contentDescription = "${state.currentWord.value}, letter ${state.letterPosition + 1} active"
        },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.currentWord.value.forEachIndexed { index, letter ->
            Text(
                text = letter.toString(),
                modifier = Modifier
                    .background(
                        if (index == state.letterPosition) Color(0xFFFFD54F) else SnowWhite.copy(alpha = .92f),
                        RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                color = DeepIndigo,
                fontSize = 25.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun WordControls(
    state: WordTracingState,
    onHearLetter: () -> Unit,
    onHearWord: () -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onPreviousLetter: () -> Unit,
    onNextLetter: () -> Unit,
    onPreviousWord: () -> Unit,
    onNextWord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(SnowWhite.copy(alpha = .96f), RoundedCornerShape(24.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpeechButton(stringResource(R.string.tracing_hear_letter), onHearLetter, Modifier.weight(1f))
            SpeechButton(stringResource(R.string.tracing_hear_word), onHearWord, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TraceButton(stringResource(R.string.coloring_undo), onUndo, state.currentStrokes.isNotEmpty(), Modifier.weight(1f))
            TraceButton(stringResource(R.string.coloring_clear), onClear, state.currentStrokes.isNotEmpty(), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TraceButton(stringResource(R.string.tracing_previous_letter), onPreviousLetter, state.letterPosition > 0, Modifier.weight(1f))
            TraceButton(stringResource(R.string.tracing_next_letter), onNextLetter, state.letterPosition < state.currentWord.value.lastIndex, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TraceButton(stringResource(R.string.tracing_previous_word), onPreviousWord, state.wordPosition > 0, Modifier.weight(1f))
            TraceButton(stringResource(R.string.tracing_next_word), onNextWord, state.wordPosition < state.words.lastIndex, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SpeechButton(label: String, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
        shape = RoundedCornerShape(20.dp),
    ) {
        Text(label, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun WordTracingCelebration(
    state: WordTracingState,
    displayName: String,
    onNextWord: () -> Unit,
    onPracticeAgain: () -> Unit,
    onCategories: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CelebrationFrame(onHomeClick = onHomeClick, onBack = onCategories, modifier = modifier) {
        Text(
            text = stringResource(R.string.tracing_word_celebration_title, displayName),
            color = DeepIndigo,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        CelebrationMessage(
            stringResource(R.string.tracing_word_celebration_message, state.currentWord.value),
        )
        Spacer(Modifier.height(22.dp))
        TraceButton(
            stringResource(R.string.tracing_next_word_action),
            onNextWord,
            state.wordPosition < state.words.lastIndex,
            Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        CelebrationButton(stringResource(R.string.tracing_practice_word_again), onPracticeAgain)
        Spacer(Modifier.height(10.dp))
        TraceButton(stringResource(R.string.tracing_back_to_word_categories), onCategories, true, Modifier.fillMaxWidth())
    }
}
