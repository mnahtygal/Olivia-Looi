package com.nahtygal.olivialooi.ui.games.tracing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.games.tracing.LETTER_GUIDES
import com.nahtygal.olivialooi.games.tracing.LetterVocabulary
import com.nahtygal.olivialooi.games.tracing.LetterTracingEngine
import com.nahtygal.olivialooi.games.tracing.LetterTracingState
import com.nahtygal.olivialooi.games.tracing.LetterTracingStateCodec
import com.nahtygal.olivialooi.games.tracing.TracingPoint
import com.nahtygal.olivialooi.games.tracing.TracingStroke
import com.nahtygal.olivialooi.games.tracing.TracingMode
import com.nahtygal.olivialooi.games.tracing.WordCatalog
import com.nahtygal.olivialooi.games.tracing.WordTracingEngine
import com.nahtygal.olivialooi.games.tracing.WordTracingState
import com.nahtygal.olivialooi.games.tracing.WordTracingStateCodec
import com.nahtygal.olivialooi.games.tracing.WordTracingSpeech
import com.nahtygal.olivialooi.speech.AndroidTextToSpeech
import com.nahtygal.olivialooi.ui.LocalKidProfile
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.FrostBlue
import com.nahtygal.olivialooi.ui.theme.SnowWhite

private val LetterTracingSaver = Saver<LetterTracingState, String>(
    save = { state -> LetterTracingStateCodec.encode(state) },
    restore = LetterTracingStateCodec::decode,
)

private val WordTracingSaver = Saver<WordTracingState, String>(
    save = { state -> WordTracingStateCodec.encode(state) },
    restore = WordTracingStateCodec::decode,
)

private enum class WritingPage {
    MENU, NAME, ALPHABET, LOWERCASE, WORD_CATEGORIES, WORD_PRACTICE,
    NAME_CELEBRATION, ALPHABET_CELEBRATION, LOWERCASE_CELEBRATION, WORD_CELEBRATION,
}

