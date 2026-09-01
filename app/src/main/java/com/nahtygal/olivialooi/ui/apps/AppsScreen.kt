package com.nahtygal.olivialooi.ui.apps

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.apps.KidApp
import com.nahtygal.olivialooi.apps.KidAppIcon
import com.nahtygal.olivialooi.apps.KidApps
import com.nahtygal.olivialooi.ui.games.WinterGamesBackground
import com.nahtygal.olivialooi.ui.games.WinterNavigationButton
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.SnowWhite

@Composable
fun AppsScreen(
    onLaunchApp: (KidApp) -> Boolean,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var unavailableAppId by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = unavailableAppId != null) {
        unavailableAppId = null
    }

    WinterGamesBackground(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            val tabletLayout = maxWidth >= 600.dp
            if (unavailableAppId != null) {
                MissingAppContent(
                    onBackToAppsClick = { unavailableAppId = null },
                    onHomeClick = onHomeClick,
                    tabletLayout = tabletLayout,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.apps_heading),
                        color = SnowWhite,
                        fontSize = if (tabletLayout) 40.sp else 32.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.apps_subtitle),
                        color = SnowWhite.copy(alpha = 0.92f),
                        fontSize = if (tabletLayout) 20.sp else 17.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(if (tabletLayout) 24.dp else 14.dp))

                    if (tabletLayout) {
                        Row(
                            modifier = Modifier.widthIn(max = 720.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            KidApps.all.forEach { app ->
                                KidAppCard(
                                    app = app,
                                    tabletLayout = true,
                                    onClick = {
                                        if (!onLaunchApp(app)) unavailableAppId = app.id
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            KidApps.all.forEach { app ->
                                KidAppCard(
                                    app = app,
                                    tabletLayout = false,
                                    onClick = {
                                        if (!onLaunchApp(app)) unavailableAppId = app.id
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(if (tabletLayout) 24.dp else 14.dp))
                    WinterNavigationButton(
                        labelResource = R.string.back_to_home,
                        onClick = onHomeClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun KidAppCard(
    app: KidApp,
    tabletLayout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(app.accessibilityDescriptionResource)
    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = if (tabletLayout) 360.dp else 150.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        shape = RoundedCornerShape(if (tabletLayout) 30.dp else 24.dp),
        colors = CardDefaults.cardColors(containerColor = SnowWhite.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
    ) {
        if (tabletLayout) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                KidAppIcon(
                    icon = app.icon,
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .aspectRatio(1f),
                )
                Spacer(modifier = Modifier.height(20.dp))
                KidAppName(app)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KidAppIcon(
                    icon = app.icon,
                    modifier = Modifier.size(116.dp),
                )
                Text(
                    text = stringResource(app.displayNameResource),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 18.dp),
                    color = DeepIndigo,
                    fontSize = 25.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Start,
                )
                Text(
                    text = stringResource(R.string.open_game_mode_symbol),
                    color = DeepIndigo.copy(alpha = 0.72f),
                    fontSize = 34.sp,
                )
            }
        }
    }
}

@Composable
private fun KidAppName(app: KidApp) {
    Text(
        text = stringResource(app.displayNameResource),
        color = DeepIndigo,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun KidAppIcon(icon: KidAppIcon, modifier: Modifier = Modifier) {
    when (icon) {
        KidAppIcon.Video -> YouTubeKidsGlyph(modifier)
        KidAppIcon.Magic -> DisneyPlusGlyph(modifier)
    }
}

@Composable
private fun YouTubeKidsGlyph(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(listOf(Color(0xFFFFD9E5), Color(0xFFFFF0C8))),
                shape = RoundedCornerShape(28.dp),
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color(0xFFFF345D),
                topLeft = Offset(size.width * 0.08f, size.height * 0.20f),
                size = Size(size.width * 0.84f, size.height * 0.60f),
                cornerRadius = CornerRadius(size.minDimension * 0.18f),
            )
            val play = Path().apply {
                moveTo(size.width * 0.42f, size.height * 0.34f)
                lineTo(size.width * 0.42f, size.height * 0.66f)
                lineTo(size.width * 0.68f, size.height * 0.50f)
                close()
            }
            drawPath(play, SnowWhite)
        }
    }
}

@Composable
private fun DisneyPlusGlyph(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(listOf(Color(0xFF172C84), Color(0xFF5E68D8))),
                shape = RoundedCornerShape(28.dp),
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = Color(0xFFA9E9FF),
                startAngle = 198f,
                sweepAngle = 145f,
                useCenter = false,
                topLeft = Offset(size.width * 0.08f, size.height * 0.05f),
                size = Size(size.width * 0.84f, size.height * 0.62f),
                style = Stroke(width = size.minDimension * 0.055f, cap = StrokeCap.Round),
            )
            drawLine(
                color = SnowWhite,
                start = Offset(size.width * 0.24f, size.height * 0.68f),
                end = Offset(size.width * 0.76f, size.height * 0.68f),
                strokeWidth = size.minDimension * 0.07f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = SnowWhite,
                start = Offset(size.width * 0.50f, size.height * 0.55f),
                end = Offset(size.width * 0.50f, size.height * 0.81f),
                strokeWidth = size.minDimension * 0.07f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun MissingAppContent(
    onBackToAppsClick: () -> Unit,
    onHomeClick: () -> Unit,
    tabletLayout: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.app_not_ready_message),
            color = SnowWhite,
            fontSize = if (tabletLayout) 38.sp else 29.sp,
            lineHeight = if (tabletLayout) 44.sp else 34.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.app_not_ready_help),
            color = SnowWhite.copy(alpha = 0.92f),
            fontSize = if (tabletLayout) 20.sp else 17.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(28.dp))
        WinterNavigationButton(
            labelResource = R.string.back_to_apps,
            onClick = onBackToAppsClick,
        )
        Spacer(modifier = Modifier.height(10.dp))
        WinterNavigationButton(
            labelResource = R.string.back_to_home,
            onClick = onHomeClick,
        )
    }
}
