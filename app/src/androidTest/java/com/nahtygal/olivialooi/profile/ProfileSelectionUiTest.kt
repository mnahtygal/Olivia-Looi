package com.nahtygal.olivialooi.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nahtygal.olivialooi.screenshots.CaptureActivity
import com.nahtygal.olivialooi.ui.LooLooApp
import com.nahtygal.olivialooi.ui.theme.OliviaLooiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

class ProfileSelectionUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<CaptureActivity>()

    @Test
    fun chooserSwitchesVisibleProfileWithoutRestartingActivity() {
        val repository = FakeRepository(KidProfile.OLIVIA)
        val originalActivity = compose.activity
        compose.setContent {
            OliviaLooiTheme {
                LooLooApp(profileRepository = repository)
            }
        }

        compose.onNodeWithText("Hi, Olivia!").assertIsDisplayed()
        compose.onNodeWithText("Switch Player").performClick()
        compose.onNodeWithText("Who's playing with LooLoo?").assertIsDisplayed()
        compose.onNodeWithContentDescription("Play as Olivia").assertIsDisplayed()
        compose.onNodeWithContentDescription("Play as Eliana").assertIsDisplayed().performClick()

        compose.onNodeWithText("Hi, Eliana!").assertIsDisplayed()
        assertEquals(KidProfile.ELIANA, repository.selectedProfile)
        assertSame(originalActivity, compose.activity)

        compose.onNodeWithText("Switch Player").performClick()
        compose.onNodeWithContentDescription("Play as Olivia").performClick()

        compose.onNodeWithText("Hi, Olivia!").assertIsDisplayed()
        assertEquals(KidProfile.OLIVIA, repository.selectedProfile)
        assertSame(originalActivity, compose.activity)
    }

    private class FakeRepository(initialProfile: KidProfile) : KidProfileRepository {
        override var selectedProfile: KidProfile = initialProfile
            private set

        override fun selectProfile(profile: KidProfile) {
            selectedProfile = profile
        }
    }
}
