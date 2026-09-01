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
import com.nahtygal.olivialooi.games.tictactoe.TicTacToeGameMode
import com.nahtygal.olivialooi.ui.apps.AppsScreen
import com.nahtygal.olivialooi.ui.games.GamesScreen
import com.nahtygal.olivialooi.ui.games.tictactoe.TicTacToeGameScreen
import com.nahtygal.olivialooi.ui.games.tictactoe.TicTacToeModeScreen
import com.nahtygal.olivialooi.ui.home.LooLooHomeScreen

private enum class LooLooScreen {
    Home,
    Apps,
    Games,
    TicTacToeMode,
    TicTacToeGame,
}

@Composable
fun LooLooApp() {
    val context = LocalContext.current
    val kidAppLauncher = remember(context) { KidAppLauncher(context) }
    var screenName by rememberSaveable { mutableStateOf(LooLooScreen.Home.name) }
    var gameModeName by rememberSaveable {
        mutableStateOf(TicTacToeGameMode.PersonVsPerson.name)
    }
    val screen = enumValueOf<LooLooScreen>(screenName)
    val gameMode = enumValueOf<TicTacToeGameMode>(gameModeName)

    fun navigateTo(destination: LooLooScreen) {
        screenName = destination.name
    }

    BackHandler(enabled = screen != LooLooScreen.Home) {
        navigateTo(
            when (screen) {
                LooLooScreen.TicTacToeGame -> LooLooScreen.TicTacToeMode
                LooLooScreen.TicTacToeMode -> LooLooScreen.Games
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
    }
}
