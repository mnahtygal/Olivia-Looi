package com.nahtygal.olivialooi.ui.games.drums

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
import com.nahtygal.olivialooi.audio.LocalDrumAudioPlayer
import com.nahtygal.olivialooi.audio.DrumAudioStatus
import com.nahtygal.olivialooi.games.drums.DrumCatalog
import com.nahtygal.olivialooi.games.drums.DrumEngine
import com.nahtygal.olivialooi.games.drums.DrumMode
import com.nahtygal.olivialooi.games.drums.DrumSound
import com.nahtygal.olivialooi.games.drums.BeatPhase
import com.nahtygal.olivialooi.games.drums.DrumPointers
import com.nahtygal.olivialooi.games.drums.DrumState
import com.nahtygal.olivialooi.games.drums.DrumStateCodec
import com.nahtygal.olivialooi.games.drums.drumPadAt
import com.nahtygal.olivialooi.speech.AndroidTextToSpeech
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.SnowWhite
import kotlinx.coroutines.delay

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.Stroke
private val DrumSaver = Saver<DrumState, String>(save = { DrumStateCodec.encode(it) }, restore = DrumStateCodec::decode)

@Composable
fun DrumScreen(onGamesClick: () -> Unit) {
    var state by rememberSaveable(stateSaver = DrumSaver) { mutableStateOf(DrumState()) }
    var openingSpoken by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var status by remember { mutableStateOf(DrumAudioStatus.LOADING) }
    val audio = remember(context) { LocalDrumAudioPlayer(context.applicationContext) { status = it } }
    val speech = remember(context) { AndroidTextToSpeech(context.applicationContext) }
    var foreground by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var inputEpoch by remember { mutableIntStateOf(0) }
    var replayPad by remember { mutableIntStateOf(-1) }
    var retry by remember { mutableStateOf(false) }
    val opening = stringResource(R.string.drums_opening)
    val yourTurn = stringResource(R.string.drums_your_turn)
    val almost = stringResource(R.string.drums_retry)
    val great = stringResource(R.string.drums_great_beat)
    val complete = stringResource(R.string.drums_complete)

    DisposableEffect(audio, speech, owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { foreground = true; audio.resume() }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    foreground = false
                    inputEpoch += 1
                    replayPad = -1
                    state = DrumEngine.interruptReplay(state)
                    audio.suspend()
                    speech.stop()
                }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); audio.close(); speech.close() }
    }
    LaunchedEffect(foreground) {
        if (foreground && !openingSpoken) { openingSpoken = true; speech.speak(opening) }
    }
    // Cancellation prevents overlapping replays. Rotation/background resumes the same
    // pattern from its beginning; completion is guarded by the engine's replay identity.
    LaunchedEffect(state.mode, state.phase, state.replayIdentity, foreground, status) {
        if (foreground && status != DrumAudioStatus.LOADING && state.mode == DrumMode.COPY_BEAT && state.phase == BeatPhase.REPLAY) {
            val identity = state.replayIdentity
            val pattern = state.pattern
            speech.stop()
            audio.stopAll()
            try {
                delay(350)
                for (sound in pattern) {
                    replayPad = sound.ordinal
                    audio.play(sound)
                    delay(230)
                    replayPad = -1
                    delay(470)
                }
                val updated = DrumEngine.finishReplay(state, identity)
                if (updated !== state) { state = updated; speech.speak(yourTurn) }
            } finally { replayPad = -1; audio.stopAll() }
        }
    }
    fun hit(sound: DrumSound) {
        if (!foreground || status == DrumAudioStatus.LOADING || state.inputLocked) return
        audio.play(sound)
        if (state.mode == DrumMode.COPY_BEAT) {
            val wrong = sound != state.expected
            state = DrumEngine.hit(state, sound)
            retry = wrong
            if (wrong) speech.speak(almost)
            else if (state.phase == BeatPhase.COMPLETE) speech.speak(if (state.sessionComplete) complete else great)
        }
    }
    fun transition(next: DrumState) {
        if (next === state) return
        inputEpoch += 1
        retry = false
        speech.stop()
        audio.stopAll()
        state = next
    }
    WinterGamesBackground(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val tablet = maxWidth >= 600.dp
            val columns = if (tablet) 3 else 2
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.drums_name), color = SnowWhite, fontSize = if (tablet) 38.sp else 30.sp, fontWeight = FontWeight.ExtraBold)
                Row(Modifier.widthIn(max = 680.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DrumAction(stringResource(R.string.drums_free), Modifier.weight(1f), state.mode == DrumMode.FREE_PLAY) { transition(DrumEngine.freePlay(state)) }
                    DrumAction(stringResource(R.string.drums_copy), Modifier.weight(1f), state.mode == DrumMode.COPY_BEAT) { transition(DrumEngine.copyBeat(state)) }
                }
                if (state.mode == DrumMode.COPY_BEAT) {
                    Text(stringResource(R.string.drums_round, state.round), color = SnowWhite, fontSize = 20.sp)
                    val message = when {
                        state.sessionComplete -> complete
                        state.phase == BeatPhase.COMPLETE -> great
                        state.phase == BeatPhase.REPLAY -> stringResource(R.string.drums_listen)
                        retry -> almost
                        else -> yourTurn
                    }
                    Text(message, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = SnowWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    if (state.phase == BeatPhase.YOUR_TURN) {
                        Text(stringResource(R.string.drums_progress, state.position, state.pattern.size), color = SnowWhite, fontSize = 17.sp)
                    }
                }
                if (status != DrumAudioStatus.READY) {
                    Text(stringResource(if (status == DrumAudioStatus.LOADING) R.string.drums_loading else R.string.drums_unavailable), color = SnowWhite, textAlign = TextAlign.Center)
                }
                DrumPads(
                    columns, foreground && status != DrumAudioStatus.LOADING && !state.inputLocked,
                    inputEpoch, replayPad, ::hit,
                    Modifier.widthIn(max = 1100.dp).fillMaxWidth().height(if (tablet) 410.dp else 510.dp),
                )
                if (state.mode == DrumMode.COPY_BEAT) {
                    when {
                        state.sessionComplete -> DrumAction(stringResource(R.string.drums_more)) { transition(DrumEngine.copyBeat(state)) }
                        state.phase == BeatPhase.COMPLETE -> DrumAction(stringResource(R.string.drums_next)) { transition(DrumEngine.nextBeat(state)) }
                        else -> DrumAction(stringResource(R.string.drums_hear), enabled = state.phase == BeatPhase.YOUR_TURN) { transition(DrumEngine.replay(state)) }
                    }
                }
                DrumAction(stringResource(R.string.back_to_games), onClick = onGamesClick)
            }
        }
    }
}

