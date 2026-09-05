package com.nahtygal.olivialooi.ui.games.puzzles

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.games.puzzles.PuzzleDifficulty
import com.nahtygal.olivialooi.games.puzzles.PuzzleEngine
import com.nahtygal.olivialooi.games.puzzles.PuzzlePicture
import com.nahtygal.olivialooi.games.puzzles.PuzzlePiece
import com.nahtygal.olivialooi.games.puzzles.PuzzleSpeech
import com.nahtygal.olivialooi.games.puzzles.PuzzleState
import com.nahtygal.olivialooi.games.puzzles.PuzzleStateCodec
import com.nahtygal.olivialooi.speech.AndroidTextToSpeech
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.SnowWhite
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

private val PuzzleSaver = Saver<PuzzleState, String>(save = { PuzzleStateCodec.encode(it) }, restore = PuzzleStateCodec::decode)

/** Only the drag overlay reads center during drawing/layout. The engine is untouched by samples. */
@Stable
private class PuzzleDrag {
    var piece: PuzzlePiece? by mutableStateOf(null)
    var center by mutableStateOf(Offset.Zero)
    var tileSize by mutableStateOf(Size.Zero)
    var returning by mutableStateOf(false)
    var grabbed by mutableStateOf(false)
    var origin = Offset.Zero
    var attempt = 0L
    fun clear() { piece = null; returning = false; grabbed = false }
}

