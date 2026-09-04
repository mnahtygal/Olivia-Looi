package com.nahtygal.olivialooi.ui.games.memory

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import com.nahtygal.olivialooi.games.memory.MemoryCard
import com.nahtygal.olivialooi.games.memory.MemoryCardIdentity
import com.nahtygal.olivialooi.games.memory.MemoryGameSize
import com.nahtygal.olivialooi.games.memory.MemoryMatchEngine
import com.nahtygal.olivialooi.games.memory.MemoryMatchState
import com.nahtygal.olivialooi.speech.AndroidTextToSpeech
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.FrostBlue
import com.nahtygal.olivialooi.ui.theme.ReadyMint
import com.nahtygal.olivialooi.ui.theme.SnowWhite
import kotlinx.coroutines.delay

private val MemoryMatchStateSaver = Saver<MemoryMatchState, String>(
    save = { state ->
        listOf(
            state.size.name,
            state.cards.joinToString(",") { card ->
                "${card.identity.name}:${card.isFaceUp}:${card.isMatched}"
            },
            state.firstSelection?.toString().orEmpty(),
            state.secondSelection?.toString().orEmpty(),
            state.matchedPairCount.toString(),
        ).joinToString("|")
    },
    restore = { saved ->
        runCatching {
            val fields = saved.split('|')
            MemoryMatchState(
                size = enumValueOf(fields[0]),
                cards = fields[1].split(',').mapIndexed { position, savedCard ->
                    val cardFields = savedCard.split(':')
                    MemoryCard(
                        position = position,
                        identity = enumValueOf(cardFields[0]),
                        isFaceUp = cardFields[1].toBoolean(),
                        isMatched = cardFields[2].toBoolean(),
                    )
                },
                firstSelection = fields[2].toIntOrNull(),
                secondSelection = fields[3].toIntOrNull(),
                matchedPairCount = fields[4].toInt(),
            )
        }.getOrNull()
    },
)