@Composable
private fun DrumAction(label: String, modifier: Modifier = Modifier, chosen: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, modifier.heightIn(min = 56.dp).semantics { selected = chosen }, enabled = enabled,
        shape = RoundedCornerShape(22.dp), border = BorderStroke(if (chosen) 3.dp else 1.dp, DeepIndigo),
        colors = ButtonDefaults.buttonColors(containerColor = SnowWhite, contentColor = DeepIndigo),
    ) { Text(if (chosen) "✓ $label" else label, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
}

@Composable
private fun DrumPads(columns: Int, enabled: Boolean, inputEpoch: Int, replayPad: Int, onHit: (DrumSound) -> Unit, modifier: Modifier) {
    val pointers = remember { DrumPointers() }
    var pressedMask by remember { mutableIntStateOf(0) }
    var flash by remember { mutableIntStateOf(-1) }
    var flashRequest by remember { mutableIntStateOf(0) }
    val latestHit by rememberUpdatedState(onHit)
    val gap = with(LocalDensity.current) { 12.dp.toPx() }
    LaunchedEffect(flashRequest, inputEpoch, enabled) { if (flash >= 0) { delay(150); flash = -1 } }
    DisposableEffect(enabled, inputEpoch, columns) {
        pointers.clear(); pressedMask = 0; flash = -1
        onDispose { pointers.clear(); pressedMask = 0 }
    }
    Column(
        modifier.pointerInput(enabled, inputEpoch, columns, gap) {
            if (!enabled) return@pointerInput
            val trigger: (DrumSound) -> Unit = { latestHit(it); flash = it.ordinal; flashRequest += 1 }
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                try {
                    var event = currentEvent
                    do {
                        // Stable pointer-ID order gives simultaneous hits a deterministic order
                        // in Copy Beat, without sorting/allocating on each pointer event.
                        var previous = Long.MIN_VALUE
                        var first = true
                        repeat(event.changes.size) {
                            var chosen = -1
                            for (i in event.changes.indices) {
                                val id = event.changes[i].id.value
                                if ((first || id > previous) && (chosen < 0 || id < event.changes[chosen].id.value)) chosen = i
                            }
                            if (chosen >= 0) {
                                val change = event.changes[chosen]
                                previous = change.id.value; first = false
                                pointers.update(change.id.value,
                                    drumPadAt(change.position.x, change.position.y, size.width.toFloat(), size.height.toFloat(), columns, gap),
                                    change.pressed, trigger)
                                change.consume()
                            }
                        }
                        pressedMask = pointers.pressedMask()
                        if (event.changes.none { it.pressed }) break
                        event = awaitPointerEvent(PointerEventPass.Initial)
                    } while (true)
                } finally { pointers.clear(); pressedMask = 0 }
            }
        }, verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(6 / columns) { row ->
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(columns) { column ->
                    val index = row * columns + column
                    val sound = DrumSound.entries[index]
                    val active = pressedMask and (1 shl index) != 0 || flash == index || replayPad == index
                    val pressedText = stringResource(R.string.drums_pressed)
                    Card(
                        Modifier.weight(1f).fillMaxSize().graphicsLayer { scaleX = if (active) .96f else 1f; scaleY = if (active) .96f else 1f }
                            .semantics(mergeDescendants = true) {
                                contentDescription = sound.accessibilityDescription
                                role = Role.Button
                                stateDescription = if (active) pressedText else ""
                                if (!enabled) disabled() else onClick {
                                    latestHit(sound); flash = index; flashRequest += 1; true
                                }
                            },
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = if (active) Color(0xFFFFE7A6) else SnowWhite),
                        border = BorderStroke(if (active) 5.dp else 2.dp, DeepIndigo),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (active) 1.dp else 7.dp),
                    ) {
                        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            DrumIcon(sound, Modifier.weight(1f).fillMaxWidth())
                            Text(sound.displayName, color = DeepIndigo, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

/** Original local instrument glyphs; pad labels and outlines also identify the instrument/state. */
@Composable
private fun DrumIcon(sound: DrumSound, modifier: Modifier) {
    Canvas(modifier) {
        val r = size.minDimension * .34f
        val c = center
        when (sound) {
            DrumSound.KICK, DrumSound.SNARE, DrumSound.TOM -> {
                val color = when (sound) { DrumSound.KICK -> Color(0xFF8355BD); DrumSound.SNARE -> Color(0xFFE8769F); else -> Color(0xFF408CCB) }
                drawOval(color, Offset(c.x-r, c.y-r*.4f), Size(r*2, r*1.4f))
                drawOval(Color(0xFFE2EDFF), Offset(c.x-r, c.y-r*.65f), Size(r*2, r))
                drawOval(DeepIndigo, Offset(c.x-r, c.y-r*.65f), Size(r*2, r), style = Stroke(3.dp.toPx()))
                drawLine(DeepIndigo, Offset(c.x-r*.8f,c.y+r*.8f), Offset(c.x-r*.8f,c.y+r*.1f), 3.dp.toPx())
                drawLine(DeepIndigo, Offset(c.x+r*.8f,c.y+r*.8f), Offset(c.x+r*.8f,c.y+r*.1f), 3.dp.toPx())
                if (sound == DrumSound.SNARE) repeat(3) { drawLine(DeepIndigo, Offset(c.x-r*.5f+it*r*.5f,c.y+r*.3f),Offset(c.x-r*.5f+it*r*.5f,c.y+r*.85f),2.dp.toPx()) }
            }
            DrumSound.CLAP -> {
                repeat(2) { hand ->
                    val x = c.x + (hand * 2 - 1) * r * .43f
                    drawOval(Color(0xFFF2B078), Offset(x-r*.5f,c.y-r*.1f), Size(r, r))
                    repeat(4) { finger -> drawLine(Color(0xFFF2B078), Offset(x-r*.36f+finger*r*.24f,c.y), Offset(x-r*.36f+finger*r*.24f,c.y-r*(.7f+finger%2*.15f)), r*.19f, cap = androidx.compose.ui.graphics.StrokeCap.Round) }
                }
                drawLine(DeepIndigo, Offset(c.x,c.y-r*1.2f), Offset(c.x,c.y-r*.95f), 3.dp.toPx())
            }
            DrumSound.TAMBOURINE -> {
                drawCircle(Color(0xFFDA8A46), r, c, style = Stroke(r*.18f))
                repeat(6) { i ->
                    val angle = i * kotlin.math.PI / 3
                    val p = c + Offset(kotlin.math.cos(angle).toFloat()*r,kotlin.math.sin(angle).toFloat()*r)
                    drawCircle(Color(0xFFBCCADD),r*.22f,p)
                    drawCircle(DeepIndigo,r*.22f,p,style=Stroke(2.dp.toPx()))
                }
            }
            DrumSound.CYMBAL -> {
                drawLine(DeepIndigo,c,Offset(c.x,c.y+r),4.dp.toPx())
                drawOval(Color(0xFFEABD42),Offset(c.x-r*1.15f,c.y-r*.5f),Size(r*2.3f,r))
                drawOval(Color(0xFFAE791E),Offset(c.x-r*.55f,c.y-r*.25f),Size(r*1.1f,r*.5f),style=Stroke(2.dp.toPx()))
                drawCircle(DeepIndigo,r*.08f,c)
            }
        }
    }
}

@Composable
fun DrumGameGlyph(modifier: Modifier = Modifier) = DrumIcon(DrumSound.KICK, modifier)
