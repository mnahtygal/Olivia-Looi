package com.nahtygal.olivialooi.ui.games.piano

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.audio.LocalPianoAudioPlayer
import com.nahtygal.olivialooi.audio.PianoAudioStatus
import com.nahtygal.olivialooi.games.piano.PianoCatalog
import com.nahtygal.olivialooi.games.piano.PianoEngine
import com.nahtygal.olivialooi.games.piano.PianoMode
import com.nahtygal.olivialooi.games.piano.PianoNote
import com.nahtygal.olivialooi.games.piano.PianoPointers
import com.nahtygal.olivialooi.games.piano.PianoState
import com.nahtygal.olivialooi.games.piano.PianoStateCodec
import com.nahtygal.olivialooi.games.piano.pianoKeyAt
import com.nahtygal.olivialooi.speech.AndroidTextToSpeech
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.SnowWhite
import kotlinx.coroutines.delay

private val PianoSaver = Saver<PianoState, String>(save = { PianoStateCodec.encode(it) }, restore = PianoStateCodec::decode)

@Composable
fun PianoScreen(onGamesClick: () -> Unit) {
    var state by rememberSaveable(stateSaver = PianoSaver) { mutableStateOf(PianoState()) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var audioStatus by remember { mutableStateOf(PianoAudioStatus.LOADING) }
    val audio = remember(context) { LocalPianoAudioPlayer(context.applicationContext) { audioStatus = it } }
    val speech = remember(context) { AndroidTextToSpeech(context.applicationContext) }
    var foreground by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var inputEpoch by remember { mutableIntStateOf(0) }
    var noteRequest by remember { mutableIntStateOf(0) }
    var pendingNote by remember { mutableStateOf<PianoNote?>(null) }
    val opening = stringResource(R.string.piano_opening)
    val completion = stringResource(R.string.piano_completion_speech)

    DisposableEffect(audio, speech, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { foreground = true; audio.resume() }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    foreground = false
                    inputEpoch += 1
                    pendingNote = null
                    audio.suspend()
                    speech.stop()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            audio.close()
            speech.close()
        }
    }
    LaunchedEffect(foreground) {
        if (foreground && !state.openingSpoken) {
            state = state.copy(openingSpoken = true)
            speech.speak(opening)
        }
    }
    LaunchedEffect(state.isComplete, foreground) {
        if (foreground && state.isComplete && !state.completionSpoken) {
            state = PianoEngine.acknowledgeCompletion(state)
            pendingNote = null
            speech.speak(completion)
        }
    }
    // Optional spoken names wait for a pause in playing; rapid input never floods TTS.
    LaunchedEffect(noteRequest, state.sayNotes, foreground) {
        if (state.sayNotes && foreground && pendingNote != null) {
            delay(450)
            if (!state.isComplete) pendingNote?.let { speech.speak(it.displayLabel) }
        }
    }

    fun press(note: PianoNote) {
        if (!foreground || audioStatus == PianoAudioStatus.LOADING) return
        audio.play(note)
        state = PianoEngine.press(state, note)
        if (state.sayNotes) { pendingNote = note; noteRequest += 1 }
    }
    fun changeMode(songs: Boolean) {
        inputEpoch += 1
        pendingNote = null
        speech.stop()
        audio.stopAll()
        state = if (songs) PianoEngine.chooseSongs(state) else PianoEngine.freePlay(state)
    }
    fun startSong(restart: Boolean) {
        inputEpoch += 1
        pendingNote = null
        audio.stopAll()
        state = if (restart) PianoEngine.restartSong(state) else PianoEngine.startSong(state)
        state.song?.let { speech.speak("Let’s play ${it.title}!") }
    }

    WinterGamesBackground(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val tablet = maxWidth >= 600.dp
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .statusBarsPadding().navigationBarsPadding().padding(horizontal = if (tablet) 20.dp else 8.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.piano_name), color = SnowWhite, fontSize = if (tablet) 38.sp else 30.sp, fontWeight = FontWeight.ExtraBold)
                Row(Modifier.widthIn(max = 680.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PianoAction(stringResource(R.string.piano_free_play), Modifier.weight(1f), chosen = state.mode == PianoMode.FREE_PLAY) { changeMode(false) }
                    PianoAction(stringResource(R.string.piano_play_song), Modifier.weight(1f), chosen = state.mode == PianoMode.SONGS) { changeMode(true) }
                }
                if (state.mode == PianoMode.SONGS && state.song == null) {
                    Text(stringResource(R.string.piano_choose_song), color = SnowWhite, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    PianoCatalog.songs.forEach { song ->
                        PianoAction(song.title, Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                            inputEpoch += 1
                            state = PianoEngine.selectSong(state, song.stableId)
                        }
                    }
                } else if (state.song != null) {
                    Text(state.song!!.title, color = SnowWhite, fontSize = if (tablet) 27.sp else 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    if (state.isComplete) {
                        Text(stringResource(R.string.piano_complete), Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = Color(0xFFFFE5A3), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    } else if (state.songStarted) {
                        val next = stringResource(R.string.piano_next_note, state.expectedNote!!.displayLabel)
                        Text(next, color = SnowWhite, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text(pluralStringResource(R.plurals.piano_progress, state.position, state.position), color = SnowWhite, fontSize = 16.sp)
                    }
                    Row(Modifier.widthIn(max = 680.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PianoAction(
                            stringResource(if (state.songStarted) R.string.piano_restart_song else R.string.piano_start_song), Modifier.weight(1f),
                        ) { startSong(state.songStarted) }
                        PianoAction(stringResource(R.string.piano_exit_song), Modifier.weight(1f)) { changeMode(false) }
                    }
                }
                when (audioStatus) {
                    PianoAudioStatus.LOADING -> Text(stringResource(R.string.piano_loading), color = SnowWhite, fontSize = 18.sp)
                    PianoAudioStatus.UNAVAILABLE -> Text(stringResource(R.string.piano_audio_unavailable), color = SnowWhite, fontSize = 18.sp, textAlign = TextAlign.Center)
                    PianoAudioStatus.READY -> Unit
                }
                PianoKeyboard(
                    expected = state.expectedNote,
                    enabled = foreground && audioStatus != PianoAudioStatus.LOADING,
                    inputEpoch = inputEpoch,
                    onPress = ::press,
                    modifier = Modifier.widthIn(max = 1200.dp).fillMaxWidth().height(if (tablet) 320.dp else 250.dp),
                )
                PianoAction(
                    stringResource(if (state.sayNotes) R.string.piano_say_notes_on else R.string.piano_say_notes_off),
                    chosen = state.sayNotes,
                ) {
                    state = state.copy(sayNotes = !state.sayNotes)
                    pendingNote = null
                    speech.stop()
                }
                PianoAction(stringResource(R.string.back_to_games), onClick = onGamesClick)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun PianoAction(label: String, modifier: Modifier = Modifier, chosen: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp).semantics { selected = chosen },
        colors = ButtonDefaults.buttonColors(containerColor = SnowWhite, contentColor = DeepIndigo),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(if (chosen) 3.dp else 1.dp, DeepIndigo),
    ) { Text(if (chosen) "✓ $label" else label, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
}

@Composable
private fun PianoKeyboard(expected: PianoNote?, enabled: Boolean, inputEpoch: Int, onPress: (PianoNote) -> Unit, modifier: Modifier) {
    val pointers = remember { PianoPointers() }
    var pressedMask by remember { mutableIntStateOf(0) }
    var accessibleFlash by remember { mutableIntStateOf(-1) }
    var accessibleRequest by remember { mutableIntStateOf(0) }
    val latestPress by rememberUpdatedState(onPress)
    LaunchedEffect(accessibleRequest, inputEpoch, enabled) {
        if (accessibleFlash >= 0) { delay(140); accessibleFlash = -1 }
    }
    // An epoch change (mode/background) cancels the pointer handler and clears held keys.
    DisposableEffect(inputEpoch, enabled) {
        pointers.clear()
        pressedMask = 0
        accessibleFlash = -1
        onDispose { pointers.clear(); pressedMask = 0 }
    }
    Box(modifier.background(Color(0xFF25263C), RoundedCornerShape(18.dp)).padding(6.dp)) {
        Row(
            Modifier.fillMaxSize().pointerInput(enabled, inputEpoch) {
                if (!enabled) return@pointerInput
                val soundOnPress: (PianoNote) -> Unit = { latestPress(it) }
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    try {
                        // Process the whole initial event: a chord may begin in one event.
                        val initial = currentEvent
                        for (i in initial.changes.indices) {
                            val change = initial.changes[i]
                            pointers.update(change.id.value, pianoKeyAt(change.position.x, change.position.y, size.width.toFloat(), size.height.toFloat()), change.pressed, soundOnPress)
                            change.consume()
                        }
                        pressedMask = pointers.pressedMask()
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            for (i in event.changes.indices) {
                                val change = event.changes[i]
                                pointers.update(
                                    change.id.value,
                                    pianoKeyAt(change.position.x, change.position.y, size.width.toFloat(), size.height.toFloat()),
                                    change.pressed,
                                    soundOnPress,
                                )
                                change.consume()
                            }
                            pressedMask = pointers.pressedMask()
                        } while (event.changes.any { it.pressed })
                    } finally {
                        pointers.clear()
                        pressedMask = 0
                    }
                }
            },
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            PianoCatalog.notes.forEachIndexed { index, note ->
                val pressed = pressedMask and (1 shl index) != 0 || accessibleFlash == index
                val expectedKey = note == expected
                val pressedDescription = stringResource(R.string.piano_key_pressed)
                val nextDescription = stringResource(R.string.piano_key_next)
                Card(
                    Modifier.weight(1f).fillMaxSize().graphicsLayer {
                        translationY = if (pressed) 6.dp.toPx() else 0f
                        scaleY = if (pressed) .97f else 1f
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(.5f, 0f)
                    }.semantics(mergeDescendants = true) {
                        contentDescription = note.accessibilityDescription
                        role = Role.Button
                        stateDescription = when {
                            pressed && expectedKey -> "$pressedDescription, $nextDescription"
                            pressed -> pressedDescription
                            expectedKey -> nextDescription
                            else -> ""
                        }
                        if (!enabled) disabled()
                        else onClick {
                            latestPress(note)
                            accessibleFlash = index
                            accessibleRequest += 1
                            true
                        }
                    },
                    shape = RoundedCornerShape(bottomStart = 9.dp, bottomEnd = 9.dp),
                    colors = CardDefaults.cardColors(containerColor = if (pressed) Color(0xFFD7E7FF) else Color.White),
                    border = BorderStroke(if (expectedKey) 4.dp else if (pressed) 3.dp else 1.dp, DeepIndigo),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (pressed) 0.dp else 5.dp),
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text(if (expectedKey) "▼" else if (pressed) "●" else "", color = DeepIndigo, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(note.displayLabel, color = DeepIndigo, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun PianoGameGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRoundRect(Color(0xFF292A43), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()))
        val gap = size.width * .02f
        val keyWidth = (size.width - gap * 9) / 8
        repeat(8) { key ->
            val x = gap + key * (keyWidth + gap)
            drawRoundRect(Color.White, Offset(x, size.height * .16f), Size(keyWidth, size.height * .72f), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
        }
        drawCircle(Color(0xFFAD75D9), size.minDimension * .045f, Offset(size.width * .31f, size.height * .72f))
    }
}
