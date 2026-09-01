package com.nahtygal.olivialooi.ui.games.tictactoe

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.games.tictactoe.TicTacToeAi
import com.nahtygal.olivialooi.games.tictactoe.TicTacToeDifficulty
import com.nahtygal.olivialooi.games.tictactoe.TicTacToeEngine
import com.nahtygal.olivialooi.games.tictactoe.TicTacToeGameMode
import com.nahtygal.olivialooi.games.tictactoe.TicTacToeGameState
import com.nahtygal.olivialooi.games.tictactoe.TicTacToeMark
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.ReadyMint
import com.nahtygal.olivialooi.ui.theme.SnowWhite
import kotlinx.coroutines.delay

private val TicTacToeStateSaver = Saver<TicTacToeGameState, String>(
    save = { state ->
        listOf(
            state.board.joinToString("") { mark -> mark?.name ?: "_" },
            state.currentTurn.name,
            state.winner?.name.orEmpty(),
            state.winningCells.sorted().joinToString(","),
            state.isDraw.toString(),
        ).joinToString("|")
    },
    restore = { saved ->
        val fields = saved.split('|')
        TicTacToeGameState(
            board = fields[0].map { character ->
                when (character) {
                    'X' -> TicTacToeMark.X
                    'O' -> TicTacToeMark.O
                    else -> null
                }
            },
            currentTurn = enumValueOf<TicTacToeMark>(fields[1]),
            winner = fields[2]
                .takeIf(String::isNotEmpty)
                ?.let { enumValueOf<TicTacToeMark>(it) },
            winningCells = fields[3]
                .takeIf(String::isNotEmpty)
                ?.split(',')
                ?.map(String::toInt)
                ?.toSet()
                .orEmpty(),
            isDraw = fields[4].toBoolean(),
        )
    },
)

