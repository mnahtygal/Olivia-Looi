package com.nahtygal.olivialooi.ui

import com.nahtygal.olivialooi.ui.games.piano.PianoScreen
import com.nahtygal.olivialooi.ui.games.drums.DrumScreen
import com.nahtygal.olivialooi.games.billiards.BilliardsMode
import com.nahtygal.olivialooi.ui.games.billiards.BilliardsModeScreen
import com.nahtygal.olivialooi.ui.games.billiards.BilliardsTableScreen
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.nahtygal.olivialooi.games.puzzles.PuzzlePicture
import com.nahtygal.olivialooi.games.puzzles.PuzzleDifficulty
import com.nahtygal.olivialooi.ui.games.puzzles.PuzzlePictureScreen
import com.nahtygal.olivialooi.ui.games.puzzles.PuzzleDifficultyScreen
import com.nahtygal.olivialooi.ui.games.puzzles.PuzzleBoardScreen
import com.nahtygal.olivialooi.apps.KidAppLaunchResult
import com.nahtygal.olivialooi.apps.KidAppLauncher
import com.nahtygal.olivialooi.games.coloring.ColoringPicture
import com.nahtygal.olivialooi.games.counting.CountingLevel
import com.nahtygal.olivialooi.games.shapes.ShapesMode
import com.nahtygal.olivialooi.ui.games.shapes.ShapesActivityScreen
import com.nahtygal.olivialooi.ui.games.shapes.ShapesAdventureScreen
import com.nahtygal.olivialooi.games.abc.AbcMode
import com.nahtygal.olivialooi.games.memory.MemoryGameSize
import com.nahtygal.olivialooi.games.math.MathLevel
import com.nahtygal.olivialooi.games.spelling.SpellingLevel
import com.nahtygal.olivialooi.games.tictactoe.TicTacToeGameMode
import com.nahtygal.olivialooi.ui.apps.AppsScreen
import com.nahtygal.olivialooi.ui.games.GamesScreen
import com.nahtygal.olivialooi.ui.games.abc.AbcActivityScreen
import com.nahtygal.olivialooi.ui.games.abc.AbcAdventureScreen
import com.nahtygal.olivialooi.ui.games.animals.AnimalSoundsScreen
import com.nahtygal.olivialooi.ui.games.coloring.ColoringCanvasScreen
import com.nahtygal.olivialooi.ui.games.coloring.ColoringPictureScreen
import com.nahtygal.olivialooi.ui.games.counting.CountingGameScreen
import com.nahtygal.olivialooi.ui.games.counting.CountingLevelScreen
import com.nahtygal.olivialooi.ui.games.memory.MemoryMatchGameScreen
import com.nahtygal.olivialooi.ui.games.memory.MemoryMatchModeScreen
import com.nahtygal.olivialooi.ui.games.math.MathGameScreen
import com.nahtygal.olivialooi.ui.games.math.MathLevelScreen
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
    CountingLevel,
    CountingGame,
    MathLevel,
    MathGame,
    AbcAdventure,
    AbcActivity,
    ShapesAdventure,
    ShapesActivity,
    PuzzlePictures,
    PuzzleDifficulty,
    PuzzleBoard,
    Piano,
    Drums,
    BilliardsModes,
    BilliardsTable,
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
    var countingLevelName by rememberSaveable {
        mutableStateOf(CountingLevel.Level1.name)
    }
    var mathLevelName by rememberSaveable { mutableStateOf(MathLevel.SINGLE_DIGIT.name) }
    var mathSessionId by rememberSaveable { mutableIntStateOf(0) }
    var abcModeName by rememberSaveable { mutableStateOf(AbcMode.LEARN.name) }
    var abcSessionId by rememberSaveable { mutableIntStateOf(0) }
    var shapesModeName by rememberSaveable { mutableStateOf(ShapesMode.LEARN.name) }
    var shapesSessionId by rememberSaveable { mutableIntStateOf(0) }
    var puzzlePictureName by rememberSaveable { mutableStateOf(PuzzlePicture.BUTTERFLY.name) }
    var puzzleDifficultyName by rememberSaveable { mutableStateOf(PuzzleDifficulty.EASY.name) }
    var puzzleSessionId by rememberSaveable { mutableIntStateOf(0) }
    var billiardsModeName by rememberSaveable { mutableStateOf(BilliardsMode.FREE_PLAY.name) }
    var billiardsSessionId by rememberSaveable { mutableIntStateOf(0) }
    val screen = enumValueOf<LooLooScreen>(screenName)
    val gameMode = enumValueOf<TicTacToeGameMode>(gameModeName)
    val memoryGameSize = enumValueOf<MemoryGameSize>(memoryGameSizeName)
    val coloringPicture = enumValueOf<ColoringPicture>(coloringPictureName)
    val spellingLevel = enumValueOf<SpellingLevel>(spellingLevelName)
    val countingLevel = enumValueOf<CountingLevel>(countingLevelName)
    val mathLevel = enumValueOf<MathLevel>(mathLevelName)
    val abcMode = enumValueOf<AbcMode>(abcModeName)

    fun navigateTo(destination: LooLooScreen) {
        screenName = destination.name
    }

    BackHandler(enabled = screen != LooLooScreen.Home && screen != LooLooScreen.PuzzleBoard) {
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
                LooLooScreen.CountingGame -> LooLooScreen.CountingLevel
                LooLooScreen.CountingLevel -> LooLooScreen.Games
                LooLooScreen.MathGame -> LooLooScreen.MathLevel
                LooLooScreen.MathLevel -> LooLooScreen.Games
                LooLooScreen.BilliardsTable -> LooLooScreen.BilliardsModes
                LooLooScreen.BilliardsModes -> LooLooScreen.Games
                LooLooScreen.Drums -> LooLooScreen.Games
                LooLooScreen.Piano -> LooLooScreen.Games
                LooLooScreen.PuzzleBoard -> LooLooScreen.PuzzleDifficulty
                LooLooScreen.PuzzleDifficulty -> LooLooScreen.PuzzlePictures
                LooLooScreen.PuzzlePictures -> LooLooScreen.Games
                LooLooScreen.ShapesActivity -> LooLooScreen.ShapesAdventure
                LooLooScreen.ShapesAdventure -> LooLooScreen.Games
                LooLooScreen.AbcActivity -> LooLooScreen.AbcAdventure
                LooLooScreen.AbcAdventure -> LooLooScreen.Games
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
            onCountingClick = { navigateTo(LooLooScreen.CountingLevel) },
            onMathClick = { navigateTo(LooLooScreen.MathLevel) },
            onBilliardsClick = { navigateTo(LooLooScreen.BilliardsModes) },
            onDrumsClick = { navigateTo(LooLooScreen.Drums) },
            onPianoClick = { navigateTo(LooLooScreen.Piano) },
            onPuzzlesClick = { navigateTo(LooLooScreen.PuzzlePictures) },
            onShapesClick = { navigateTo(LooLooScreen.ShapesAdventure) },
            onAbcClick = { navigateTo(LooLooScreen.AbcAdventure) },
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

        LooLooScreen.CountingLevel -> CountingLevelScreen(
            onLevelSelected = { selectedLevel ->
                countingLevelName = selectedLevel.name
                navigateTo(LooLooScreen.CountingGame)
            },
            onGamesClick = { navigateTo(LooLooScreen.Games) },
        )

        LooLooScreen.CountingGame -> CountingGameScreen(
            level = countingLevel,
            onPickAnotherLevelClick = { navigateTo(LooLooScreen.CountingLevel) },
            onBackToGamesClick = { navigateTo(LooLooScreen.Games) },
        )

        LooLooScreen.MathLevel -> MathLevelScreen(
            onLevelSelected = { selectedLevel ->
                mathLevelName = selectedLevel.name
                mathSessionId += 1
                navigateTo(LooLooScreen.MathGame)
            },
            onGamesClick = { navigateTo(LooLooScreen.Games) },
        )

        LooLooScreen.MathGame -> MathGameScreen(
            level = mathLevel,
            sessionId = mathSessionId,
            onPickAnotherLevelClick = { navigateTo(LooLooScreen.MathLevel) },
            onBackToGamesClick = { navigateTo(LooLooScreen.Games) },
        )

        LooLooScreen.BilliardsModes -> BilliardsModeScreen(
            onMode = {
                billiardsModeName = it.name
                billiardsSessionId += 1
                navigateTo(LooLooScreen.BilliardsTable)
            },
            onGamesClick = { navigateTo(LooLooScreen.Games) },
        )
        LooLooScreen.BilliardsTable -> BilliardsTableScreen(
            mode = enumValueOf<BilliardsMode>(billiardsModeName),
            sessionId = billiardsSessionId,
            onMode = { billiardsModeName = it.name; billiardsSessionId += 1 },
            onModesClick = { navigateTo(LooLooScreen.BilliardsModes) },
            onGamesClick = { navigateTo(LooLooScreen.Games) },
        )

        LooLooScreen.Drums -> DrumScreen(onGamesClick = { navigateTo(LooLooScreen.Games) })

        LooLooScreen.Piano -> PianoScreen(
            onGamesClick = { navigateTo(LooLooScreen.Games) },
        )

        LooLooScreen.PuzzlePictures -> PuzzlePictureScreen(
            onPictureSelected = { selected ->
                puzzlePictureName = selected.name
                navigateTo(LooLooScreen.PuzzleDifficulty)
            },
            onGamesClick = { navigateTo(LooLooScreen.Games) },
        )

        LooLooScreen.PuzzleDifficulty -> PuzzleDifficultyScreen(
            picture = enumValueOf<PuzzlePicture>(puzzlePictureName),
            onDifficultySelected = { selected ->
                puzzleDifficultyName = selected.name
                puzzleSessionId += 1
                navigateTo(LooLooScreen.PuzzleBoard)
            },
            onPicturesClick = { navigateTo(LooLooScreen.PuzzlePictures) },
        )

        LooLooScreen.PuzzleBoard -> PuzzleBoardScreen(
            picture = enumValueOf<PuzzlePicture>(puzzlePictureName),
            difficulty = enumValueOf<PuzzleDifficulty>(puzzleDifficultyName),
            sessionId = puzzleSessionId,
            onChoosePieces = { navigateTo(LooLooScreen.PuzzleDifficulty) },
            onPickAnotherPuzzle = { navigateTo(LooLooScreen.PuzzlePictures) },
            onGamesClick = { navigateTo(LooLooScreen.Games) },
        )

        LooLooScreen.ShapesAdventure -> ShapesAdventureScreen(
            onModeSelected = { selectedMode ->
                shapesModeName = selectedMode.name
                shapesSessionId += 1
                navigateTo(LooLooScreen.ShapesActivity)
            },
            onGamesClick = { navigateTo(LooLooScreen.Games) },
        )

        LooLooScreen.ShapesActivity -> ShapesActivityScreen(
            mode = enumValueOf<ShapesMode>(shapesModeName),
            sessionId = shapesSessionId,
            onPickAnotherAdventureClick = { navigateTo(LooLooScreen.ShapesAdventure) },
            onBackToGamesClick = { navigateTo(LooLooScreen.Games) },
        )

        LooLooScreen.AbcAdventure -> AbcAdventureScreen(
            onModeSelected = { selectedMode ->
                abcModeName = selectedMode.name
                abcSessionId += 1
                navigateTo(LooLooScreen.AbcActivity)
            },
            onGamesClick = { navigateTo(LooLooScreen.Games) },
        )

        LooLooScreen.AbcActivity -> AbcActivityScreen(
            mode = abcMode,
            sessionId = abcSessionId,
            onPickAnotherAdventureClick = { navigateTo(LooLooScreen.AbcAdventure) },
            onBackToGamesClick = { navigateTo(LooLooScreen.Games) },
        )
    }
}
