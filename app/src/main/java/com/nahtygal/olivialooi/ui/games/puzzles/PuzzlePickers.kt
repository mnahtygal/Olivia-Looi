package com.nahtygal.olivialooi.ui.games.puzzles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.games.puzzles.PuzzleCatalog
import com.nahtygal.olivialooi.games.puzzles.PuzzleDifficulty
import com.nahtygal.olivialooi.games.puzzles.PuzzlePicture
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.games.WinterNavigationButton
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.SnowWhite

@Composable
fun PuzzlePictureScreen(onPictureSelected: (PuzzlePicture) -> Unit, onGamesClick: () -> Unit) {
    PickerFrame(stringResource(R.string.puzzle_pick_picture)) { tablet ->
        val columns = if (tablet) 3 else 2
        PuzzleCatalog.pictures.chunked(columns).forEach { pictures ->
            Row(Modifier.widthIn(max = 940.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pictures.forEach { picture ->
                    val description = stringResource(R.string.puzzle_picture_description, picture.displayName)
                    Card(
                        onClick = { onPictureSelected(picture) },
                        modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = description },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SnowWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    ) {
                        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            PuzzleImage(picture, Modifier.fillMaxWidth().aspectRatio(1f))
                            Text(picture.displayName, Modifier.padding(6.dp), color = DeepIndigo, fontSize = if (tablet) 24.sp else 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        WinterNavigationButton(R.string.back_to_games, onGamesClick)
    }
}

@Composable
fun PuzzleDifficultyScreen(picture: PuzzlePicture, onDifficultySelected: (PuzzleDifficulty) -> Unit, onPicturesClick: () -> Unit) {
    PickerFrame(stringResource(R.string.puzzle_choose_difficulty)) { tablet ->
        Text(picture.displayName, color = SnowWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (tablet) {
            Row(Modifier.widthIn(max = 940.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PuzzleDifficulty.entries.forEach { difficulty ->
                    DifficultyCard(picture, difficulty, { onDifficultySelected(difficulty) }, Modifier.weight(1f))
                }
            }
        } else {
            PuzzleDifficulty.entries.forEach { difficulty ->
                DifficultyCard(picture, difficulty, { onDifficultySelected(difficulty) }, Modifier.widthIn(max = 360.dp).fillMaxWidth())
            }
        }
        PuzzleAction(stringResource(R.string.puzzle_back_pictures), onClick = onPicturesClick)
    }
}

@Composable
private fun DifficultyCard(picture: PuzzlePicture, difficulty: PuzzleDifficulty, onClick: () -> Unit, modifier: Modifier) {
    val description = pluralStringResource(R.plurals.puzzle_difficulty_description, difficulty.pieceCount, difficulty.pieceCount, difficulty.displayName)
    Card(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SnowWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.foundation.layout.Box(Modifier.widthIn(max = 210.dp).fillMaxWidth().aspectRatio(1f)) {
                PuzzleImage(picture, Modifier.fillMaxSize())
                PuzzleGrid(difficulty, Modifier.fillMaxSize())
            }
            Text(pluralStringResource(R.plurals.puzzle_piece_count, difficulty.pieceCount, difficulty.pieceCount), color = DeepIndigo, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text(difficulty.displayName, color = DeepIndigo, fontSize = 22.sp)
        }
    }
}

@Composable
private fun PickerFrame(title: String, content: @Composable ColumnScope.(Boolean) -> Unit) {
    WinterGamesBackground(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val tablet = maxWidth >= 600.dp
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(title, color = SnowWhite, fontSize = if (tablet) 36.sp else 29.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                content(tablet)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
internal fun PuzzleAction(label: String, modifier: Modifier = Modifier, description: String = label, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp).semantics { contentDescription = description },
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SnowWhite, contentColor = DeepIndigo),
        border = BorderStroke(1.dp, Color(0xFFD4DDF5)),
    ) { Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
}
