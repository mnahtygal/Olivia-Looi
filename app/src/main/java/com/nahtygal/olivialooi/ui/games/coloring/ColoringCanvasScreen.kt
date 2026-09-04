package com.nahtygal.olivialooi.ui.games.coloring

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.games.coloring.ColoringBrush
import com.nahtygal.olivialooi.games.coloring.ColoringColor
import com.nahtygal.olivialooi.games.coloring.ColoringEngine
import com.nahtygal.olivialooi.games.coloring.ColoringPicture
import com.nahtygal.olivialooi.games.coloring.ColoringPoint
import com.nahtygal.olivialooi.games.coloring.ColoringState
import com.nahtygal.olivialooi.games.coloring.ColoringStroke
import com.nahtygal.olivialooi.speech.AndroidTextToSpeech
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.FrostBlue
import com.nahtygal.olivialooi.ui.theme.SnowWhite

private val ColoringStateSaver = Saver<ColoringState, String>(
    save = { state ->
        val strokes = state.strokes.joinToString(";") { stroke ->
            val points = stroke.points.joinToString("/") { point -> "${point.x},${point.y}" }
            "${stroke.color.name}:${stroke.brush.name}:$points"
        }
        listOf(
            state.picture.name,
            state.selectedColor.name,
            state.selectedBrush.name,
            strokes,
        ).joinToString("|")
    },
    restore = { saved ->
        runCatching {
            val fields = saved.split('|', limit = 4)
            ColoringState(
                picture = enumValueOf(fields[0]),
                selectedColor = enumValueOf(fields[1]),
                selectedBrush = enumValueOf(fields[2]),
                strokes = fields[3].takeIf(String::isNotEmpty)?.split(';')?.map { savedStroke ->
                    val strokeFields = savedStroke.split(':', limit = 3)
                    ColoringStroke(
                        color = enumValueOf(strokeFields[0]),
                        brush = enumValueOf(strokeFields[1]),
                        points = strokeFields[2].split('/').map { savedPoint ->
                            val coordinates = savedPoint.split(',', limit = 2)
                            ColoringPoint(coordinates[0].toFloat(), coordinates[1].toFloat())
                        },
                    )
                }.orEmpty(),
            )
        }.getOrNull()
    },
)

private enum class ColoringConfirmation {
    Clear,
    NewPicture,
    BackToGames,
}

