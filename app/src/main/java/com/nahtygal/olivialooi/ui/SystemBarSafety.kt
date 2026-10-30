package com.nahtygal.olivialooi.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Keeps interactive content clear of gesture/three-button navigation and landscape cutouts. */
@Composable
internal fun Modifier.navigationSafeContentPadding(
    insets: WindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
        ),
    comfortBottom: Dp = 14.dp,
): Modifier = windowInsetsPadding(insets).padding(bottom = comfortBottom)