@Composable
fun PuzzleBoardScreen(
    picture: PuzzlePicture,
    difficulty: PuzzleDifficulty,
    sessionId: Int,
    onChoosePieces: () -> Unit,
    onPickAnotherPuzzle: () -> Unit,
    onGamesClick: () -> Unit,
) {
    var state by rememberSaveable(picture.name, difficulty.name, sessionId, stateSaver = PuzzleSaver) {
        mutableStateOf(PuzzleEngine.newPuzzle(picture, difficulty))
    }
    val context = LocalContext.current
    val speech = remember(context) { AndroidTextToSpeech(context.applicationContext) }
    DisposableEffect(speech) { onDispose { speech.close() } }
    // Acknowledge before speaking so rotation/recomposition cannot repeat a milestone.
    LaunchedEffect(state.pendingMilestones) {
        val pending = state.pendingMilestones
        if (pending.isNotEmpty()) {
            state = PuzzleEngine.acknowledgeMilestones(state)
            speech.speak(PuzzleSpeech.phrase(state.picture, pending.last()))
        }
    }

    val drag = remember { PuzzleDrag() }
    var boardBounds by remember { mutableStateOf(Rect.Zero) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var rootHeight by remember { mutableFloatStateOf(0f) }
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val edgeZone = with(density) { 96.dp.toPx() }
    val scrollSpeed = with(density) { 480.dp.toPx() }
    var confirmation by rememberSaveable { mutableStateOf<String?>(null) }
    var previewRequest by remember { mutableIntStateOf(0) }
    var showingPreview by remember { mutableStateOf(false) }
    var retry by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var returnJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(previewRequest) {
        if (previewRequest > 0) {
            showingPreview = true
            delay(2000)
            showingPreview = false
        }
    }

    LaunchedEffect(drag.piece?.id, drag.returning) {
        if (drag.piece != null && !drag.returning) {
            var previousFrame = withFrameNanos { it }
            while (isActive) {
                val frame = withFrameNanos { it }
                val elapsed = ((frame - previousFrame) / 1_000_000_000f).coerceAtMost(.05f)
                previousFrame = frame
                val y = drag.center.y - rootOrigin.y
                val direction = when {
                    y < edgeZone -> -1f
                    y > rootHeight - edgeZone -> 1f
                    else -> 0f
                }
                if (direction != 0f) scroll.scrollBy(direction * scrollSpeed * elapsed)
            }
        }
    }
    LaunchedEffect(state.isComplete) {
        if (state.isComplete) scroll.animateScrollTo(0)
    }

    fun clearTransient() {
        returnJob?.cancel()
        drag.clear()
        previewRequest = 0
        showingPreview = false
        retry = false
    }
    fun restart() {
        clearTransient()
        speech.stop()
        state = PuzzleEngine.startOver(state)
        confirmation = null
        scope.launch { scroll.scrollTo(0) }
    }
    fun requestLeave() {
        if (state.needsLeaveConfirmation) {
            clearTransient()
            confirmation = "leave"
        } else onChoosePieces()
    }
    BackHandler { requestLeave() }

    fun place(piece: PuzzlePiece, cell: Int, attempt: Long): Boolean {
        val before = state
        val updated = PuzzleEngine.placePiece(before, piece.id, cell, attempt)
        state = updated
        val succeeded = updated.completedPieceCount > before.completedPieceCount
        retry = !succeeded && !before.isComplete
        return succeeded
    }

    WinterGamesBackground(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned {
            rootOrigin = it.localToRoot(Offset.Zero)
            rootHeight = it.size.height.toFloat()
        }) {
            val tablet = maxWidth >= 600.dp
            val sideBySide = tablet && maxWidth >= 900.dp && maxWidth > maxHeight
            val boardWidth = if (sideBySide) minOf((maxWidth - 64.dp) * .57f, 620.dp, (maxHeight - 270.dp).coerceAtLeast(300.dp)) else minOf(maxWidth - 32.dp, 580.dp)
            Column(
                Modifier.fillMaxSize().verticalScroll(scroll, enabled = !drag.grabbed && drag.piece == null)
                    .statusBarsPadding().navigationBarsPadding().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.puzzle_name), color = SnowWhite, fontSize = if (tablet) 36.sp else 28.sp, fontWeight = FontWeight.ExtraBold)
                val progress = pluralStringResource(R.plurals.puzzle_progress, state.totalPieceCount, state.completedPieceCount, state.totalPieceCount)
                val progressDescription = pluralStringResource(R.plurals.puzzle_progress_description, state.totalPieceCount, state.completedPieceCount, state.totalPieceCount)
                Text(progress, Modifier.semantics { contentDescription = progressDescription; liveRegion = LiveRegionMode.Polite }, color = SnowWhite, fontSize = 20.sp)
                if (state.isComplete) {
                    PuzzleImage(picture, Modifier.width(boardWidth).aspectRatio(1f).clip(RoundedCornerShape(20.dp)))
                    Text(stringResource(R.string.puzzle_complete), color = SnowWhite, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Text("✦  ✨  ✦", color = Color(0xFFFFDE77), fontSize = 32.sp)
                    PuzzleAction(stringResource(R.string.puzzle_again), onClick = ::restart)
                    PuzzleAction(stringResource(R.string.puzzle_pick_another), onClick = onPickAnotherPuzzle)
                    PuzzleAction(stringResource(R.string.back_to_games), onClick = onGamesClick)
                } else {
                    Text(stringResource(R.string.puzzle_instructions), color = SnowWhite, fontSize = 18.sp, textAlign = TextAlign.Center)
                    val board: @Composable () -> Unit = {
                        PuzzleBoard(state, showingPreview, Modifier.width(boardWidth), onBounds = { boardBounds = it }) { cell ->
                            if (drag.piece == null && confirmation == null) {
                                state.selectedPieceId?.let { id -> place(state.pieces.first { it.id == id }, cell, state.attemptIdentity) }
                            }
                        }
                    }
                    val tray: @Composable (Modifier) -> Unit = { modifier ->
                        PuzzleTray(
                            state, drag, modifier,
                            onSelect = { id -> if (drag.piece == null) { state = PuzzleEngine.selectPiece(state, id); retry = false } },
                            onDragStart = { piece, bounds ->
                                if (!drag.returning && confirmation == null) {
                                    drag.piece = piece
                                    drag.origin = bounds.center
                                    drag.center = bounds.center
                                    drag.tileSize = bounds.size
                                    drag.attempt = state.attemptIdentity
                                    retry = false
                                }
                            },
                            onDrop = { returnBounds ->
                                val piece = drag.piece
                                if (piece != null) {
                                    val cell = puzzleDropCell(drag.center, drag.tileSize, boardBounds, state.rows, state.columns, piece.targetIndex)
                                    if (place(piece, cell, drag.attempt)) drag.clear()
                                    else {
                                        drag.returning = true
                                        returnJob = scope.launch {
                                            val animation = Animatable(drag.center, Offset.VectorConverter)
                                            animation.animateTo(returnBounds.center, tween(220)) { drag.center = value }
                                            drag.clear()
                                        }
                                    }
                                } else drag.clear()
                            },
                        )
                    }
                    if (sideBySide) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
                            board()
                            tray(Modifier.weight(1f))
                        }
                    } else {
                        board()
                        tray(Modifier.widthIn(max = 700.dp).fillMaxWidth())
                    }
                    if (retry) Text(stringResource(R.string.puzzle_retry), Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = SnowWhite, fontSize = 20.sp)
                    Row(Modifier.widthIn(max = 650.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PuzzleAction(
                            stringResource(R.string.puzzle_show_picture), Modifier.weight(1f), stringResource(R.string.puzzle_show_picture_description),
                        ) {
                            if (drag.piece == null) { previewRequest += 1; speech.speak(PuzzleSpeech.PREVIEW) }
                        }
                        PuzzleAction(stringResource(R.string.puzzle_start_over), Modifier.weight(1f), stringResource(R.string.puzzle_start_over_description)) {
                            if (state.completedPieceCount > 0) { clearTransient(); confirmation = "restart" } else restart()
                        }
                    }
                    PuzzleAction(stringResource(R.string.puzzle_back_difficulty), description = stringResource(R.string.puzzle_back_description), onClick = ::requestLeave)
                }
                Spacer(Modifier.height(4.dp))
            }
            DragOverlay(picture, difficulty, drag, rootOrigin)
        }
    }
    if (confirmation != null) {
        val restarting = confirmation == "restart"
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(stringResource(if (restarting) R.string.puzzle_restart_question else R.string.puzzle_leave_question)) },
            confirmButton = {
                TextButton(onClick = {
                    if (restarting) restart() else { confirmation = null; speech.stop(); onChoosePieces() }
                }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(if (restarting) R.string.puzzle_start_over else R.string.puzzle_leave))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmation = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.puzzle_keep_puzzling)) }
            },
        )
    }
}

