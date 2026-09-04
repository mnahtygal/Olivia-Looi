package com.nahtygal.olivialooi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.nahtygal.olivialooi.apps.KidAppLaunchResult
import com.nahtygal.olivialooi.apps.KidAppLauncher
import com.nahtygal.olivialooi.games.coloring.ColoringPicture
import com.nahtygal.olivialooi.games.memory.MemoryGameSize
import com.nahtygal.olivialooi.games.spelling.SpellingLevel
import com.nahtygal.olivialooi.games.tictactoe.TicTacToeGameMode
import com.nahtygal.olivialooi.ui.apps.AppsScreen
import com.nahtygal.olivialooi.ui.games.GamesScreen
import com.nahtygal.olivialooi.ui.games.animals.AnimalSoundsScreen
import com.nahtygal.olivialooi.ui.games.coloring.ColoringCanvasScreen
import com.nahtygal.olivialooi.ui.games.coloring.ColoringPictureScreen
import com.nahtygal.olivialooi.ui.games.memory.MemoryMatchGameScreen
import com.nahtygal.olivialooi.ui.games.memory.MemoryMatchModeScreen
import com.nahtygal.olivialooi.ui.games.spelling.SpeakAndSpellGameScreen
import com.nahtygal.olivialooi.ui.games.spelling.SpeakAndSpellLevelScreen
import com.nahtygal.olivialooi.ui.games.tictactoe.TicTacToeGameScreen
import com.nahtygal.olivialooi.ui.games.tictactoe.TicTacToeModeScreen
import com.nahtygal.olivialooi.ui.home.LooLooHomeScreen

private enum class LooLooScreen {
    Home,
    Apps,
    Games,
    TicTacToeMode,
    TicTacToeGame,
    MemoryMatchMode,
    MemoryMatchGame,
    ColoringPictures,
    ColoringCanvas,
    SpeakAndSpellLevel,
    SpeakAndSpellGame,
    AnimalSounds,
}

