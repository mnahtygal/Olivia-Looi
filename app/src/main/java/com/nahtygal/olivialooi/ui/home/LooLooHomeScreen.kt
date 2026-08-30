package com.nahtygal.olivialooi.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.audio.MicrophoneCapture
import com.nahtygal.olivialooi.ui.theme.AuroraPurple
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.FrostBlue
import com.nahtygal.olivialooi.ui.theme.IceBlue
import com.nahtygal.olivialooi.ui.theme.OliviaLooiTheme
import com.nahtygal.olivialooi.ui.theme.ReadyMint
import com.nahtygal.olivialooi.ui.theme.SkyBlue
import com.nahtygal.olivialooi.ui.theme.SnowWhite
import kotlin.math.ceil

private enum class MicrophoneUiState {
    Ready,
    Starting,
    Listening,
    PermissionDenied,
    Error,
}

@Composable
fun LooLooHomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var microphoneState by remember { mutableStateOf(MicrophoneUiState.Ready) }
    var microphoneAmplitude by remember { mutableFloatStateOf(0f) }
    var permissionRequestAttempted by rememberSaveable { mutableStateOf(false) }

    val mainExecutor = remember(context) {
        ContextCompat.getMainExecutor(context.applicationContext)
    }
    val microphoneCapture = remember(mainExecutor) {
        MicrophoneCapture(
            callbackExecutor = mainExecutor,
            onStarted = { microphoneState = MicrophoneUiState.Listening },
            onAmplitude = { microphoneAmplitude = it },
            onError = {
                microphoneAmplitude = 0f
                microphoneState = MicrophoneUiState.Error
            },
        )
    }

    @SuppressLint("MissingPermission")
    fun startMicrophone() {
        microphoneAmplitude = 0f
        microphoneState = MicrophoneUiState.Starting
        microphoneCapture.start()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startMicrophone()
        } else {
            microphoneAmplitude = 0f
            microphoneState = MicrophoneUiState.PermissionDenied
        }
    }

    DisposableEffect(lifecycleOwner, microphoneCapture) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                microphoneCapture.stop()
                microphoneAmplitude = 0f
                if (
                    microphoneState == MicrophoneUiState.Starting ||
                    microphoneState == MicrophoneUiState.Listening
                ) {
                    microphoneState = MicrophoneUiState.Ready
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            microphoneCapture.close()
        }
    }

    val onPrimaryAction = {
        when (microphoneState) {
            MicrophoneUiState.Listening -> {
                microphoneCapture.stop()
                microphoneAmplitude = 0f
                microphoneState = MicrophoneUiState.Ready
            }

            MicrophoneUiState.Starting -> Unit

            else -> {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED

                when {
                    hasPermission -> startMicrophone()
                    !permissionRequestAttempted -> {
                        permissionRequestAttempted = true
                        microphoneState = MicrophoneUiState.Starting
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }

                    else -> microphoneState = MicrophoneUiState.PermissionDenied
                }
            }
        }
    }

    val openSettings = {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ),
        )
    }

    LooLooHomeContent(
        microphoneState = microphoneState,
        microphoneAmplitude = microphoneAmplitude,
        onPrimaryAction = onPrimaryAction,
        onSettingsClick = openSettings,
        modifier = modifier,
    )
}