@Composable
private fun PuzzleBoard(state: PuzzleState, preview: Boolean, modifier: Modifier, onBounds: (Rect) -> Unit, onCell: (Int) -> Unit) {
    Box(modifier.aspectRatio(1f).onGloballyPositioned {
        onBounds(Rect(it.localToRoot(Offset.Zero), Size(it.size.width.toFloat(), it.size.height.toFloat())))
    }.background(Color(0xFFEDF2FC), RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))) {
        if (preview) PuzzleImage(state.picture, Modifier.fillMaxSize().alpha(.35f))
        Column(Modifier.fillMaxSize()) {
            repeat(state.rows) { row ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    repeat(state.columns) { column ->
                        val index = row * state.columns + column
                        val piece = state.pieces.first { it.targetIndex == index }
                        val description = if (piece.isPlaced) {
                            stringResource(R.string.puzzle_cell_filled, index + 1, piece.id + 1)
                        } else stringResource(R.string.puzzle_cell_empty, index + 1)
                        Box(
                            Modifier.weight(1f).fillMaxSize()
                                .clickable(enabled = !piece.isPlaced && state.selectedPieceId != null) { onCell(index) }
                                .semantics { contentDescription = description },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (piece.isPlaced) PuzzleImage(state.picture, Modifier.fillMaxSize(), piece, state.difficulty)
                            else Text((index + 1).toString(), color = DeepIndigo.copy(alpha = .42f), fontSize = 18.sp)
                        }
                    }
                }
            }
        }
        PuzzleGrid(state.difficulty, Modifier.fillMaxSize())
    }
}

@Composable
internal fun PuzzleGrid(difficulty: PuzzleDifficulty, modifier: Modifier) {
    Canvas(modifier) {
        val lineColor = Color(0xFF536280)
        repeat(difficulty.columns + 1) { index ->
            val x = size.width * index / difficulty.columns
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
        }
        repeat(difficulty.rows + 1) { index ->
            val y = size.height * index / difficulty.rows
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
        }
    }
}