@Composable
fun ColoringCanvasScreen(
    picture: ColoringPicture,
    onNewPictureClick: () -> Unit,
    onBackToGamesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val textToSpeech = remember(context) { AndroidTextToSpeech(context.applicationContext) }
    var state by rememberSaveable(picture.name, stateSaver = ColoringStateSaver) {
        mutableStateOf(ColoringEngine.newPicture(picture))
    }
    val activePoints = remember(picture) { mutableStateListOf<ColoringPoint>() }
    var confirmation by remember { mutableStateOf<ColoringConfirmation?>(null) }
    var encouragementGiven by rememberSaveable(picture.name) { mutableStateOf(false) }
    val letsColorSpeech = stringResource(R.string.coloring_lets_color_speech)
    val encouragementSpeech = stringResource(R.string.coloring_encouragement_speech)
    val hasDrawing = state.strokes.isNotEmpty() || activePoints.isNotEmpty()

    DisposableEffect(textToSpeech) {
        onDispose { textToSpeech.close() }
    }
    LaunchedEffect(picture) {
        textToSpeech.speak(letsColorSpeech)
    }
    BackHandler {
        if (hasDrawing) confirmation = ColoringConfirmation.NewPicture else onNewPictureClick()
    }

    fun requestNavigation(target: ColoringConfirmation, action: () -> Unit) {
        if (hasDrawing) confirmation = target else action()
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
                    .padding(horizontal = if (tabletLayout) 22.dp else 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.coloring_name),
                    color = SnowWhite,
                    fontSize = if (tabletLayout) 34.sp else 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(picture.labelResource()),
                    color = SnowWhite.copy(alpha = 0.92f),
                    fontSize = if (tabletLayout) 19.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (tabletLayout) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 1120.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        ColoringCanvas(
                            state = state,
                            activePoints = activePoints,
                            onStrokeFinished = { points ->
                                state = ColoringEngine.addCompletedStroke(state, points)
                                if (!encouragementGiven && state.strokes.size >= 5) {
                                    encouragementGiven = true
                                    textToSpeech.speak(encouragementSpeech)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                        ColoringTools(
                            state = state,
                            onColorSelected = { state = ColoringEngine.selectColor(state, it) },
                            onBrushSelected = { state = ColoringEngine.selectBrush(state, it) },
                            onUndoClick = { state = ColoringEngine.undo(state) },
                            onClearClick = {
                                if (hasDrawing) confirmation = ColoringConfirmation.Clear
                            },
                            onNewPictureClick = {
                                requestNavigation(ColoringConfirmation.NewPicture, onNewPictureClick)
                            },
                            onBackToGamesClick = {
                                requestNavigation(ColoringConfirmation.BackToGames, onBackToGamesClick)
                            },
                            modifier = Modifier.width(300.dp),
                        )
                    }
                } else {
                    ColoringCanvas(
                        state = state,
                        activePoints = activePoints,
                        onStrokeFinished = { points ->
                            state = ColoringEngine.addCompletedStroke(state, points)
                            if (!encouragementGiven && state.strokes.size >= 5) {
                                encouragementGiven = true
                                textToSpeech.speak(encouragementSpeech)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp)
                            .aspectRatio(1f),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ColoringTools(
                        state = state,
                        onColorSelected = { state = ColoringEngine.selectColor(state, it) },
                        onBrushSelected = { state = ColoringEngine.selectBrush(state, it) },
                        onUndoClick = { state = ColoringEngine.undo(state) },
                        onClearClick = {
                            if (hasDrawing) confirmation = ColoringConfirmation.Clear
                        },
                        onNewPictureClick = {
                            requestNavigation(ColoringConfirmation.NewPicture, onNewPictureClick)
                        },
                        onBackToGamesClick = {
                            requestNavigation(ColoringConfirmation.BackToGames, onBackToGamesClick)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    confirmation?.let { pending ->
        ColoringConfirmationDialog(
            confirmation = pending,
            onConfirm = {
                confirmation = null
                when (pending) {
                    ColoringConfirmation.Clear -> {
                        activePoints.clear()
                        state = ColoringEngine.clear(state)
                    }
                    ColoringConfirmation.NewPicture -> onNewPictureClick()
                    ColoringConfirmation.BackToGames -> onBackToGamesClick()
                }
            },
            onDismiss = { confirmation = null },
        )
    }
}

@Composable
private fun ColoringCanvas(
    state: ColoringState,
    activePoints: MutableList<ColoringPoint>,
    onStrokeFinished: (List<ColoringPoint>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pictureName = stringResource(state.picture.labelResource())
    val canvasDescription = stringResource(R.string.coloring_canvas_description, pictureName)
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFFFFEFF))
            .border(4.dp, FrostBlue, RoundedCornerShape(24.dp))
            .semantics { contentDescription = canvasDescription }
            .pointerInput(state.selectedColor, state.selectedBrush) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    activePoints.clear()
                    down.position.toNormalizedPoint(size.width, size.height)?.let(activePoints::add)
                    down.consume()
                    var pointer = down
                    while (pointer.pressed) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (pointer.pressed) {
                            pointer.position.toNormalizedPoint(size.width, size.height)?.let { point ->
                                val distance = activePoints.lastOrNull()?.distanceSquared(point) ?: 1f
                                if (distance >= MIN_POINT_DISTANCE) {
                                    activePoints.add(point)
                                }
                            }
                        }
                        pointer.consume()
                    }
                    if (activePoints.isNotEmpty()) onStrokeFinished(activePoints.toList())
                    activePoints.clear()
                }
            },
    ) {
        state.strokes.forEach(::drawColoringStroke)
        if (activePoints.isNotEmpty()) {
            drawColoringStroke(
                ColoringStroke(
                    color = state.selectedColor,
                    brush = state.selectedBrush,
                    points = activePoints,
                ),
            )
        }
        drawColoringLineArt(state.picture)
    }
}

@Composable
private fun ColoringTools(
    state: ColoringState,
    onColorSelected: (ColoringColor) -> Unit,
    onBrushSelected: (ColoringBrush) -> Unit,
    onUndoClick: () -> Unit,
    onClearClick: () -> Unit,
    onNewPictureClick: () -> Unit,
    onBackToGamesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(SnowWhite.copy(alpha = 0.94f), RoundedCornerShape(24.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.coloring_pick_color),
            color = DeepIndigo,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        ColorPalette(
            selectedColor = state.selectedColor,
            onColorSelected = onColorSelected,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.coloring_brush_size),
            color = DeepIndigo,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        BrushSelector(
            selectedBrush = state.selectedBrush,
            onBrushSelected = onBrushSelected,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ColoringControlButton(
                label = stringResource(R.string.coloring_undo),
                onClick = onUndoClick,
                enabled = state.strokes.isNotEmpty() || state.activeStroke != null,
                modifier = Modifier.weight(1f),
            )
            ColoringControlButton(
                label = stringResource(R.string.coloring_clear),
                onClick = onClearClick,
                enabled = state.strokes.isNotEmpty() || state.activeStroke != null,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        ColoringControlButton(
            label = stringResource(R.string.coloring_new_picture),
            onClick = onNewPictureClick,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        ColoringControlButton(
            label = stringResource(R.string.back_to_games),
            onClick = onBackToGamesClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ColorPalette(
    selectedColor: ColoringColor,
    onColorSelected: (ColoringColor) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ColoringColor.entries.chunked(5).forEach { colors ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { color ->
                    val name = stringResource(color.labelResource())
                    val isSelected = color == selectedColor
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(color.argb))
                            .border(
                                width = if (isSelected) 5.dp else 2.dp,
                                color = if (isSelected) DeepIndigo else Color(0xFF8C87A8),
                                shape = CircleShape,
                            )
                            .clickable { onColorSelected(color) }
                            .semantics(mergeDescendants = true) {
                                contentDescription = name
                                selected = isSelected
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Text(
                                text = "✓",
                                color = if (color == ColoringColor.White || color == ColoringColor.Yellow) {
                                    DeepIndigo
                                } else {
                                    Color.White
                                },
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrushSelector(
    selectedBrush: ColoringBrush,
    onBrushSelected: (ColoringBrush) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ColoringBrush.entries.forEach { brush ->
            val description = stringResource(brush.descriptionResource())
            val isSelected = brush == selectedBrush
            Button(
                onClick = { onBrushSelected(brush) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp)
                    .clearAndSetSemantics {
                        contentDescription = description
                        selected = isSelected
                    },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) DeepIndigo else FrostBlue,
                    contentColor = if (isSelected) SnowWhite else DeepIndigo,
                ),
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size((brush.widthDp * 0.65f).dp.coerceAtLeast(7.dp))
                            .background(
                                if (isSelected) SnowWhite else DeepIndigo,
                                CircleShape,
                            ),
                    )
                    Text(
                        text = stringResource(brush.labelResource()),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColoringControlButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 50.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = DeepIndigo,
            disabledContentColor = DeepIndigo.copy(alpha = 0.42f),
        ),
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun ColoringConfirmationDialog(
    confirmation: ColoringConfirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val clear = confirmation == ColoringConfirmation.Clear
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (clear) R.string.coloring_clear_question else R.string.coloring_discard_question,
                ),
                color = DeepIndigo,
                fontWeight = FontWeight.ExtraBold,
            )
        },
        text = {
            if (!clear) Text(stringResource(R.string.coloring_discard_message))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
            ) {
                Text(
                    stringResource(
                        if (clear) R.string.coloring_yes_clear else R.string.coloring_yes_discard,
                    ),
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.coloring_keep_coloring))
            }
        },
        containerColor = SnowWhite,
        shape = RoundedCornerShape(28.dp),
    )
}

private fun DrawScope.drawColoringStroke(stroke: ColoringStroke) {
    if (stroke.points.isEmpty()) return
    val strokeWidth = stroke.brush.widthDp.dp.toPx()
    if (stroke.points.size == 1) {
        val point = stroke.points.first()
        drawCircle(
            Color(stroke.color.argb),
            strokeWidth / 2f,
            Offset(point.x * size.width, point.y * size.height),
        )
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
    drawPath(
        path = path,
        color = Color(stroke.color.argb),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun Offset.toNormalizedPoint(width: Int, height: Int): ColoringPoint? {
    if (width <= 0 || height <= 0 || x !in 0f..width.toFloat() || y !in 0f..height.toFloat()) {
        return null
    }
    return ColoringPoint(x / width, y / height)
}

private fun ColoringPoint.distanceSquared(other: ColoringPoint): Float {
    val dx = x - other.x
    val dy = y - other.y
    return dx * dx + dy * dy
}

private fun ColoringColor.labelResource(): Int = when (this) {
    ColoringColor.Red -> R.string.coloring_red
    ColoringColor.Orange -> R.string.coloring_orange
    ColoringColor.Yellow -> R.string.coloring_yellow
    ColoringColor.Green -> R.string.coloring_green
    ColoringColor.Blue -> R.string.coloring_blue
    ColoringColor.Purple -> R.string.coloring_purple
    ColoringColor.Pink -> R.string.coloring_pink
    ColoringColor.Brown -> R.string.coloring_brown
    ColoringColor.Black -> R.string.coloring_black
    ColoringColor.White -> R.string.coloring_white
}

private fun ColoringBrush.labelResource(): Int = when (this) {
    ColoringBrush.Small -> R.string.coloring_small
    ColoringBrush.Medium -> R.string.coloring_medium
    ColoringBrush.Big -> R.string.coloring_big
}

private fun ColoringBrush.descriptionResource(): Int = when (this) {
    ColoringBrush.Small -> R.string.coloring_small_brush
    ColoringBrush.Medium -> R.string.coloring_medium_brush
    ColoringBrush.Big -> R.string.coloring_big_brush
}

private const val MIN_POINT_DISTANCE = 0.000025f
