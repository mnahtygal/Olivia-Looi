package com.nahtygal.olivialooi.ui.games.spelling

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
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
import com.nahtygal.olivialooi.games.spelling.LetterTile
import com.nahtygal.olivialooi.games.spelling.SpeakAndSpellEngine
import com.nahtygal.olivialooi.games.spelling.SpeakAndSpellSpeech
import com.nahtygal.olivialooi.games.spelling.SpeakAndSpellState
import com.nahtygal.olivialooi.games.spelling.SpellingLevel
import com.nahtygal.olivialooi.games.spelling.SpellingRoundState
import com.nahtygal.olivialooi.games.spelling.SpellingWordCatalog
import com.nahtygal.olivialooi.speech.AndroidTextToSpeech
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.FrostBlue
import com.nahtygal.olivialooi.ui.theme.ReadyMint
import com.nahtygal.olivialooi.ui.theme.SnowWhite
import kotlinx.coroutines.delay

private val SpeakAndSpellStateSaver = Saver<SpeakAndSpellState, String>(
    save = { state ->
        listOf(
            state.level.name,
            state.currentWord.text,
            state.previousWord?.text.orEmpty(),
            state.letterBank.joinToString(",") { tile ->
                "${tile.id}:${tile.letter}:${tile.isAvailable}"
            },
            state.enteredTileIds.joinToString(","),
            state.helpCount.toString(),
            state.completedWordCount.toString(),
            state.roundState.name,
        ).joinToString("|")
    },
    restore = { saved ->
        runCatching {
            val fields = saved.split('|')
            val level = enumValueOf<SpellingLevel>(fields[0])
            val catalog = SpellingWordCatalog.words(level)
            SpeakAndSpellState(
                level = level,
                currentWord = catalog.first { it.text == fields[1] },
                previousWord = fields[2].takeIf(String::isNotEmpty)?.let { previous ->
                    catalog.first { it.text == previous }
                },
                letterBank = fields[3].split(',').map { savedTile ->
                    val tileFields = savedTile.split(':')
                    LetterTile(
                        id = tileFields[0].toInt(),
                        letter = tileFields[1].single(),
                        isAvailable = tileFields[2].toBoolean(),
                    )
                },
                enteredTileIds = fields[4]
                    .takeIf(String::isNotEmpty)
                    ?.split(',')
                    ?.map(String::toInt)
                    .orEmpty(),
                helpCount = fields[5].toInt(),
                completedWordCount = fields[6].toInt(),
                roundState = enumValueOf(fields[7]),
            )
        }.getOrNull()
    },
)