@Composable
private fun PuzzleTray(
    state: PuzzleState,
    drag: PuzzleDrag,
    modifier: Modifier,
    onSelect: (Int) -> Unit,
    onDragStart: (PuzzlePiece, Rect) -> Unit,
    onDrop: (Rect) -> Unit,
) {
    BoxWithConstraints(modifier) {
        // Easy tiles are wider than tall. Keep every tile at least roughly 72dp tall.
        val tileAspect = state.rows.toFloat() / state.columns
        val desiredWidth = maxOf(108.dp, 80.dp * tileAspect)
        val columns = ((maxWidth + 10.dp) / (desiredWidth + 10.dp)).toInt().coerceIn(1, 4)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.trayOrder.chunked(columns).forEach { ids ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ids.forEach { id ->
                        val piece = state.pieces.first { it.id == id }
                        TrayPiece(state, piece, drag, onSelect, onDragStart, onDrop, Modifier.weight(1f))
                    }
                    repeat(columns - ids.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun TrayPiece(
    state: PuzzleState,
    piece: PuzzlePiece,
    drag: PuzzleDrag,
    onSelect: (Int) -> Unit,
    onDragStart: (PuzzlePiece, Rect) -> Unit,
    onDrop: (Rect) -> Unit,
    modifier: Modifier,
) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val select by rememberUpdatedState(onSelect)
    val start by rememberUpdatedState(onDragStart)
    val drop by rememberUpdatedState(onDrop)
    val isSelected = state.selectedPieceId == piece.id
    val description = stringResource(if (isSelected) R.string.puzzle_piece_selected else R.string.puzzle_piece_available, piece.id + 1)
    Card(
        modifier = modifier.onGloballyPositioned {
            bounds = Rect(it.localToRoot(Offset.Zero), Size(it.size.width.toFloat(), it.size.height.toFloat()))
        }.alpha(if (drag.piece?.id == piece.id) .25f else 1f)
            .semantics {
                contentDescription = description
                selected = isSelected
                role = Role.Button
                onClick { select(piece.id); true }
            }
            .pointerInput(piece.id) {
                awaitEachGesture {
                    // Claim tray gestures at Initial pass, before the surrounding scroll container.
                    // Scroll still works from board/background. Finger and stylus share this path.
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (drag.piece != null || drag.returning) return@awaitEachGesture
                    down.consume()
                    drag.grabbed = true
                    val pointerId = down.id
                    val downPosition = down.position
                    var dragging = false
                    var released = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (change.isConsumed) break
                            val delta = change.position - downPosition
                            change.consume()
                            if (!dragging && delta.getDistance() > viewConfiguration.touchSlop) {
                                start(piece, bounds)
                                dragging = drag.piece?.id == piece.id
                            }
                            if (dragging) {
                                // Source cards may move during edge scrolling; convert each sample
                                // to root coordinates before applying the original grab offset.
                                drag.center = bounds.topLeft + change.position - downPosition +
                                    Offset(bounds.width / 2, bounds.height / 2)
                            }
                            if (!change.pressed) {
                                released = true
                                if (dragging) drop(bounds) else select(piece.id)
                                break
                            }
                        }
                    } finally {
                        drag.grabbed = false
                        if (!released) drag.clear()
                    }
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SnowWhite),
        border = BorderStroke(if (isSelected) 4.dp else 1.dp, if (isSelected) DeepIndigo else Color(0xFFABB6CD)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp),
    ) {
        Box(Modifier.padding(4.dp)) {
            PuzzleImage(state.picture, Modifier.fillMaxWidth().aspectRatio(state.rows.toFloat() / state.columns).clip(RoundedCornerShape(8.dp)), piece, state.difficulty)
        }
    }
}

@Composable
private fun DragOverlay(picture: PuzzlePicture, difficulty: PuzzleDifficulty, drag: PuzzleDrag, rootOrigin: Offset) {
    val piece = drag.piece ?: return
    val density = LocalDensity.current
    Card(
        Modifier.zIndex(10f)
            .offset { IntOffset((drag.center.x - rootOrigin.x - drag.tileSize.width / 2).roundToInt(), (drag.center.y - rootOrigin.y - drag.tileSize.height / 2).roundToInt()) }
            .size(with(density) { drag.tileSize.width.toDp() }, with(density) { drag.tileSize.height.toDp() })
            .graphicsLayer { scaleX = 1.04f; scaleY = 1.04f }
            .clearAndSetSemantics { },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        border = BorderStroke(2.dp, DeepIndigo),
    ) {
        PuzzleImage(picture, Modifier.fillMaxSize().padding(3.dp), piece, difficulty)
    }
}