@Composable
private fun LooLooHomeContent(
    microphoneState: MicrophoneUiState,
    microphoneAmplitude: Float,
    onPrimaryAction: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val childName = stringResource(R.string.child_name_olivia)
    val isListening = microphoneState == MicrophoneUiState.Listening
    val greeting = when (microphoneState) {
        MicrophoneUiState.Listening -> stringResource(R.string.looloo_listening_greeting, childName)
        MicrophoneUiState.PermissionDenied -> stringResource(R.string.looloo_microphone_permission_message)
        MicrophoneUiState.Error -> stringResource(R.string.looloo_microphone_error_message)
        else -> stringResource(R.string.looloo_greeting, childName)
    }
    val helperText = when (microphoneState) {
        MicrophoneUiState.Listening -> stringResource(R.string.looloo_listening_prompt)
        MicrophoneUiState.PermissionDenied -> stringResource(R.string.looloo_microphone_permission_help)
        MicrophoneUiState.Error -> stringResource(R.string.looloo_microphone_retry_prompt)
        else -> null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF17265F),
                        0.48f to AuroraPurple,
                        1f to SkyBlue,
                    ),
                ),
            ),
    ) {
        WinterBackdrop(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.looloo_name),
                color = SnowWhite,
                fontFamily = FontFamily.Cursive,
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 48.sp,
            )
            Text(
                text = stringResource(R.string.looloo_subtitle),
                color = IceBlue,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                LooLooAvatar(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(LOOLOO_IMAGE_ASPECT_RATIO),
                )
            }

            Text(
                text = greeting,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = SnowWhite,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 32.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))

            TalkToLooLooButton(
                isListening = isListening,
                enabled = microphoneState != MicrophoneUiState.Starting,
                onClick = onPrimaryAction,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))

            LooLooStatus(
                isListening = isListening,
                microphoneAmplitude = microphoneAmplitude,
            )

            if (helperText != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = helperText,
                    color = SnowWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                )
            }

            SettingsButton(onClick = onSettingsClick)
        }
    }
}

@Composable
private fun LooLooAvatar(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Image(
        painter = painterResource(R.drawable.looloo_character),
        contentDescription = stringResource(R.string.looloo_avatar_content_description),
        modifier = modifier
            .clip(shape)
            .border(2.dp, SnowWhite.copy(alpha = 0.72f), shape),
        contentScale = ContentScale.Fit,
    )
}

private const val LOOLOO_IMAGE_ASPECT_RATIO = 1199f / 1312f

@Composable
private fun LooLooStatus(
    isListening: Boolean,
    microphoneAmplitude: Float,
    modifier: Modifier = Modifier,
) {
    val statusLabel = stringResource(
        if (isListening) R.string.looloo_listening else R.string.looloo_ready,
    )
    val statusDescription = stringResource(
        if (isListening) R.string.looloo_listening_content_description
        else R.string.looloo_ready_content_description,
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .background(SnowWhite.copy(alpha = 0.17f), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 7.dp)
                .clearAndSetSemantics {
                    contentDescription = statusDescription
                    liveRegion = LiveRegionMode.Polite
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(ReadyMint, CircleShape),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusLabel,
                color = SnowWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        if (isListening) {
            Spacer(modifier = Modifier.height(5.dp))
            MicrophoneLevelMeter(
                amplitude = microphoneAmplitude,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(38.dp),
            )
        }
    }
}

@Composable
private fun MicrophoneLevelMeter(amplitude: Float, modifier: Modifier = Modifier) {
    val level = amplitude.coerceIn(0f, 1f)
    val description = stringResource(
        R.string.looloo_microphone_level_content_description,
        (level * 100).toInt(),
    )

    Canvas(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = description
        },
    ) {
        val segmentCount = 12
        val gap = 4.dp.toPx()
        val segmentWidth = (size.width - gap * (segmentCount - 1)) / segmentCount
        val activeSegments = if (level < METER_VISIBLE_THRESHOLD) {
            0
        } else {
            ceil(level * segmentCount).toInt()
        }

        repeat(segmentCount) { index ->
            val heightScale = 0.35f + 0.65f * ((index + 1f) / segmentCount)
            val segmentHeight = size.height * heightScale
            drawRoundRect(
                color = if (index < activeSegments) ReadyMint else SnowWhite.copy(alpha = 0.2f),
                topLeft = Offset(index * (segmentWidth + gap), (size.height - segmentHeight) / 2f),
                size = Size(segmentWidth, segmentHeight),
                cornerRadius = CornerRadius(segmentWidth / 2f),
            )
        }
    }
}

private const val METER_VISIBLE_THRESHOLD = 0.035f

