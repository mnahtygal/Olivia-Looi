package com.nahtygal.olivialooi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LooLooColorScheme = lightColorScheme(
    primary = DeepIndigo,
    onPrimary = SnowWhite,
    secondary = Lavender,
    onSecondary = SnowWhite,
    tertiary = SkyBlue,
    background = IceBlue,
    onBackground = DeepIndigo,
    surface = SnowWhite,
    onSurface = DeepIndigo,
)

@Composable
fun OliviaLooiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LooLooColorScheme,
        typography = Typography,
        content = content,
    )
}
