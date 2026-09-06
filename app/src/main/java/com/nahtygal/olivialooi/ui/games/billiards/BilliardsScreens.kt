package com.nahtygal.olivialooi.ui.games.billiards

import androidx.compose.ui.graphics.drawscope.clipRect

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
import com.nahtygal.olivialooi.speech.AndroidTextToSpeech
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.SnowWhite
import kotlinx.coroutines.delay

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.Stroke
import android.graphics.Paint
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import com.nahtygal.olivialooi.games.billiards.*
import kotlinx.coroutines.isActive
import kotlin.math.sqrt
import kotlin.math.roundToInt
private fun BilliardsMode.title(): Int = when (this) {
    BilliardsMode.FREE_PLAY -> R.string.billiards_free
    BilliardsMode.VERSUS -> R.string.billiards_versus
    BilliardsMode.TRY_POCKET -> R.string.billiards_try
}
private fun BilliardsMode.subtitle(): Int = when (this) {
    BilliardsMode.FREE_PLAY -> R.string.billiards_free_subtitle
    BilliardsMode.VERSUS -> R.string.billiards_versus_subtitle
    BilliardsMode.TRY_POCKET -> R.string.billiards_try_subtitle
}

@Composable
fun BilliardsModeScreen(onMode: (BilliardsMode) -> Unit, onGamesClick: () -> Unit) {
    WinterGamesBackground(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val tablet = maxWidth >= 600.dp
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.billiards_name), color = SnowWhite, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(R.string.billiards_choose), color = SnowWhite, fontSize = 24.sp)
                if (tablet) {
                    Row(Modifier.widthIn(max = 1000.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        BilliardsMode.entries.forEach { ModeCard(it, { onMode(it) }, Modifier.weight(1f)) }
                    }
                } else BilliardsMode.entries.forEach { ModeCard(it, { onMode(it) }, Modifier.widthIn(max = 450.dp).fillMaxWidth()) }
                PoolAction(stringResource(R.string.back_to_games), onClick = onGamesClick)
            }
        }
    }
}

@Composable
private fun ModeCard(mode: BilliardsMode, onClick: () -> Unit, modifier: Modifier) {
    val title = stringResource(mode.title())
    val subtitle = stringResource(mode.subtitle())
    Card(onClick, modifier.semantics(mergeDescendants = true) { contentDescription = "$title. $subtitle" },
        shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = SnowWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (mode == BilliardsMode.VERSUS) RobotGlyph(Modifier.fillMaxWidth().height(110.dp))
            else BilliardsGameGlyph(Modifier.fillMaxWidth().height(110.dp))
            Text(title, color = DeepIndigo, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text(subtitle, color = DeepIndigo, fontSize = 18.sp, textAlign = TextAlign.Center)
        }
    }
}

private val PoolSaver = Saver<BilliardsState, String>(save = { BilliardsStateCodec.encode(it) }, restore = BilliardsStateCodec::decode)