@Composable
private fun TalkToLooLooButton(
    isListening: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 66.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SnowWhite,
            contentColor = DeepIndigo,
            disabledContainerColor = SnowWhite.copy(alpha = 0.72f),
            disabledContentColor = DeepIndigo.copy(alpha = 0.62f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 3.dp),
    ) {
        MicrophoneIcon(modifier = Modifier.size(27.dp))
        Spacer(modifier = Modifier.width(11.dp))
        Text(
            text = stringResource(
                if (isListening) R.string.looloo_done_action else R.string.looloo_talk_action,
            ),
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 25.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MicrophoneIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 2.6.dp.toPx()
        drawRoundRect(
            color = DeepIndigo,
            topLeft = Offset(size.width * 0.34f, size.height * 0.05f),
            size = Size(size.width * 0.32f, size.height * 0.55f),
            cornerRadius = CornerRadius(size.width * 0.18f),
            style = Stroke(width = stroke),
        )
        drawArc(
            color = DeepIndigo,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * 0.19f, size.height * 0.25f),
            size = Size(size.width * 0.62f, size.height * 0.48f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawLine(
            color = DeepIndigo,
            start = Offset(size.width * 0.5f, size.height * 0.73f),
            end = Offset(size.width * 0.5f, size.height * 0.91f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = DeepIndigo,
            start = Offset(size.width * 0.32f, size.height * 0.91f),
            end = Offset(size.width * 0.68f, size.height * 0.91f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit) {
    val description = stringResource(R.string.looloo_settings_content_description)
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description },
    ) {
        Canvas(modifier = Modifier.size(23.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            repeat(8) { index ->
                rotate(index * 45f, center) {
                    drawLine(
                        color = SnowWhite.copy(alpha = 0.82f),
                        start = Offset(center.x, 1.dp.toPx()),
                        end = Offset(center.x, 5.dp.toPx()),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            drawCircle(
                color = SnowWhite.copy(alpha = 0.82f),
                radius = 7.dp.toPx(),
                center = center,
                style = Stroke(width = 3.dp.toPx()),
            )
            drawCircle(
                color = SnowWhite.copy(alpha = 0.82f),
                radius = 2.dp.toPx(),
                center = center,
            )
        }
    }
}

@Composable
private fun WinterBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(IceBlue.copy(alpha = 0.3f), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.45f),
                radius = size.width * 0.72f,
            ),
            radius = size.width * 0.72f,
            center = Offset(size.width * 0.5f, size.height * 0.45f),
        )

        val flakes = listOf(
            Offset(0.09f, 0.14f) to 7f,
            Offset(0.88f, 0.19f) to 9f,
            Offset(0.13f, 0.42f) to 5f,
            Offset(0.91f, 0.52f) to 6f,
            Offset(0.08f, 0.72f) to 8f,
            Offset(0.88f, 0.79f) to 5f,
        )
        flakes.forEach { (position, radius) ->
            val center = Offset(size.width * position.x, size.height * position.y)
            repeat(3) { spoke ->
                rotate(spoke * 60f, center) {
                    drawLine(
                        color = SnowWhite.copy(alpha = 0.42f),
                        start = Offset(center.x - radius, center.y),
                        end = Offset(center.x + radius, center.y),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        listOf(Offset(0.22f, 0.25f), Offset(0.78f, 0.34f), Offset(0.24f, 0.84f)).forEach {
            drawCircle(
                color = FrostBlue.copy(alpha = 0.7f),
                radius = 2.5.dp.toPx(),
                center = Offset(size.width * it.x, size.height * it.y),
            )
        }
    }
}

@Preview(
    name = "Ready · Small phone",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
)
@Composable
private fun LooLooReadyPreview() {
    OliviaLooiTheme {
        LooLooHomeContent(
            microphoneState = MicrophoneUiState.Ready,
            microphoneAmplitude = 0f,
            onPrimaryAction = {},
            onSettingsClick = {},
        )
    }
}

@Preview(
    name = "Listening · Small phone · Large text",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    fontScale = 1.3f,
)
@Composable
private fun LooLooListeningPreview() {
    OliviaLooiTheme {
        LooLooHomeContent(
            microphoneState = MicrophoneUiState.Listening,
            microphoneAmplitude = 0.68f,
            onPrimaryAction = {},
            onSettingsClick = {},
        )
    }
}