@Composable
fun LooLooApp() {
    val context = LocalContext.current
    val kidAppLauncher = remember(context) { KidAppLauncher(context) }
    var screenName by rememberSaveable { mutableStateOf(LooLooScreen.Home.name) }
    var gameModeName by rememberSaveable {
        mutableStateOf(TicTacToeGameMode.PersonVsPerson.name)
    }
    var memoryGameSizeName by rememberSaveable {
        mutableStateOf(MemoryGameSize.Little.name)
    }
    var coloringPictureName by rememberSaveable {
        mutableStateOf(ColoringPicture.Butterfly.name)
    }
    var spellingLevelName by rememberSaveable {
        mutableStateOf(SpellingLevel.Level1.name)
    }
    val screen = enumValueOf<LooLooScreen>(screenName)
    val gameMode = enumValueOf<TicTacToeGameMode>(gameModeName)
    val memoryGameSize = enumValueOf<MemoryGameSize>(memoryGameSizeName)
    val coloringPicture = enumValueOf<ColoringPicture>(coloringPictureName)
    val spellingLevel = enumValueOf<SpellingLevel>(spellingLevelName)

    fun navigateTo(destination: LooLooScreen) {
        screenName = destination.name
    }

    BackHandler(enabled = screen != LooLooScreen.Home) {
        navigateTo(
            when (screen) {
                LooLooScreen.TicTacToeGame -> LooLooScreen.TicTacToeMode
                LooLooScreen.TicTacToeMode -> LooLooScreen.Games
                LooLooScreen.MemoryMatchGame -> LooLooScreen.MemoryMatchMode
                LooLooScreen.MemoryMatchMode -> LooLooScreen.Games
                LooLooScreen.ColoringCanvas -> LooLooScreen.ColoringPictures
                LooLooScreen.ColoringPictures -> LooLooScreen.Games
                LooLooScreen.SpeakAndSpellGame -> LooLooScreen.SpeakAndSpellLevel
                LooLooScreen.SpeakAndSpellLevel -> LooLooScreen.Games
                LooLooScreen.AnimalSounds -> LooLooScreen.Games
                LooLooScreen.Games -> LooLooScreen.Home
                LooLooScreen.Apps -> LooLooScreen.Home
                LooLooScreen.Home -> LooLooScreen.Home
            },
        )
    }

    when (screen) {
        LooLooScreen.Home -> LooLooHomeScreen(
            onGamesClick = { navigateTo(LooLooScreen.Games) },
            onAppsClick = { navigateTo(LooLooScreen.Apps) },
        )

        LooLooScreen.Apps -> AppsScreen(
            onLaunchApp = { app ->
                kidAppLauncher.launch(app) == KidAppLaunchResult.Launched
            },
            onHomeClick = { navigateTo(LooLooScreen.Home) },
        )

        LooLooScreen.Games -> GamesScreen(
            onTicTacToeClick = { navigateTo(LooLooScreen.TicTacToeMode) },
            onMemoryMatchClick = { navigateTo(LooLooScreen.MemoryMatchMode) },
            onColoringClick = { navigateTo(LooLooScreen.ColoringPictures) },
            onSpeakAndSpellClick = { navigateTo(LooLooScreen.SpeakAndSpellLevel) },
            onAnimalSoundsClick = { navigateTo(LooLooScreen.AnimalSounds) },
            onHomeClick = { navigateTo(LooLooScreen.Home) },
        )

        LooLooScreen.TicTacToeMode -> TicTacToeModeScreen(
            onModeSelected = { selectedMode ->
                gameModeName = selectedMode.name
                navigateTo(LooLooScreen.TicTacToeGame)
            },
            onHomeClick = { navigateTo(LooLooScreen.Home) },
        )

        LooLooScreen.TicTacToeGame -> TicTacToeGameScreen(
            gameMode = gameMode,
            onChangeModeClick = { navigateTo(LooLooScreen.TicTacToeMode) },
            onHomeClick = { navigateTo(LooLooScreen.Home) },
        )

        LooLooScreen.MemoryMatchMode -> MemoryMatchModeScreen(
            onSizeSelected = { selectedSize ->
                memoryGameSizeName = selectedSize.name
                navigateTo(LooLooScreen.MemoryMatchGame)
            },
            onGamesClick = { navigateTo(LooLooScreen.Games) },
        )

        LooLooScreen.MemoryMatchGame -> MemoryMatchGameScreen(
            gameSize = memoryGameSize,
            onPickAnotherGameClick = { navigateTo(LooLooScreen.MemoryMatchMode) },
            onBackToGamesClick = { navigateTo(LooLooScreen.Games) },
        )

        LooLooScreen.ColoringPictures -> ColoringPictureScreen(
            onPictureSelected = { selectedPicture ->
                coloringPictureName = selectedPicture.name
                navigateTo(LooLooScreen.ColoringCanvas)
            },
            onGamesClick = { navigateTo(LooLooScreen.Games) },
        )

        LooLooScreen.ColoringCanvas -> ColoringCanvasScreen(
            picture = coloringPicture,
            onNewPictureClick = { navigateTo(LooLooScreen.ColoringPictures) },
            onBackToGamesClick = { navigateTo(LooLooScreen.Games) },
        )

        LooLooScreen.SpeakAndSpellLevel -> SpeakAndSpellLevelScreen(
            onLevelSelected = { selectedLevel ->
                spellingLevelName = selectedLevel.name
                navigateTo(LooLooScreen.SpeakAndSpellGame)
            },
            onGamesClick = { navigateTo(LooLooScreen.Games) },
        )

        LooLooScreen.SpeakAndSpellGame -> SpeakAndSpellGameScreen(
            level = spellingLevel,
            onPickAnotherLevelClick = { navigateTo(LooLooScreen.SpeakAndSpellLevel) },
            onBackToGamesClick = { navigateTo(LooLooScreen.Games) },
        )

        LooLooScreen.AnimalSounds -> AnimalSoundsScreen(
            onGamesClick = { navigateTo(LooLooScreen.Games) },
        )
    }
}
