package com.nahtygal.olivialooi.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.nahtygal.olivialooi.games.animals.AnimalPlayMode
import com.nahtygal.olivialooi.games.puzzles.PuzzleDifficulty
import com.nahtygal.olivialooi.games.puzzles.PuzzlePicture
import com.nahtygal.olivialooi.screenshots.CaptureActivity
import com.nahtygal.olivialooi.ui.games.animals.AnimalSoundsScreen
import com.nahtygal.olivialooi.ui.games.puzzles.PuzzleDifficultyScreen
import com.nahtygal.olivialooi.ui.stories.StoryTimeScreen
import com.nahtygal.olivialooi.ui.theme.OliviaLooiTheme
import org.junit.Rule
import org.junit.Test

class ProfileAwareEntryUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<CaptureActivity>()

    @Test
    fun puzzleEntryShowsProfileRecommendationWithoutRemovingSizes() {
        compose.setContent {
            OliviaLooiTheme {
                PuzzleDifficultyScreen(
                    picture = PuzzlePicture.BUTTERFLY,
                    onDifficultySelected = {},
                    onPicturesClick = {},
                    preferredDifficulty = PuzzleDifficulty.MEDIUM,
                )
            }
        }

        compose.onNodeWithText("LooLoo’s pick").performScrollTo().assertIsDisplayed()
        listOf("6 Pieces", "9 Pieces", "12 Pieces", "16 Pieces", "20 Pieces", "25 Pieces")
            .forEach { label -> compose.onNodeWithText(label).assertExists() }
    }

    @Test
    fun StoryEntryShowsProfileFeatureWhileKeepingLibraryAvailable() {
        compose.setContent {
            OliviaLooiTheme {
                StoryTimeScreen(
                    onHome = {},
                    featuredStoryId = "eliana_barnyard_day",
                    featuredForDisplayName = "Eliana",
                )
            }
        }

        compose.onNodeWithText("Featured for Eliana").assertIsDisplayed()
        compose.onNodeWithText("Eliana’s Barnyard Morning").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pick a story!").assertExists()
    }

    @Test
    fun AnimalSoundsEntryUsesProvidedModeWithoutRemovingModeControls() {
        compose.setContent {
            OliviaLooiTheme {
                AnimalSoundsScreen(
                    onGamesClick = {},
                    initialMode = AnimalPlayMode.WHO_MAKES_THIS_SOUND,
                )
            }
        }

        compose.onNodeWithText("Listen! Who makes this sound?").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Free Play").assertExists()
        compose.onNodeWithText("Who Makes This Sound?").assertExists()
        compose.onNodeWithText("Find the Animal").assertExists()
    }
}