@Composable
fun SpeakAndSpellGameScreen(
    level: SpellingLevel,
    onPickAnotherLevelClick: () -> Unit,
    onBackToGamesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val textToSpeech = remember(context) { AndroidTextToSpeech(context.applicationContext) }
    var state by rememberSaveable(level.name, stateSaver = SpeakAndSpellStateSaver) {
        mutableStateOf(SpeakAndSpellEngine.newGame(level))
    }
    var nextWordReady by remember(state.currentWord.text) { mutableStateOf(false) }
    val incorrectSpeech = stringResource(R.string.speak_spell_incorrect_speech)
    val sessionSpeech = stringResource(R.string.speak_spell_session_complete)

    DisposableEffect(textToSpeech) {
        onDispose { textToSpeech.close() }
    }
    LaunchedEffect(state.currentWord.text) {
        if (state.roundState == SpellingRoundState.Playing) {
            textToSpeech.speak(SpeakAndSpellSpeech.roundPrompt(state.currentWord.text))
        }
    }
    LaunchedEffect(state.roundState, state.currentWord.text) {
        when (state.roundState) {
            SpellingRoundState.Playing -> Unit
            SpellingRoundState.Correct -> {
                textToSpeech.speak(SpeakAndSpellSpeech.correctAnswer(state.currentWord.text))
                delay(CORRECT_CELEBRATION_PAUSE_MILLIS)
                if (state.roundState == SpellingRoundState.Correct) nextWordReady = true
            }
            SpellingRoundState.Incorrect -> {
                textToSpeech.speak(incorrectSpeech)
                delay(INCORRECT_ANSWER_PAUSE_MILLIS)
                if (state.roundState == SpellingRoundState.Incorrect) {
                    state = SpeakAndSpellEngine.resetIncorrectAnswer(state)
                }
            }
            SpellingRoundState.SessionComplete -> {
                textToSpeech.speak(
                    "${SpeakAndSpellSpeech.correctAnswer(state.currentWord.text)} $sessionSpeech",
                )
            }
        }
    }

    fun requestHelp() {
        val updated = SpeakAndSpellEngine.requestHelp(state)
        if (updated == state) return
        state = updated
        textToSpeech.speak(
            if (updated.helpCount == 1) {
                SpeakAndSpellSpeech.roundPrompt(updated.currentWord.text)
            } else {
                SpeakAndSpellSpeech.firstLetterHelp(updated.currentWord.text)
            },
        )
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
                    text = stringResource(R.string.speak_spell_name),
                    color = SnowWhite,
                    fontSize = if (tabletLayout) 36.sp else 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.speak_spell_words_spelled,
                        state.completedWordCount,
                        state.completedWordCount,
                    ),
                    color = SnowWhite.copy(alpha = 0.92f),
                    fontSize = if (tabletLayout) 18.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (state.roundState == SpellingRoundState.SessionComplete) {
                    SessionCompleteContent(
                        tabletLayout = tabletLayout,
                        onSpellMoreClick = {
                            textToSpeech.stop()
                            state = SpeakAndSpellEngine.newSession(state)
                        },
                        onPickAnotherLevelClick = onPickAnotherLevelClick,
                        onBackToGamesClick = onBackToGamesClick,
                    )
                } else {
                    RoundContent(
                        state = state,
                        tabletLayout = tabletLayout,
                        onTileClick = { tileId ->
                            state = SpeakAndSpellEngine.selectTile(state, tileId)
                        },
                        onBackspaceClick = { state = SpeakAndSpellEngine.backspace(state) },
                        onSayAgainClick = {
                            textToSpeech.speak(
                                SpeakAndSpellSpeech.roundPrompt(state.currentWord.text),
                            )
                        },
                        onHelpClick = ::requestHelp,
                        onNextWordClick = { state = SpeakAndSpellEngine.nextWord(state) },
                        nextWordReady = nextWordReady,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SpellingActionButton(
                        label = stringResource(R.string.back_to_games),
                        onClick = onBackToGamesClick,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun RoundContent(
    state: SpeakAndSpellState,
    tabletLayout: Boolean,
    onTileClick: (Int) -> Unit,
    onBackspaceClick: () -> Unit,
    onSayAgainClick: () -> Unit,
    onHelpClick: () -> Unit,
    onNextWordClick: () -> Unit,
    nextWordReady: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 900.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.currentWord.clue,
            fontSize = if (tabletLayout) 86.sp else 66.sp,
            textAlign = TextAlign.Center,
        )
        AnswerSlots(state = state, tabletLayout = tabletLayout)
        Spacer(modifier = Modifier.height(14.dp))

        when (state.roundState) {
            SpellingRoundState.Correct -> CorrectContent(
                nextWordReady = nextWordReady,
                onNextWordClick = onNextWordClick,
            )
            SpellingRoundState.Incorrect -> IncorrectContent()
            SpellingRoundState.Playing -> {
                LetterBank(
                    state = state,
                    tabletLayout = tabletLayout,
                    onTileClick = onTileClick,
                )
                Spacer(modifier = Modifier.height(14.dp))
                RoundControls(
                    canBackspace = state.enteredTileIds.isNotEmpty(),
                    onBackspaceClick = onBackspaceClick,
                    onSayAgainClick = onSayAgainClick,
                    onHelpClick = onHelpClick,
                )
            }
            SpellingRoundState.SessionComplete -> Unit
        }
    }
}

@Composable
private fun AnswerSlots(state: SpeakAndSpellState, tabletLayout: Boolean) {
    Row(
        modifier = Modifier.widthIn(max = 650.dp),
        horizontalArrangement = Arrangement.spacedBy(if (tabletLayout) 10.dp else 5.dp),
    ) {
        state.answerSlots.forEachIndexed { index, letter ->
            val hintedLetter = state.firstLetterHint.takeIf { index == 0 && letter == null }
            val spokenState = when {
                letter != null -> letter.toString()
                hintedLetter != null -> stringResource(R.string.speak_spell_slot_hint, hintedLetter)
                else -> stringResource(R.string.speak_spell_slot_empty)
            }
            val description = stringResource(
                R.string.speak_spell_slot_description,
                index + 1,
                spokenState,
            )
            Card(
                modifier = Modifier
                    .size(if (tabletLayout) 76.dp else 50.dp)
                    .clearAndSetSemantics { contentDescription = description },
                shape = RoundedCornerShape(if (tabletLayout) 18.dp else 13.dp),
                colors = CardDefaults.cardColors(containerColor = SnowWhite),
                border = BorderStroke(
                    width = if (hintedLetter != null) 4.dp else 3.dp,
                    color = if (hintedLetter != null) Color(0xFFE060A6) else FrostBlue,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = (letter ?: hintedLetter)?.toString().orEmpty(),
                        color = if (hintedLetter != null) Color(0xFFE060A6) else DeepIndigo,
                        fontSize = if (tabletLayout) 39.sp else 27.sp,
                        fontWeight = FontWeight.Black,
                    )
                    if (hintedLetter != null) {
                        Text(
                            text = stringResource(R.string.speak_spell_hint_label),
                            color = DeepIndigo,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterBank(
    state: SpeakAndSpellState,
    tabletLayout: Boolean,
    onTileClick: (Int) -> Unit,
) {
    val columns = if (tabletLayout) state.letterBank.size else 5
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.letterBank.chunked(columns).forEach { tiles ->
            Row(horizontalArrangement = Arrangement.spacedBy(if (tabletLayout) 9.dp else 6.dp)) {
                tiles.forEach { tile ->
                    LetterTileButton(
                        tile = tile,
                        tabletLayout = tabletLayout,
                        onClick = { onTileClick(tile.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LetterTileButton(
    tile: LetterTile,
    tabletLayout: Boolean,
    onClick: () -> Unit,
) {
    val description = stringResource(
        if (tile.isAvailable) {
            R.string.speak_spell_letter_description
        } else {
            R.string.speak_spell_letter_used_description
        },
        tile.letter,
    )
    Card(
        onClick = onClick,
        enabled = tile.isAvailable,
        modifier = Modifier
            .size(if (tabletLayout) 70.dp else 61.dp)
            .clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE9F6FF),
            disabledContainerColor = ReadyMint.copy(alpha = 0.82f),
            contentColor = DeepIndigo,
            disabledContentColor = DeepIndigo,
        ),
        border = BorderStroke(2.dp, SnowWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = tile.letter.toString(),
                fontSize = if (tabletLayout) 34.sp else 29.sp,
                fontWeight = FontWeight.Black,
            )
            if (!tile.isAvailable) {
                Text(
                    text = "✓",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun RoundControls(
    canBackspace: Boolean,
    onBackspaceClick: () -> Unit,
    onSayAgainClick: () -> Unit,
    onHelpClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SpellingActionButton(
            label = stringResource(R.string.speak_spell_say_again),
            onClick = onSayAgainClick,
            description = stringResource(R.string.speak_spell_say_again_description),
            compact = true,
            modifier = Modifier.weight(1f),
        )
        SpellingActionButton(
            label = stringResource(R.string.speak_spell_help_me),
            onClick = onHelpClick,
            description = stringResource(R.string.speak_spell_help_description),
            compact = true,
            modifier = Modifier.weight(1f),
        )
        SpellingActionButton(
            label = stringResource(R.string.speak_spell_backspace),
            onClick = onBackspaceClick,
            description = stringResource(R.string.speak_spell_backspace_description),
            enabled = canBackspace,
            compact = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CorrectContent(
    nextWordReady: Boolean,
    onNextWordClick: () -> Unit,
) {
    Text(text = "🎉  ✨  🎉", fontSize = 31.sp)
    Text(
        text = stringResource(R.string.speak_spell_correct_message),
        color = SnowWhite,
        fontSize = 27.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
    )
    if (nextWordReady) {
        Spacer(modifier = Modifier.height(12.dp))
        SpellingActionButton(
            label = stringResource(R.string.speak_spell_next_word),
            onClick = onNextWordClick,
            primary = true,
        )
    }
}

@Composable
private fun IncorrectContent() {
    Text(text = "🌟", fontSize = 32.sp)
    Text(
        text = stringResource(R.string.speak_spell_almost_message),
        color = SnowWhite,
        fontSize = 23.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SessionCompleteContent(
    tabletLayout: Boolean,
    onSpellMoreClick: () -> Unit,
    onPickAnotherLevelClick: () -> Unit,
    onBackToGamesClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "🎉  ⭐  🎉", fontSize = if (tabletLayout) 64.sp else 44.sp)
        Text(
            text = stringResource(R.string.speak_spell_session_complete),
            color = SnowWhite,
            fontSize = if (tabletLayout) 34.sp else 27.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(18.dp))
        SpellingActionButton(
            label = stringResource(R.string.speak_spell_more_words),
            onClick = onSpellMoreClick,
            primary = true,
        )
        Spacer(modifier = Modifier.height(9.dp))
        SpellingActionButton(
            label = stringResource(R.string.speak_spell_pick_another_level),
            onClick = onPickAnotherLevelClick,
        )
        Spacer(modifier = Modifier.height(9.dp))
        SpellingActionButton(
            label = stringResource(R.string.back_to_games),
            onClick = onBackToGamesClick,
        )
    }
}

@Composable
private fun SpellingActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = label,
    enabled: Boolean = true,
    primary: Boolean = false,
    compact: Boolean = false,
) {
    val buttonModifier = modifier
        .heightIn(min = 54.dp)
        .sizeIn(minWidth = if (compact) 0.dp else 170.dp)
        .clearAndSetSemantics { contentDescription = description }
    if (primary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SnowWhite,
                contentColor = DeepIndigo,
            ),
        ) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = SnowWhite,
                disabledContentColor = SnowWhite.copy(alpha = 0.42f),
            ),
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val INCORRECT_ANSWER_PAUSE_MILLIS = 1_350L
private const val CORRECT_CELEBRATION_PAUSE_MILLIS = 900L