@Composable
fun LetterTracingScreen(
    onGamesClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = LocalKidProfile.current
    val tracingName = remember(profile) { LetterTracingEngine.resolveName(profile.displayName) }
    if (tracingName == null) {
        TracingUnavailable(
            onHomeClick = onHomeClick,
            onBack = onGamesClick,
            modifier = modifier,
        )
        return
    }
    val context = LocalContext.current
    val speech = remember(context) { AndroidTextToSpeech(context.applicationContext) }
    var pageName by rememberSaveable(profile.stableId) { mutableStateOf(WritingPage.MENU.name) }
    var nameState by rememberSaveable(profile.stableId, "name", stateSaver = LetterTracingSaver) {
        mutableStateOf(LetterTracingEngine.newNameSession(tracingName.letters))
    }
    var alphabetState by rememberSaveable(profile.stableId, "alphabet", stateSaver = LetterTracingSaver) {
        mutableStateOf(LetterTracingEngine.newAlphabetSession())
    }
    var lowercaseState by rememberSaveable(profile.stableId, "lowercase", stateSaver = LetterTracingSaver) {
        mutableStateOf(LetterTracingEngine.newLowercaseSession())
    }
    var threeLetterState by rememberSaveable(profile.stableId, WordCatalog.THREE_LETTER_ID, stateSaver = WordTracingSaver) {
        mutableStateOf(WordTracingEngine.newSession(profile.stableId, WordCatalog.THREE_LETTER_ID))
    }
    var fourLetterState by rememberSaveable(profile.stableId, WordCatalog.FOUR_LETTER_ID, stateSaver = WordTracingSaver) {
        mutableStateOf(WordTracingEngine.newSession(profile.stableId, WordCatalog.FOUR_LETTER_ID))
    }
    var fiveLetterState by rememberSaveable(profile.stableId, WordCatalog.FIVE_LETTER_ID, stateSaver = WordTracingSaver) {
        mutableStateOf(WordTracingEngine.newSession(profile.stableId, WordCatalog.FIVE_LETTER_ID))
    }
    var activeWordCategoryId by rememberSaveable(profile.stableId, "active-word-category") {
        mutableStateOf(WordCatalog.THREE_LETTER_ID)
    }
    val page = enumValueOf<WritingPage>(pageName)
    val alphabetIntroduction = stringResource(R.string.tracing_alphabet_introduction)
    val lowercaseIntroduction = stringResource(R.string.tracing_lowercase_introduction)
    val nameCelebrationSpeech = stringResource(
        R.string.tracing_celebration_speech,
        nameState.letters.joinToString("-"),
        tracingName.displayName,
    )
    val alphabetCelebrationSpeech = stringResource(
        R.string.tracing_alphabet_celebration_speech,
        tracingName.displayName,
    )
    val lowercaseCelebrationSpeech = stringResource(
        R.string.tracing_lowercase_celebration_speech,
        tracingName.displayName,
    )

    DisposableEffect(speech) { onDispose { speech.close() } }
    fun activeWordState(): WordTracingState = when (activeWordCategoryId) {
        WordCatalog.FOUR_LETTER_ID -> fourLetterState
        WordCatalog.FIVE_LETTER_ID -> fiveLetterState
        else -> threeLetterState
    }
    fun updateActiveWordState(state: WordTracingState) {
        when (state.categoryId) {
            WordCatalog.THREE_LETTER_ID -> threeLetterState = state
            WordCatalog.FOUR_LETTER_ID -> fourLetterState = state
            WordCatalog.FIVE_LETTER_ID -> fiveLetterState = state
        }
    }

    BackHandler(enabled = page != WritingPage.MENU) {
        pageName = when (page) {
            WritingPage.WORD_PRACTICE, WritingPage.WORD_CELEBRATION -> WritingPage.WORD_CATEGORIES
            WritingPage.WORD_CATEGORIES -> WritingPage.MENU
            else -> WritingPage.MENU
        }.name
    }

    when (page) {
        WritingPage.MENU -> WritingModeMenu(
            onName = {
                pageName = WritingPage.NAME.name
            },
            onAlphabet = {
                pageName = WritingPage.ALPHABET.name
                if (alphabetState.letterPosition == 0 && alphabetState.strokesByPosition.all(List<TracingStroke>::isEmpty)) {
                    speech.speak(alphabetIntroduction)
                }
            },
            onLowercase = {
                pageName = WritingPage.LOWERCASE.name
                if (lowercaseState.letterPosition == 0 && lowercaseState.strokesByPosition.all(List<TracingStroke>::isEmpty)) {
                    speech.speak(lowercaseIntroduction)
                }
            },
            onWords = { pageName = WritingPage.WORD_CATEGORIES.name },
            onGamesClick = onGamesClick,
            onHomeClick = onHomeClick,
            modifier = modifier,
        )
        WritingPage.NAME -> TracingPracticeScreen(
            state = nameState,
            speech = speech,
            onStateChange = { nameState = it },
            onComplete = {
                pageName = WritingPage.NAME_CELEBRATION.name
                speech.speak(nameCelebrationSpeech)
            },
            onWritingMenu = { pageName = WritingPage.MENU.name },
            onHomeClick = onHomeClick,
            modifier = modifier,
        )
        WritingPage.ALPHABET -> TracingPracticeScreen(
            state = alphabetState,
            speech = speech,
            onStateChange = { alphabetState = it },
            onComplete = {
                pageName = WritingPage.ALPHABET_CELEBRATION.name
                speech.speak(alphabetCelebrationSpeech)
            },
            onWritingMenu = { pageName = WritingPage.MENU.name },
            onHomeClick = onHomeClick,
            modifier = modifier,
        )
        WritingPage.LOWERCASE -> TracingPracticeScreen(
            state = lowercaseState,
            speech = speech,
            onStateChange = { lowercaseState = it },
            onComplete = {
                pageName = WritingPage.LOWERCASE_CELEBRATION.name
                speech.speak(lowercaseCelebrationSpeech)
            },
            onWritingMenu = { pageName = WritingPage.MENU.name },
            onHomeClick = onHomeClick,
            modifier = modifier,
        )
        WritingPage.WORD_CATEGORIES -> WordCategoryMenu(
            onCategory = { categoryId ->
                activeWordCategoryId = categoryId
                pageName = WritingPage.WORD_PRACTICE.name
            },
            onWritingMenu = { pageName = WritingPage.MENU.name },
            onHomeClick = onHomeClick,
            modifier = modifier,
        )
        WritingPage.WORD_PRACTICE -> WordPracticeScreen(
            state = activeWordState(),
            speech = speech,
            onStateChange = ::updateActiveWordState,
            onComplete = { completedWord ->
                pageName = WritingPage.WORD_CELEBRATION.name
                speech.speak(WordTracingSpeech.completion(tracingName.displayName, completedWord))
            },
            onCategories = { pageName = WritingPage.WORD_CATEGORIES.name },
            onHomeClick = onHomeClick,
            modifier = modifier,
        )
        WritingPage.NAME_CELEBRATION -> NameTracingCelebration(
            displayName = tracingName.displayName,
            onPracticeAgain = {
                nameState = LetterTracingEngine.practiceAgain(nameState)
                pageName = WritingPage.NAME.name
            },
            onPracticeAlphabet = {
                alphabetState = LetterTracingEngine.newAlphabetSession()
                pageName = WritingPage.ALPHABET.name
                speech.speak(alphabetIntroduction)
            },
            onHomeClick = onHomeClick,
            onBack = { pageName = WritingPage.MENU.name },
            modifier = modifier,
        )
        WritingPage.ALPHABET_CELEBRATION -> AlphabetTracingCelebration(
            displayName = tracingName.displayName,
            onPracticeAgain = {
                alphabetState = LetterTracingEngine.practiceAgain(alphabetState)
                pageName = WritingPage.ALPHABET.name
                speech.speak(alphabetIntroduction)
            },
            onPracticeName = {
                nameState = LetterTracingEngine.newNameSession(tracingName.letters)
                pageName = WritingPage.NAME.name
            },
            onWritingMenu = { pageName = WritingPage.MENU.name },
            onHomeClick = onHomeClick,
            onBack = { pageName = WritingPage.MENU.name },
            modifier = modifier,
        )
        WritingPage.LOWERCASE_CELEBRATION -> LowercaseTracingCelebration(
            displayName = tracingName.displayName,
            onPracticeAgain = {
                lowercaseState = LetterTracingEngine.practiceAgain(lowercaseState)
                pageName = WritingPage.LOWERCASE.name
                speech.speak(lowercaseIntroduction)
            },
            onPracticeAlphabet = { pageName = WritingPage.ALPHABET.name },
            onPracticeName = { pageName = WritingPage.NAME.name },
            onWritingMenu = { pageName = WritingPage.MENU.name },
            onHomeClick = onHomeClick,
            onBack = { pageName = WritingPage.MENU.name },
            modifier = modifier,
        )
        WritingPage.WORD_CELEBRATION -> {
            val wordState = activeWordState()
            WordTracingCelebration(
                state = wordState,
                displayName = tracingName.displayName,
                onNextWord = {
                    updateActiveWordState(WordTracingEngine.nextWord(wordState))
                    pageName = WritingPage.WORD_PRACTICE.name
                },
                onPracticeAgain = {
                    updateActiveWordState(WordTracingEngine.practiceWordAgain(wordState))
                    pageName = WritingPage.WORD_PRACTICE.name
                },
                onCategories = { pageName = WritingPage.WORD_CATEGORIES.name },
                onHomeClick = onHomeClick,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun TracingPracticeScreen(
    state: LetterTracingState,
    speech: AndroidTextToSpeech,
    onStateChange: (LetterTracingState) -> Unit,
    onComplete: () -> Unit,
    onWritingMenu: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activePoints = remember(state.mode) { mutableStateListOf<TracingPoint>() }
    var encouragement by remember(state.mode) { mutableStateOf<String?>(null) }
    val letterSpeech = LetterVocabulary.hearLetterPhrase(state.mode, state.currentLetter)
    val encouragementMessages = listOf(
        stringResource(R.string.tracing_encouragement_great),
        stringResource(R.string.tracing_encouragement_wonderful),
        stringResource(R.string.tracing_encouragement_keep_going),
        stringResource(R.string.tracing_encouragement_nice),
    )

    WinterGamesBackground(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val tablet = maxWidth >= 600.dp
            val compactHeight = maxHeight < 620.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("tracingPracticeViewport")
                    .then(if (compactHeight) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .statusBarsPadding()
                    .padding(horizontal = if (tablet) 24.dp else 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WritingTopNavigation(onHomeClick = onHomeClick, onBack = onWritingMenu)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(when (state.mode) {
                        TracingMode.NAME -> R.string.tracing_my_name
                        TracingMode.ALPHABET -> R.string.tracing_uppercase_abcs
                        TracingMode.LOWERCASE -> R.string.tracing_lowercase_abcs
                    }),
                    color = SnowWhite,
                    fontSize = if (tablet) 36.sp else 29.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = if (state.mode != TracingMode.NAME) {
                        stringResource(R.string.tracing_alphabet_progress, state.letterPosition + 1, state.letters.size)
                    } else {
                        stringResource(
                            R.string.tracing_progress,
                            state.currentLetter.toString(),
                            state.letterPosition + 1,
                            state.letters.size,
                        )
                    },
                    color = SnowWhite.copy(alpha = .92f),
                    fontSize = if (tablet) 20.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                encouragement?.let {
                    Text(it, color = SnowWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                TracingCanvas(
                    state = state,
                    activePoints = activePoints,
                    onStrokeFinished = { points ->
                        val completedBefore = state.strokesByPosition.count(List<TracingStroke>::isNotEmpty)
                        val updated = LetterTracingEngine.addStroke(state, points)
                        if (updated.celebrationPending) {
                            onStateChange(LetterTracingEngine.consumeCelebration(updated))
                            onComplete()
                        } else {
                            onStateChange(updated)
                            val completedAfter = updated.strokesByPosition.count(List<TracingStroke>::isNotEmpty)
                            val milestoneIndex = listOf(5, 10, 15, 20).indexOf(completedAfter)
                            if (
                                state.mode != TracingMode.NAME &&
                                completedAfter > completedBefore && milestoneIndex >= 0
                            ) {
                                encouragement = encouragementMessages[milestoneIndex]
                                speech.speak(encouragementMessages[milestoneIndex])
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = if (tablet) 560.dp else 440.dp)
                        .then(
                            if (compactHeight) Modifier.height(300.dp)
                            else Modifier.weight(1f),
                        ),
                )
                Spacer(Modifier.height(12.dp))
                TracingControls(
                    state = state,
                    onHear = { speech.speak(letterSpeech) },
                    onUndo = { onStateChange(LetterTracingEngine.undo(state)) },
                    onClear = {
                        activePoints.clear()
                        onStateChange(LetterTracingEngine.clear(state))
                    },
                    onPrevious = {
                        activePoints.clear()
                        encouragement = null
                        onStateChange(LetterTracingEngine.previous(state))
                    },
                    onNext = {
                        activePoints.clear()
                        encouragement = null
                        onStateChange(LetterTracingEngine.next(state))
                    },
                    modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                )
            }
        }
    }
}

@Composable
private fun WritingModeMenu(
    onName: () -> Unit,
    onAlphabet: () -> Unit,
    onLowercase: () -> Unit,
    onWords: () -> Unit,
    onGamesClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WinterGamesBackground(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            WritingTopNavigation(onHomeClick = onHomeClick, onBack = onGamesClick)
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.tracing_writing_menu_title),
                color = SnowWhite,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            WritingModeButton(
                label = stringResource(R.string.tracing_trace_my_name),
                symbol = "✎",
                onClick = onName,
            )
            Spacer(Modifier.height(14.dp))
            WritingModeButton(
                label = stringResource(R.string.tracing_trace_uppercase),
                symbol = "ABC",
                onClick = onAlphabet,
            )
            Spacer(Modifier.height(14.dp))
            WritingModeButton(
                label = stringResource(R.string.tracing_trace_lowercase),
                symbol = "abc",
                onClick = onLowercase,
            )
            Spacer(Modifier.height(14.dp))
            WritingModeButton(
                label = stringResource(R.string.tracing_trace_words),
                symbol = "CAT",
                onClick = onWords,
            )
            Spacer(Modifier.height(20.dp))
            TraceButton(stringResource(R.string.back_to_games), onGamesClick, true, Modifier.widthIn(max = 560.dp).fillMaxWidth())
        }
    }
}

@Composable
private fun WritingModeButton(label: String, symbol: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).heightIn(min = 112.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SnowWhite.copy(alpha = .96f), contentColor = DeepIndigo),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(symbol, fontSize = 31.sp, fontWeight = FontWeight.Black)
            Text(label, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun WritingTopNavigation(
    onHomeClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().widthIn(max = 560.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onHomeClick,
            modifier = Modifier.weight(1f).heightIn(min = 58.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SnowWhite.copy(alpha = .97f),
                contentColor = DeepIndigo,
            ),
            shape = RoundedCornerShape(20.dp),
        ) {
            Icon(
                imageVector = WritingHomeVector,
                contentDescription = null,
                modifier = Modifier.size(24.dp).testTag("writingHomeIcon"),
            )
            Spacer(Modifier.widthIn(min = 8.dp))
            Text(stringResource(R.string.home_action), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
        Button(
            onClick = onBack,
            modifier = Modifier.weight(1f).heightIn(min = 58.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SnowWhite.copy(alpha = .97f),
                contentColor = DeepIndigo,
            ),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text("←", fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.widthIn(min = 6.dp))
            Text(stringResource(R.string.tracing_back), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private val WritingHomeVector: ImageVector = ImageVector.Builder(
    name = "WritingHome",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2.7f)
        lineTo(2.5f, 10.5f)
        lineTo(4.2f, 12.6f)
        lineTo(5.8f, 11.3f)
        lineTo(5.8f, 21f)
        lineTo(10.2f, 21f)
        lineTo(10.2f, 15.1f)
        lineTo(13.8f, 15.1f)
        lineTo(13.8f, 21f)
        lineTo(18.2f, 21f)
        lineTo(18.2f, 11.3f)
        lineTo(19.8f, 12.6f)
        lineTo(21.5f, 10.5f)
        close()
    }
}.build()

@Composable
internal fun TracingCanvas(
    state: LetterTracingState,
    activePoints: MutableList<TracingPoint>,
    onStrokeFinished: (List<TracingPoint>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.tracing_canvas_description, state.currentLetter.toString())
    val currentOnStrokeFinished by rememberUpdatedState(onStrokeFinished)
    Canvas(
        modifier = modifier
            .testTag("letterTracingCanvas")
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFFFFFEFF))
            .border(4.dp, FrostBlue, RoundedCornerShape(26.dp))
            .semantics {
                contentDescription = description
                stateDescription = "${state.currentStrokes.size} completed strokes"
            }
            .pointerInput(state.letterPosition) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    activePoints.clear()
                    down.position.normalized(size.width, size.height)?.let(activePoints::add)
                    down.consume()
                    var pointer = down
                    while (pointer.pressed) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (pointer.pressed) {
                            pointer.position.normalized(size.width, size.height)?.let { point ->
                                if (activePoints.lastOrNull()?.distanceSquared(point) ?: 1f >= .000025f) {
                                    activePoints.add(point)
                                }
                            }
                        }
                        pointer.consume()
                    }
                    if (activePoints.isNotEmpty()) currentOnStrokeFinished(activePoints.toList())
                    activePoints.clear()
                }
            },
    ) {
        drawGuide(state.currentLetter)
        state.currentStrokes.forEach(::drawTracingStroke)
        if (activePoints.isNotEmpty()) drawTracingStroke(TracingStroke(activePoints))
    }
}

@Composable
private fun TracingControls(
    state: LetterTracingState,
    onHear: () -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.background(SnowWhite.copy(alpha = .95f), RoundedCornerShape(24.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Button(
            onClick = onHear,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
            shape = RoundedCornerShape(20.dp),
        ) { Text(stringResource(R.string.tracing_hear_letter), fontWeight = FontWeight.ExtraBold) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TraceButton(stringResource(R.string.coloring_undo), onUndo, state.currentStrokes.isNotEmpty(), Modifier.weight(1f))
            TraceButton(stringResource(R.string.coloring_clear), onClear, state.currentStrokes.isNotEmpty(), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TraceButton(stringResource(R.string.abc_previous), onPrevious, state.letterPosition > 0, Modifier.weight(1f))
            TraceButton(stringResource(R.string.abc_next), onNext, state.letterPosition < state.letters.lastIndex, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun TraceButton(label: String, onClick: () -> Unit, enabled: Boolean, modifier: Modifier) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 54.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepIndigo),
    ) { Text(label, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center) }
}

private fun DrawScope.drawGuide(letter: Char) {
    val guide = checkNotNull(LETTER_GUIDES[letter])
    // Preschool handwriting guide: thin, clearly separated dashes.
    val width = size.minDimension * .012f
    val effect = PathEffect.dashPathEffect(
        floatArrayOf(width * 3.5f, width * 3.0f),
        0f,
    )
    guide.paths.forEach { points ->
        if (points.size < 2) return@forEach
        val path = Path().apply {
            moveTo(points.first().x * size.width, points.first().y * size.height)
            points.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
        }
        drawPath(
            path,
            Color(0xFFB7BAC3),
            style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = effect),
        )
    }
}

@Composable
private fun NameTracingCelebration(
    displayName: String,
    onPracticeAgain: () -> Unit,
    onPracticeAlphabet: () -> Unit,
    onHomeClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CelebrationFrame(onHomeClick = onHomeClick, onBack = onBack, modifier = modifier) {
        Text(
            text = stringResource(R.string.tracing_celebration_title, displayName),
            color = DeepIndigo,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        CelebrationMessage(stringResource(R.string.tracing_celebration_message))
        Spacer(Modifier.height(22.dp))
        CelebrationButton(stringResource(R.string.tracing_practice_again), onPracticeAgain)
        Spacer(Modifier.height(10.dp))
        CelebrationButton(stringResource(R.string.tracing_practice_abcs), onPracticeAlphabet)
    }
}

@Composable
private fun AlphabetTracingCelebration(
    displayName: String,
    onPracticeAgain: () -> Unit,
    onPracticeName: () -> Unit,
    onWritingMenu: () -> Unit,
    onHomeClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CelebrationFrame(onHomeClick = onHomeClick, onBack = onBack, modifier = modifier) {
        Text(
            text = stringResource(R.string.tracing_alphabet_celebration_title, displayName),
            color = DeepIndigo,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        CelebrationMessage(stringResource(R.string.tracing_alphabet_celebration_message))
        Spacer(Modifier.height(22.dp))
        CelebrationButton(stringResource(R.string.tracing_practice_abcs_again), onPracticeAgain)
        Spacer(Modifier.height(10.dp))
        CelebrationButton(stringResource(R.string.tracing_practice_my_name), onPracticeName)
        Spacer(Modifier.height(10.dp))
        TraceButton(stringResource(R.string.tracing_back_to_writing_menu), onWritingMenu, true, Modifier.fillMaxWidth())
    }
}

@Composable
private fun LowercaseTracingCelebration(
    displayName: String,
    onPracticeAgain: () -> Unit,
    onPracticeAlphabet: () -> Unit,
    onPracticeName: () -> Unit,
    onWritingMenu: () -> Unit,
    onHomeClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CelebrationFrame(onHomeClick = onHomeClick, onBack = onBack, modifier = modifier) {
        Text(
            text = stringResource(R.string.tracing_lowercase_celebration_title, displayName),
            color = DeepIndigo,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        CelebrationMessage(stringResource(R.string.tracing_lowercase_celebration_message))
        Spacer(Modifier.height(22.dp))
        CelebrationButton(stringResource(R.string.tracing_practice_lowercase_again), onPracticeAgain)
        Spacer(Modifier.height(10.dp))
        CelebrationButton(stringResource(R.string.tracing_uppercase_abcs), onPracticeAlphabet)
        Spacer(Modifier.height(10.dp))
        CelebrationButton(stringResource(R.string.tracing_practice_my_name), onPracticeName)
        Spacer(Modifier.height(10.dp))
        TraceButton(stringResource(R.string.tracing_back_to_writing_menu), onWritingMenu, true, Modifier.fillMaxWidth())
    }
}

@Composable
internal fun CelebrationFrame(
    onHomeClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    WinterGamesBackground(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            WritingTopNavigation(onHomeClick = onHomeClick, onBack = onBack)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "★  ✦  ★  ✧  ★",
                color = Color(0xFFFFD54F),
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .background(SnowWhite.copy(alpha = .96f), RoundedCornerShape(30.dp))
                    .padding(24.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "✦  ★  ✧  ★  ✦",
                color = Color(0xFFFF8AB8),
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun CelebrationMessage(message: String) {
    Text(
        text = message,
        color = DeepIndigo.copy(alpha = .82f),
        fontSize = 20.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}

@Composable
internal fun CelebrationButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
        shape = RoundedCornerShape(22.dp),
    ) { Text(label, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center) }
}

@Composable
private fun TracingUnavailable(
    onHomeClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WinterGamesBackground(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            WritingTopNavigation(onHomeClick = onHomeClick, onBack = onBack)
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.tracing_name_unavailable),
                color = SnowWhite,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun DrawScope.drawTracingStroke(stroke: TracingStroke) {
    if (stroke.points.isEmpty()) return
    val width = size.minDimension * .052f
    if (stroke.points.size == 1) {
        val point = stroke.points.first()
        drawCircle(Color(0xFF7558C8), width / 2f, Offset(point.x * size.width, point.y * size.height))
        return
    }
    val path = Path().apply {
        val first = stroke.points.first()
        moveTo(first.x * size.width, first.y * size.height)
        for (index in 1 until stroke.points.lastIndex) {
            val current = stroke.points[index]
            val next = stroke.points[index + 1]
            quadraticTo(
                current.x * size.width,
                current.y * size.height,
                (current.x + next.x) * size.width / 2f,
                (current.y + next.y) * size.height / 2f,
            )
        }
        val last = stroke.points.last()
        lineTo(last.x * size.width, last.y * size.height)
    }
    drawPath(path, Color(0xFF7558C8), style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun Offset.normalized(width: Int, height: Int): TracingPoint? {
    if (width <= 0 || height <= 0 || !x.isFinite() || !y.isFinite() || x !in 0f..width.toFloat() || y !in 0f..height.toFloat()) return null
    return TracingPoint(x / width, y / height)
}

private fun TracingPoint.distanceSquared(other: TracingPoint): Float {
    val dx = x - other.x
    val dy = y - other.y
    return dx * dx + dy * dy
}