@Composable
fun TicTacToeGameScreen(
    gameMode: TicTacToeGameMode,
    onChangeModeClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var difficultyName by rememberSaveable { mutableStateOf(TicTacToeDifficulty.Easy.name) }
    val difficulty = enumValueOf<TicTacToeDifficulty>(difficultyName)
    var gameState by rememberSaveable(
        gameMode.name,
        difficultyName,
        stateSaver = TicTacToeStateSaver,
    ) {
        mutableStateOf(TicTacToeEngine.newGame())
    }
    val isComputerGame = gameMode == TicTacToeGameMode.PersonVsComputer
    val isLooLooThinking = isComputerGame &&
        !gameState.isGameOver &&
        gameState.currentTurn == TicTacToeMark.O

    LaunchedEffect(gameMode, difficulty, gameState) {
        if (!isLooLooThinking) return@LaunchedEffect

        delay(500)
        val move = when (difficulty) {
            TicTacToeDifficulty.Easy -> TicTacToeAi.easyMove(gameState)
            TicTacToeDifficulty.Smart -> TicTacToeAi.smartMove(gameState, TicTacToeMark.O)
        }
        if (move != null) {
            gameState = TicTacToeEngine.playMove(gameState, move)
        }
    }

    WinterGamesBackground(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabletLayout = maxWidth >= 600.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (tabletLayout) 28.dp else 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.tic_tac_toe_name),
                    color = SnowWhite,
                    fontSize = if (tabletLayout) 38.sp else 30.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(
                        if (isComputerGame) {
                            R.string.tic_tac_toe_players_looloo
                        } else {
                            R.string.tic_tac_toe_players_uncle_moo
                        },
                    ),
                    color = SnowWhite.copy(alpha = 0.9f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )

                if (isComputerGame) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DifficultySelector(
                        selectedDifficulty = difficulty,
                        onDifficultySelected = { selected -> difficultyName = selected.name },
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = gameStatusText(gameState, gameMode, isLooLooThinking),
                    color = SnowWhite,
                    fontSize = if (tabletLayout) 27.sp else 22.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))

                TicTacToeBoard(
                    state = gameState,
                    inputEnabled = !isLooLooThinking,
                    onCellClick = { cell ->
                        if (!isLooLooThinking) {
                            gameState = TicTacToeEngine.playMove(gameState, cell)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .sizeIn(maxWidth = 430.dp)
                        .aspectRatio(1f),
                )

                Spacer(modifier = Modifier.height(14.dp))
                if (gameState.isGameOver) {
                    GameActionButton(
                        label = stringResource(R.string.play_again),
                        onClick = { gameState = TicTacToeEngine.newGame() },
                        primary = true,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (tabletLayout) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GameActionButton(
                            label = stringResource(R.string.change_players_game_mode),
                            onClick = onChangeModeClick,
                        )
                        GameActionButton(
                            label = stringResource(R.string.home_action),
                            onClick = onHomeClick,
                        )
                    }
                } else {
                    GameActionButton(
                        label = stringResource(R.string.change_players_game_mode),
                        onClick = onChangeModeClick,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GameActionButton(
                        label = stringResource(R.string.home_action),
                        onClick = onHomeClick,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun gameStatusText(
    state: TicTacToeGameState,
    gameMode: TicTacToeGameMode,
    isLooLooThinking: Boolean,
): String = when {
    state.winner == TicTacToeMark.X -> stringResource(R.string.olivia_wins)
    state.winner == TicTacToeMark.O && gameMode == TicTacToeGameMode.PersonVsComputer ->
        stringResource(R.string.looloo_wins)

    state.winner == TicTacToeMark.O -> stringResource(R.string.uncle_moo_wins)
    state.isDraw -> stringResource(R.string.tic_tac_toe_draw)
    isLooLooThinking -> stringResource(R.string.looloo_turn)
    state.currentTurn == TicTacToeMark.X -> stringResource(R.string.olivia_turn)
    else -> stringResource(R.string.uncle_moo_turn)
}

@Composable
private fun DifficultySelector(
    selectedDifficulty: TicTacToeDifficulty,
    onDifficultySelected: (TicTacToeDifficulty) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TicTacToeDifficulty.entries.forEach { difficulty ->
            Button(
                onClick = { onDifficultySelected(difficulty) },
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (difficulty == selectedDifficulty) {
                        SnowWhite
                    } else {
                        SnowWhite.copy(alpha = 0.24f)
                    },
                    contentColor = if (difficulty == selectedDifficulty) DeepIndigo else SnowWhite,
                ),
            ) {
                Text(
                    text = stringResource(
                        if (difficulty == TicTacToeDifficulty.Easy) {
                            R.string.difficulty_easy
                        } else {
                            R.string.difficulty_smart
                        },
                    ),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun TicTacToeBoard(
    state: TicTacToeGameState,
    inputEnabled: Boolean,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(SnowWhite.copy(alpha = 0.94f))
            .padding(9.dp),
    ) {
        repeat(3) { row ->
            Row(modifier = Modifier.weight(1f)) {
                repeat(3) { column ->
                    val cell = row * 3 + column
                    val mark = state.board[cell]
                    val enabled = inputEnabled && !state.isGameOver && mark == null
                    val position = stringResource(cellPositionResource(cell))
                    val value = stringResource(
                        when (mark) {
                            TicTacToeMark.X -> R.string.board_cell_x
                            TicTacToeMark.O -> R.string.board_cell_o
                            null -> R.string.board_cell_empty
                        },
                    )
                    val description = stringResource(R.string.board_cell_description, position, value)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(3.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(
                                if (cell in state.winningCells) {
                                    ReadyMint.copy(alpha = 0.72f)
                                } else {
                                    Color(0xFFE6EEFF)
                                },
                            )
                            .semantics { contentDescription = description }
                            .clickable(enabled = enabled) { onCellClick(cell) },
                        contentAlignment = Alignment.Center,
                    ) {
                        TicTacToeMarkGlyph(mark = mark, modifier = Modifier.fillMaxSize(0.66f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TicTacToeMarkGlyph(mark: TicTacToeMark?, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = (size.minDimension * 0.11f).coerceAtLeast(4.dp.toPx())
        when (mark) {
            TicTacToeMark.X -> {
                drawLine(
                    Color(0xFF6155C7),
                    Offset(size.width * 0.18f, size.height * 0.18f),
                    Offset(size.width * 0.82f, size.height * 0.82f),
                    stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    Color(0xFF6155C7),
                    Offset(size.width * 0.82f, size.height * 0.18f),
                    Offset(size.width * 0.18f, size.height * 0.82f),
                    stroke,
                    cap = StrokeCap.Round,
                )
            }

            TicTacToeMark.O -> drawCircle(
                color = Color(0xFFE05A9D),
                radius = size.minDimension * 0.31f,
                center = center,
                style = Stroke(stroke),
            )

            null -> Unit
        }
    }
}

@Composable
private fun GameActionButton(
    label: String,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    if (primary) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .heightIn(min = 52.dp)
                .sizeIn(minWidth = 190.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SnowWhite,
                contentColor = DeepIndigo,
            ),
        ) {
            Text(label, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .heightIn(min = 52.dp)
                .sizeIn(minWidth = 190.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SnowWhite),
        ) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun cellPositionResource(cell: Int): Int = when (cell) {
    0 -> R.string.board_position_top_left
    1 -> R.string.board_position_top_center
    2 -> R.string.board_position_top_right
    3 -> R.string.board_position_middle_left
    4 -> R.string.board_position_center
    5 -> R.string.board_position_middle_right
    6 -> R.string.board_position_bottom_left
    7 -> R.string.board_position_bottom_center
    else -> R.string.board_position_bottom_right
}