@Composable
fun MemoryMatchGameScreen(
    gameSize: MemoryGameSize,
    onPickAnotherGameClick: () -> Unit,
    onBackToGamesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val textToSpeech = remember(context) { AndroidTextToSpeech(context.applicationContext) }
    var gameState by rememberSaveable(
        gameSize.name,
        stateSaver = MemoryMatchStateSaver,
    ) {
        mutableStateOf(MemoryMatchEngine.newGame(gameSize))
    }
    val matchSpeech = stringResource(R.string.memory_match_speech)
    val mismatchSpeech = stringResource(R.string.memory_mismatch_speech)
    val completionSpeech = stringResource(R.string.memory_complete_speech)

    DisposableEffect(textToSpeech) {
        onDispose { textToSpeech.close() }
    }

    LaunchedEffect(gameState.firstSelection, gameState.secondSelection) {
        if (gameState.isResolvingMismatch) {
            delay(MISMATCH_REVEAL_MILLIS)
            gameState = MemoryMatchEngine.resolveMismatch(gameState)
        }
    }

    fun selectCard(position: Int) {
        val previous = gameState
        val updated = MemoryMatchEngine.selectCard(previous, position)
        if (updated == previous) return
        gameState = updated
        when {
            updated.isComplete && !previous.isComplete -> textToSpeech.speak(completionSpeech)
            updated.matchedPairCount > previous.matchedPairCount -> textToSpeech.speak(matchSpeech)
            updated.isResolvingMismatch -> textToSpeech.speak(mismatchSpeech)
        }
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
                    .padding(horizontal = if (tabletLayout) 28.dp else 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.memory_match_name),
                    color = SnowWhite,
                    fontSize = if (tabletLayout) 38.sp else 30.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.memory_match_progress,
                        gameState.matchedPairCount,
                        gameState.matchedPairCount,
                        gameState.size.pairCount,
                    ),
                    color = SnowWhite.copy(alpha = 0.92f),
                    fontSize = if (tabletLayout) 19.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(if (tabletLayout) 16.dp else 10.dp))

                MemoryBoard(
                    state = gameState,
                    inputEnabled = !gameState.isResolvingMismatch && !gameState.isComplete,
                    onCardClick = ::selectCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = if (gameSize == MemoryGameSize.Little) 560.dp else 620.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))
                if (gameState.isComplete) {
                    CompletionContent(
                        tabletLayout = tabletLayout,
                        onPlayAgainClick = {
                            textToSpeech.stop()
                            gameState = MemoryMatchEngine.reset(gameState)
                        },
                        onPickAnotherGameClick = onPickAnotherGameClick,
                        onBackToGamesClick = onBackToGamesClick,
                    )
                } else {
                    MemoryActionButton(
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
private fun MemoryBoard(
    state: MemoryMatchState,
    inputEnabled: Boolean,
    onCardClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.cards.chunked(state.size.columnCount).forEach { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowCards.forEach { card ->
                    MemoryCardView(
                        card = card,
                        enabled = inputEnabled && !card.isFaceUp && !card.isMatched,
                        onClick = { onCardClick(card.position) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.9f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryCardView(
    card: MemoryCard,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val identity = stringResource(card.identity.labelResource())
    val stateDescription = stringResource(
        when {
            card.isMatched -> R.string.memory_card_matched
            card.isFaceUp -> R.string.memory_card_revealed
            else -> R.string.memory_card_hidden
        },
    )
    val description = stringResource(R.string.memory_card_description, identity, stateDescription)
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                card.isMatched -> ReadyMint
                card.isFaceUp -> SnowWhite
                else -> DeepIndigo
            },
            disabledContainerColor = when {
                card.isMatched -> ReadyMint
                card.isFaceUp -> SnowWhite
                else -> DeepIndigo
            },
        ),
        border = BorderStroke(
            width = if (card.isMatched) 4.dp else 2.dp,
            color = if (card.isMatched) SnowWhite else FrostBlue,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (card.isFaceUp || card.isMatched) {
                Text(
                    text = card.identity.symbol,
                    fontSize = 38.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = identity,
                    color = DeepIndigo,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                if (card.isMatched) {
                    Text(
                        text = "✓",
                        color = DeepIndigo,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            } else {
                Text(
                    text = "❄",
                    color = FrostBlue,
                    fontSize = 40.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "?",
                    color = SnowWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun CompletionContent(
    tabletLayout: Boolean,
    onPlayAgainClick: () -> Unit,
    onPickAnotherGameClick: () -> Unit,
    onBackToGamesClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 620.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "🎉  ✨  🎉",
            fontSize = if (tabletLayout) 34.sp else 27.sp,
        )
        Text(
            text = stringResource(R.string.memory_complete_heading),
            color = SnowWhite,
            fontSize = if (tabletLayout) 31.sp else 25.sp,
            lineHeight = 35.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.memory_complete_subtitle),
            color = SnowWhite.copy(alpha = 0.94f),
            fontSize = if (tabletLayout) 21.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MemoryActionButton(
            label = stringResource(R.string.play_again),
            onClick = onPlayAgainClick,
            primary = true,
        )
        Spacer(modifier = Modifier.height(8.dp))
        MemoryActionButton(
            label = stringResource(R.string.pick_another_game),
            onClick = onPickAnotherGameClick,
        )
        Spacer(modifier = Modifier.height(8.dp))
        MemoryActionButton(
            label = stringResource(R.string.back_to_games),
            onClick = onBackToGamesClick,
        )
    }
}

@Composable
private fun MemoryActionButton(
    label: String,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    if (primary) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .heightIn(min = 52.dp)
                .sizeIn(minWidth = 210.dp),
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
                .sizeIn(minWidth = 210.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SnowWhite),
        ) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private val MemoryCardIdentity.symbol: String
    get() = when (this) {
        MemoryCardIdentity.Cow -> "🐮"
        MemoryCardIdentity.Butterfly -> "🦋"
        MemoryCardIdentity.Star -> "⭐"
        MemoryCardIdentity.Puppy -> "🐶"
        MemoryCardIdentity.Flower -> "🌸"
        MemoryCardIdentity.Car -> "🚗"
    }

@StringRes
private fun MemoryCardIdentity.labelResource(): Int = when (this) {
    MemoryCardIdentity.Cow -> R.string.memory_cow
    MemoryCardIdentity.Butterfly -> R.string.memory_butterfly
    MemoryCardIdentity.Star -> R.string.memory_star
    MemoryCardIdentity.Puppy -> R.string.memory_puppy
    MemoryCardIdentity.Flower -> R.string.memory_flower
    MemoryCardIdentity.Car -> R.string.memory_car
}

private const val MISMATCH_REVEAL_MILLIS = 800L
