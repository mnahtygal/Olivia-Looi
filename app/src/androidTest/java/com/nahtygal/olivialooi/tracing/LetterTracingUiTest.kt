package com.nahtygal.olivialooi.tracing

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.espresso.Espresso.pressBack
import androidx.compose.ui.unit.dp
import com.nahtygal.olivialooi.profile.KidProfile
import com.nahtygal.olivialooi.ui.LooLooApp
import com.nahtygal.olivialooi.ui.games.tracing.LetterTracingScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LetterTracingUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun traceUndoAndNextLetterRemainManual() {
        compose.setContent { LetterTracingScreen(onGamesClick = {}, onHomeClick = {}) }

        compose.onNodeWithText("My Name").performClick()
        compose.onNodeWithText("Letter O • 1 of 6").assertIsDisplayed()
        compose.onNodeWithContentDescription("Trace the dotted letter O")
            .performTouchInput { swipe(centerLeft, centerRight, 300L) }
        compose.onNodeWithText("Undo").assertIsEnabled()
        compose.onNodeWithText("Letter O • 1 of 6").assertIsDisplayed()
        compose.onNodeWithText("Previous").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Next").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Letter L • 2 of 6").assertTextContains("Letter L")
        compose.onNodeWithText("Previous").assertIsEnabled().performClick()
        compose.onNodeWithContentDescription("Trace the dotted letter O")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "1 completed strokes"))
    }

    @Test
    fun writingMenuOpensAlphabetAtAWithManualNavigation() {
        compose.setContent { LetterTracingScreen(onGamesClick = {}, onHomeClick = {}) }

        compose.onNodeWithText("Let’s Practice Writing!").assertIsDisplayed()
        compose.onNodeWithText("Uppercase ABCs").performClick()
        compose.onNodeWithText("Letter 1 of 26").assertIsDisplayed()
        compose.onNodeWithContentDescription("Trace the dotted letter A").assertIsDisplayed()
        compose.onNodeWithText("Previous").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Next").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Letter 2 of 26").assertIsDisplayed()
        compose.onNodeWithText("Previous").performClick()
        compose.onNodeWithText("Letter 1 of 26").assertIsDisplayed()
    }

    @Test
    fun navigationAndHomeIconFitInsideConstrainedPortraitViewport() {
        compose.setContent {
            LetterTracingScreen(
                onGamesClick = {},
                onHomeClick = {},
                modifier = Modifier.size(width = 412.dp, height = 780.dp),
            )
        }

        compose.onNodeWithText("Uppercase ABCs").performClick()
        compose.onNodeWithText("Previous").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Next").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithTag("writingHomeIcon", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Home").assertIsDisplayed()

        val viewport = compose.onNodeWithTag("tracingPracticeViewport").fetchSemanticsNode().boundsInRoot
        val previous = compose.onNodeWithText("Previous").fetchSemanticsNode().boundsInRoot
        val next = compose.onNodeWithText("Next").fetchSemanticsNode().boundsInRoot
        assertTrue(previous.top >= viewport.top && previous.bottom <= viewport.bottom)
        assertTrue(next.top >= viewport.top && next.bottom <= viewport.bottom)

    }

    @Test
    fun lowercaseCompletionProvidesEveryRequiredExitAndPracticeChoice() {
        compose.setContent { LetterTracingScreen(onGamesClick = {}, onHomeClick = {}) }
        compose.onNodeWithText("Lowercase abc's").performClick()

        ('a'..'z').forEachIndexed { index, letter ->
            compose.onNodeWithContentDescription("Trace the dotted letter $letter")
                .performTouchInput { swipe(centerLeft, centerRight, 100L) }
            if (index < 25) compose.onNodeWithText("Next").performClick()
        }

        compose.onNodeWithText("Hooray, Olivia!").assertIsDisplayed()
        compose.onNodeWithText("You practiced all your lowercase letters! You’re becoming a wonderful writer!")
            .assertIsDisplayed()
        compose.onNodeWithText("Practice Lowercase Again").assertIsDisplayed()
        compose.onNodeWithText("Uppercase ABCs").assertIsDisplayed()
        compose.onNodeWithText("Practice My Name").assertIsDisplayed()
        compose.onNodeWithText("Back to Writing Menu").assertIsDisplayed()
        compose.onNodeWithText("Home").assertIsDisplayed()
    }

    @Test
    fun lowercaseModeRetainsStrokesAndControlsFitConstrainedPortraitViewport() {
        compose.setContent {
            LetterTracingScreen(
                onGamesClick = {},
                onHomeClick = {},
                modifier = Modifier.size(width = 412.dp, height = 780.dp),
            )
        }

        compose.onNodeWithText("Lowercase abc's").assertIsDisplayed().performClick()
        compose.onNodeWithText("Letter 1 of 26").assertIsDisplayed()
        val canvas = compose.onNodeWithContentDescription("Trace the dotted letter a")
        repeat(2) { canvas.performTouchInput { swipe(centerLeft, centerRight, 150L) } }
        compose.onNodeWithText("Previous").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Next").assertIsDisplayed().assertIsEnabled().performClick()
        compose.onNodeWithContentDescription("Trace the dotted letter b").assertIsDisplayed()
        compose.onNodeWithText("Previous").performClick()
        compose.onNodeWithContentDescription("Trace the dotted letter a")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "2 completed strokes"))

        val viewport = compose.onNodeWithTag("tracingPracticeViewport").fetchSemanticsNode().boundsInRoot
        val previous = compose.onNodeWithText("Previous").fetchSemanticsNode().boundsInRoot
        val next = compose.onNodeWithText("Next").fetchSemanticsNode().boundsInRoot
        assertTrue(previous.top >= viewport.top && previous.bottom <= viewport.bottom)
        assertTrue(next.top >= viewport.top && next.bottom <= viewport.bottom)

        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Uppercase ABCs").performClick()
        compose.onNodeWithContentDescription("Trace the dotted letter A")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "0 completed strokes"))
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Lowercase abc's").performClick()
        compose.onNodeWithContentDescription("Trace the dotted letter a")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "2 completed strokes"))
    }

    @Test
    fun threeSeparateGesturesRemainCommittedAcrossRecompositionAndMenuNavigation() {
        compose.setContent { LetterTracingScreen(onGamesClick = {}, onHomeClick = {}) }
        compose.onNodeWithText("Uppercase ABCs").performClick()
        val canvas = compose.onNodeWithContentDescription("Trace the dotted letter A")

        repeat(3) { canvas.performTouchInput { swipe(centerLeft, centerRight, 150L) } }

        canvas.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "3 completed strokes"),
        )
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Uppercase ABCs").performClick()
        compose.onNodeWithContentDescription("Trace the dotted letter A")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "3 completed strokes"))
    }

    @Test
    fun homeAndBackAreAvailableInBothPracticeModesAndEarlyExitDoesNotCelebrate() {
        var homeClicks = 0
        compose.setContent {
            LetterTracingScreen(onGamesClick = {}, onHomeClick = { homeClicks += 1 })
        }

        compose.onNodeWithText("Uppercase ABCs").performClick()
        compose.onNodeWithText("Home").assertIsDisplayed()
        compose.onNodeWithText("Back").assertIsDisplayed()
        compose.onNodeWithContentDescription("Trace the dotted letter A")
            .performTouchInput { swipe(centerLeft, centerRight, 200L) }
        compose.onNodeWithText("Hooray, Olivia!").assertDoesNotExist()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Let’s Practice Writing!").assertIsDisplayed()
        compose.onNodeWithText("Uppercase ABCs").performClick()
        compose.onNodeWithText("Undo").assertIsEnabled()
        compose.onNodeWithText("Home").performClick()
        compose.runOnIdle { assert(homeClicks == 1) }

        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("My Name").performClick()
        compose.onNodeWithText("Home").assertIsDisplayed()
        compose.onNodeWithText("Back").assertIsDisplayed()
    }

    @Test
    fun systemBackMatchesVisibleBackAndReturnsToWritingMenu() {
        compose.setContent { LetterTracingScreen(onGamesClick = {}, onHomeClick = {}) }
        compose.onNodeWithText("Uppercase ABCs").performClick()

        pressBack()

        compose.onNodeWithText("Let’s Practice Writing!").assertIsDisplayed()
    }

    @Test
    fun nameCelebrationProvidesHomeNavigation() {
        var homeClicked = false
        compose.setContent {
            LetterTracingScreen(onGamesClick = {}, onHomeClick = { homeClicked = true })
        }
        compose.onNodeWithText("My Name").performClick()
        val letters = listOf('O', 'L', 'I', 'V', 'I', 'A')
        letters.forEachIndexed { index, letter ->
            compose.onNodeWithContentDescription("Trace the dotted letter $letter")
                .performTouchInput { swipe(centerLeft, centerRight, 150L) }
            if (index < letters.lastIndex) compose.onNodeWithText("Next").performClick()
        }

        compose.onNodeWithText("Hooray, Olivia!").assertIsDisplayed()
        compose.onNodeWithText("Home").assertIsDisplayed().performClick()
        compose.runOnIdle { assert(homeClicked) }
    }

    @Test
    fun homeControlUsesActualAppHomeRoute() {
        compose.setContent { LooLooApp(activeKidProfile = KidProfile.OLIVIA) }
        compose.onNodeWithText("Games").performClick()
        compose.onNodeWithContentDescription("Open Letter Tracing").performScrollTo().performClick()
        compose.onNodeWithText("Uppercase ABCs").performClick()

        compose.onNodeWithText("Home").performClick()

        compose.onNodeWithText("Hi, Olivia!").assertIsDisplayed()
    }
}