@Composable
fun BilliardsTableScreen(mode: BilliardsMode, sessionId: Int, onMode: (BilliardsMode) -> Unit, onModesClick: () -> Unit, onGamesClick: () -> Unit) {
    var state by rememberSaveable(mode.name, sessionId, stateSaver = PoolSaver) { mutableStateOf(BilliardsEngine.newGame(mode)) }
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val speech = remember(context) { AndroidTextToSpeech(context.applicationContext) }
    var foreground by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var gestureEpoch by remember { mutableIntStateOf(0) }
    var spokenIdentity by rememberSaveable(mode.name, sessionId) { mutableLongStateOf(-1L) }
    DisposableEffect(owner, speech) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> foreground = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> { foreground = false; gestureEpoch += 1; speech.stop() }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); speech.close() }
    }
    LaunchedEffect(mode, sessionId, state.shotActive, foreground) {
        if (foreground && state.shotActive) {
            var last = withFrameNanos { it }
            while (isActive && state.shotActive) {
                val now = withFrameNanos { it }
                state = BilliardsEngine.tick(state, (now - last) / 1_000_000_000.0)
                last = now
            }
        }
    }
    LaunchedEffect(mode, sessionId, state.turnIdentity, state.turn, state.shotActive, foreground) {
        if (foreground && state.mode == BilliardsMode.VERSUS && state.turn == TurnOwner.LOOLOO && !state.shotActive && !state.sessionComplete) {
            val identity = state.turnIdentity
            state = BilliardsEngine.prepareAi(state, identity)
            delay(1000)
            state = BilliardsEngine.fireAi(state, identity)
        }
    }
    LaunchedEffect(mode, sessionId, state.turnIdentity, foreground) {
        if (foreground && spokenIdentity != state.turnIdentity) {
            spokenIdentity = state.turnIdentity
            val phrase = when {
                state.sessionComplete -> completionText(state)
                state.roundComplete -> "You did it, Olivia!"
                state.mode == BilliardsMode.VERSUS -> if (state.turn == TurnOwner.OLIVIA) "Your turn, Olivia!" else "LooLoo’s turn!"
                state.mode == BilliardsMode.TRY_POCKET && state.outcome == ShotOutcome.WRONG_POCKET -> "Nice shot!"
                state.mode == BilliardsMode.TRY_POCKET -> "Can you roll the ${state.target!!.ballId.number} ball into this pocket?"
                state.outcome == ShotOutcome.POCKET -> "Into the pocket!"
                state.outcome == ShotOutcome.NONE -> "Let’s roll some balls!"
                state.turnIdentity % 3L == 0L -> "Nice roll!"
                else -> null
            }
            phrase?.let { speech.speak(it) }
        }
    }
    fun reset() { gestureEpoch += 1; speech.stop(); state = BilliardsEngine.rack(state) }
    WinterGamesBackground(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val tablet = maxWidth >= 600.dp
            val tableWidth = minOf(maxWidth - 24.dp, ((maxHeight - 190.dp).coerceAtLeast(420.dp)) * (664f / 1064f), 680.dp)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.billiards_name), color = SnowWhite, fontSize = if (tablet) 30.sp else 25.sp, fontWeight = FontWeight.ExtraBold)
                if (state.mode == BilliardsMode.VERSUS) {
                    Text(stringResource(R.string.billiards_ownership, state.oliviaBalls.size, state.looLooBalls.size), color = SnowWhite, fontSize = 18.sp)
                    Text(stringResource(if (state.turn == TurnOwner.OLIVIA) R.string.billiards_olivia_turn else R.string.billiards_looloo_turn), color = SnowWhite, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                }
                if (state.mode == BilliardsMode.TRY_POCKET) {
                    Text(stringResource(R.string.billiards_round, state.round), color = SnowWhite, fontSize = 18.sp)
                    Text(stringResource(R.string.billiards_target, state.target!!.ballId.number.toString()), color = SnowWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                val status = when {
                    state.sessionComplete -> completionText(state)
                    state.roundComplete -> "You did it, Olivia!"
                    state.shotActive -> stringResource(R.string.billiards_rolling)
                    state.mode == BilliardsMode.VERSUS && state.turn == TurnOwner.LOOLOO -> stringResource(R.string.billiards_friendly)
                    state.outcome == ShotOutcome.WRONG_POCKET -> "Nice shot! Try the highlighted pocket."
                    else -> stringResource(R.string.billiards_instructions)
                }
                Text(status, color = SnowWhite, fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                PoolTable(state, foreground, gestureEpoch, Modifier.widthIn(max = tableWidth).fillMaxWidth().aspectRatio(664f / 1064f),
                    onSelect = { state = BilliardsEngine.selectBall(state, it) },
                    onShot = { state = BilliardsEngine.shoot(state, it) },
                    onCancel = { state = state.copy(selectedBall = null) })
                if (state.sessionComplete) {
                    PoolAction(stringResource(if (mode == BilliardsMode.TRY_POCKET) R.string.billiards_more else R.string.billiards_again), onClick = ::reset)
                    if (mode == BilliardsMode.TRY_POCKET) PoolAction(stringResource(R.string.billiards_versus)) { onMode(BilliardsMode.VERSUS) }
                    PoolAction(stringResource(R.string.billiards_free)) { onMode(BilliardsMode.FREE_PLAY) }
                } else if (state.roundComplete) {
                    PoolAction(stringResource(R.string.billiards_next)) { state = BilliardsEngine.nextTarget(state) }
                }
                Row(Modifier.widthIn(max = 680.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PoolAction(stringResource(if (state.aimGuide) R.string.billiards_aim_on else R.string.billiards_aim_off), Modifier.weight(1f)) { state = state.copy(aimGuide = !state.aimGuide) }
                    if (mode == BilliardsMode.FREE_PLAY) PoolAction(stringResource(R.string.billiards_rack), Modifier.weight(1f), onClick = ::reset)
                }
                Row(Modifier.widthIn(max = 680.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PoolAction(stringResource(R.string.billiards_back_modes), Modifier.weight(1f), onClick = onModesClick)
                    PoolAction(stringResource(R.string.back_to_games), Modifier.weight(1f), onClick = onGamesClick)
                }
            }
        }
    }
}

private fun completionText(state: BilliardsState): String = if (state.mode == BilliardsMode.TRY_POCKET) "Great pool, Olivia!" else when (BilliardsEngine.matchResult(state.oliviaBalls.size, state.looLooBalls.size)) {
    MatchResult.OLIVIA_WINS -> "Olivia wins! Great playing!"
    MatchResult.LOOLOO_WINS -> "Great game, Olivia! LooLoo wins this one!"
    MatchResult.TIE -> "It’s a tie! Great game!"
}

@Composable
private fun PoolAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick, modifier.heightIn(min = 52.dp), shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SnowWhite, contentColor = DeepIndigo)) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PoolTable(state: BilliardsState, foreground: Boolean, epoch: Int, modifier: Modifier, onSelect: (BallId) -> Unit, onShot: (BallShot) -> Unit, onCancel: () -> Unit) {
    val latestState by rememberUpdatedState(state)
    val latestSelect by rememberUpdatedState(onSelect)
    val latestShot by rememberUpdatedState(onShot)
    val latestCancel by rememberUpdatedState(onCancel)
    var drag by remember { mutableStateOf<Vec2?>(null) }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true; color = android.graphics.Color.BLACK } }
    BoxWithConstraints(modifier.semantics {
        stateDescription = state.table.balls.filter { it.pocketed }.joinToString(". ") { "${it.id.description} pocketed" }
    }) {
        val scale = maxWidth.value / 664f
        val density = LocalDensity.current
        val hitRadius = maxOf(48.0, 24.0 / scale)
        Canvas(Modifier.fillMaxSize().pointerInput(foreground, epoch, state.childCanShoot) {
            if (!foreground || !latestState.childCanShoot) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val worldScale = size.width / 664.0
                fun world(p: Offset) = Vec2(p.x / worldScale - 32, p.y / worldScale - 32)
                val start = world(down.position)
                val ball = latestState.table.balls.filter { !it.pocketed && (latestState.mode != BilliardsMode.TRY_POCKET || it.id == latestState.target?.ballId) }
                    .minByOrNull { (it.position - start).length() }
                if (ball == null || (ball.position - start).length() > hitRadius) return@awaitEachGesture
                down.consume()
                latestSelect(ball.id)
                drag = Vec2.ZERO
                var released = false
                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.isConsumed) break
                        change.consume()
                        drag = world(change.position) - start
                        if (!change.pressed) {
                            released = true
                            BilliardsPhysics.shotFromDrag(ball.id, drag!!)?.let { latestShot(it) }
                            break
                        }
                    }
                } finally {
                    drag = null
                    if (!released || !latestState.shotActive) latestCancel()
                }
            }
        }) {
            val s = size.width / 664f
            withTransform({ scale(s, s, pivot = Offset.Zero); translate(32f, 32f) }) {
                drawRoundRect(Color(0xFF794E30), Offset(-32f, -32f), Size(664f, 1064f), androidx.compose.ui.geometry.CornerRadius(28f))
                drawRect(Color(0xFF176F53), Offset.Zero, Size(600f, 1000f))
                drawRect(Color(0xFF164D3B), Offset.Zero, Size(600f,1000f), style = Stroke(9f))
                state.table.pockets.forEach { pocket ->
                    val p = Offset(pocket.position.x.toFloat(), pocket.position.y.toFloat())
                    drawCircle(Color(0xFF191D25), pocket.radius.toFloat(), p)
                    if (state.target?.pocketId == pocket.id && !state.sessionComplete) {
                        drawCircle(Color(0xFFFFE176), pocket.radius.toFloat() + 8, p, style = Stroke(7f))
                        drawCircle(Color.White, 6f, p)
                    }
                }
                state.table.balls.filterNot { it.pocketed }.forEach { ball ->
                    val p = Offset(ball.position.x.toFloat(), ball.position.y.toFloat())
                    drawCircle(Color.Black.copy(alpha = .25f), ball.radius.toFloat(), p + Offset(4f, 5f))
                    if (ball.id.style == BallStyle.STRIPE) {
                        drawCircle(Color.White, ball.radius.toFloat(), p)
                        val halfBand = ball.radius.toFloat() * .55f
                        clipRect(p.x - ball.radius.toFloat(), p.y - halfBand, p.x + ball.radius.toFloat(), p.y + halfBand) {
                            drawCircle(ballColor(ball.id), ball.radius.toFloat(), p)
                        }
                    } else {
                        drawCircle(ballColor(ball.id), ball.radius.toFloat(), p)
                    }
                    drawCircle(Color(0xFF17283A), ball.radius.toFloat(), p, style = Stroke(2f))
                    if (ball.id != BallId.CUE) {
                        drawCircle(Color.White, ball.radius.toFloat() * .62f, p)
                        paint.textSize = 27f
                        drawContext.canvas.nativeCanvas.drawText(ball.id.number.toString(), p.x, p.y - (paint.ascent() + paint.descent()) / 2, paint)
                    }
                    if (ball.id == state.selectedBall || ball.id == state.target?.ballId || ball.id == state.pendingAi?.ballId) {
                        drawCircle(Color.White, ball.radius.toFloat() + 7, p, style = Stroke(4f))
                    }
                }
                val aimingId = state.pendingAi?.ballId ?: state.selectedBall
                val vector = state.pendingAi?.let { it.direction * (it.power / 3) } ?: drag?.let { it * -1.0 }
                val aimingBall = state.table.balls.firstOrNull { it.id == aimingId }
                if (aimingBall != null && vector != null && vector.length() > 1) {
                    // Power stays visible even when the dotted aim guide is off.
                    val powerFraction = ((vector.length() * 3.0).coerceIn(BilliardsPhysics.MIN_SPEED, BilliardsPhysics.MAX_SPEED) / BilliardsPhysics.MAX_SPEED).toFloat()
                    drawRoundRect(Color.Black.copy(alpha = .45f), Offset(190f, 955f), Size(220f, 14f), androidx.compose.ui.geometry.CornerRadius(7f))
                    drawRoundRect(Color(0xFFFFE176), Offset(190f, 955f), Size(220f * powerFraction, 14f), androidx.compose.ui.geometry.CornerRadius(7f))
                    val start = aimingBall.position
                    val direction = vector.normalized()
                    val length = vector.length().coerceAtMost(250.0)
                    fun offset(v: Vec2) = Offset(v.x.toFloat(), v.y.toFloat())
                    val horizontal = if (direction.x > 0) (600 - start.x) / direction.x else if (direction.x < 0) -start.x / direction.x else Double.POSITIVE_INFINITY
                    val vertical = if (direction.y > 0) (1000 - start.y) / direction.y else if (direction.y < 0) -start.y / direction.y else Double.POSITIVE_INFINITY
                    val guideLength = minOf(length + 100, horizontal, vertical).coerceAtLeast(0.0)
                    val tip = start + direction * minOf(length, 95.0, guideLength)
                    drawLine(Color.White, offset(start), offset(tip), 6f)
                    val side = Vec2(-direction.y, direction.x)
                    drawLine(Color.White, offset(tip), offset(tip - direction * 17.0 + side * 10.0), 5f)
                    drawLine(Color.White, offset(tip), offset(tip - direction * 17.0 - side * 10.0), 5f)
                    if (state.aimGuide) {
                        var distance = 110.0
                        while (distance < guideLength) { drawCircle(Color(0xFFFFE176), 4f, offset(start + direction * distance)); distance += 18 }
                    }
                }
            }
        }
        // Native accessibility actions supplement dragging: select a ball, then activate
        // a pocket to roll toward it using the same power limits and physics.
        state.table.balls.forEach { ball ->
            if (!ball.pocketed) {
                Box(Modifier.offset { IntOffset(with(density) { ((ball.position.x + 32).toFloat() * scale).dp.roundToPx() } - with(density) { 24.dp.roundToPx() },
                    with(density) { ((ball.position.y + 32).toFloat() * scale).dp.roundToPx() } - with(density) { 24.dp.roundToPx() }) }
                    .size(48.dp).semantics {
                        contentDescription = ball.id.description
                        selected = state.selectedBall == ball.id
                        if (state.childCanShoot && foreground && (state.mode != BilliardsMode.TRY_POCKET || state.target?.ballId == ball.id)) {
                            onClick("Select ${ball.id.description}") { onSelect(ball.id); true }
                        }
                    })
            }
        }
        state.table.pockets.forEach { pocket ->
            Box(Modifier.offset { IntOffset(with(density) { ((pocket.position.x + 32).toFloat() * scale).dp.roundToPx() } - with(density) { 24.dp.roundToPx() },
                with(density) { ((pocket.position.y + 32).toFloat() * scale).dp.roundToPx() } - with(density) { 24.dp.roundToPx() }) }
                .size(48.dp).semantics {
                    contentDescription = if (state.target?.pocketId == pocket.id) "Target pocket" else "Pocket ${pocket.id + 1}"
                    if (state.childCanShoot && foreground && state.selectedBall != null) onClick("Roll selected ball toward pocket") {
                        val ball = state.table.balls.first { it.id == state.selectedBall }
                        val delta = pocket.position - ball.position
                        onShot(BallShot(ball.id, delta.normalized(), sqrt(2 * BilliardsPhysics.DECELERATION * (delta.length() + 35))))
                        true
                    }
                })
        }
    }
}

