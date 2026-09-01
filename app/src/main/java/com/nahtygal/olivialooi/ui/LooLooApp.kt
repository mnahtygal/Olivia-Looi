package com.nahtygal.olivialooi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.nahtygal.olivialooi.games.tictactoe.TicTacToeGameMode
import com.nahtygal.olivialooi.ui.games.GamesScreen
import com.nahtygal.olivialooi.ui.games.tictactoe.TicTacToeGameScreen
import com.nahtygal.olivialooi.ui.games.tictactoe.TicTacToeModeScreen
import com.nahtygal.olivialooi.ui.home.LooLooHomeScreen

private enum class LooLooScreen {
    Home,
    Games,
    TicTacToeMode,
    TicTacToeGame,
}

@Composable
fun LooLooApp() {
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
                LooLooScreen.Home -> LooLooScreen.Home
            },
        )
    }

    when (screen) {
        LooLooScreen.Home -> LooLooHomeScreen(
            onGamesClick = { navigateTo(LooLooScreen.Games) },
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
