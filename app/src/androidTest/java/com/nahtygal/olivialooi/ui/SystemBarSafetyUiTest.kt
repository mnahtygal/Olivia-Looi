package com.nahtygal.olivialooi.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class SystemBarSafetyUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun safeContentUsesHorizontalBottomInsetsAndComfortMarginOnce() {
        compose.setContent {
            Box(
                Modifier
                    .size(200.dp)
                    .navigationSafeContentPadding(
                        insets = WindowInsets(left = 10.dp, right = 20.dp, bottom = 30.dp),
                        comfortBottom = 14.dp,
                    ),
            ) {
                Box(Modifier.fillMaxSize().testTag("safeContent"))
            }
        }

        compose.onNodeWithTag("safeContent")
            .assertPositionInRootIsEqualTo(10.dp, 0.dp)
            .assertWidthIsEqualTo(170.dp)
            .assertHeightIsEqualTo(156.dp)
    }
}