private fun ballColor(id: BallId): Color = when (id) {
    BallId.CUE -> Color.White
    BallId.ONE, BallId.NINE -> Color(0xFFFFD640)
    BallId.TWO, BallId.TEN -> Color(0xFF3D78EA)
    BallId.THREE, BallId.ELEVEN -> Color(0xFFED5260)
    BallId.FOUR, BallId.TWELVE -> Color(0xFF9860D5)
    BallId.FIVE, BallId.THIRTEEN -> Color(0xFFFF963C)
    BallId.SIX, BallId.FOURTEEN -> Color(0xFF69C956)
    BallId.SEVEN, BallId.FIFTEEN -> Color(0xFFA33363)
    BallId.EIGHT -> Color.Black
}

@Composable
fun BilliardsGameGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRoundRect(Color(0xFF855338), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()))
        drawRect(Color(0xFF247D5D), Offset(size.width*.08f,size.height*.1f), Size(size.width*.84f,size.height*.8f))
        listOf(Offset(.12f,.15f),Offset(.88f,.15f),Offset(.12f,.85f),Offset(.88f,.85f)).forEach {
            drawCircle(Color(0xFF202332),size.minDimension*.08f,Offset(size.width*it.x,size.height*it.y))
        }
        drawCircle(Color.White,size.minDimension*.09f,Offset(size.width*.35f,size.height*.65f))
        drawCircle(Color(0xFFFFD640),size.minDimension*.09f,Offset(size.width*.62f,size.height*.35f))
        drawCircle(Color(0xFFED5260),size.minDimension*.09f,Offset(size.width*.66f,size.height*.58f))
    }
}

@Composable
private fun RobotGlyph(modifier: Modifier) {
    Canvas(modifier) {
        val w=size.width; val h=size.height
        drawRoundRect(Color(0xFF9A89D9),Offset(w*.25f,h*.2f),Size(w*.5f,h*.65f),androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()))
        drawCircle(Color.White,h*.07f,Offset(w*.4f,h*.45f)); drawCircle(Color.White,h*.07f,Offset(w*.6f,h*.45f))
        drawLine(DeepIndigo,Offset(w*.4f,h*.68f),Offset(w*.6f,h*.68f),4.dp.toPx())
        drawLine(DeepIndigo,Offset(w*.5f,h*.2f),Offset(w*.5f,h*.05f),3.dp.toPx())
    }
}
