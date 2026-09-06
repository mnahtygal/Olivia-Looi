package com.nahtygal.olivialooi.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.nahtygal.olivialooi.brain.LooLooKidBrain
import com.nahtygal.olivialooi.network.AndroidJarvisChatClient
import com.nahtygal.olivialooi.network.JarvisChatResult
import com.nahtygal.olivialooi.speech.AndroidSpeechRecognizer
import com.nahtygal.olivialooi.speech.AndroidTextToSpeech
import com.nahtygal.olivialooi.speech.SpeechRecognitionFailure
import com.nahtygal.olivialooi.ui.theme.AuroraPurple
import com.nahtygal.olivialooi.ui.theme.DeepIndigo
import com.nahtygal.olivialooi.ui.theme.FrostBlue
import com.nahtygal.olivialooi.ui.theme.IceBlue
import com.nahtygal.olivialooi.ui.theme.OliviaLooiTheme
import com.nahtygal.olivialooi.ui.theme.ReadyMint
import com.nahtygal.olivialooi.ui.theme.SkyBlue
import com.nahtygal.olivialooi.ui.theme.SnowWhite
import kotlin.math.ceil

private enum class HomeSpeechState {
    Ready,
    Starting,
    Listening,
    Processing,
    Thinking,
    Response,
    NetworkError,
    NoSpeech,
    PermissionDenied,
    Error,
}

@Composable
fun LooLooHomeScreen(
    onGamesClick: () -> Unit,
    onAppsClick: () -> Unit,
    onStoriesClick: () -> Unit,
    onActivityClick: (HomeActivity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var speechState by remember { mutableStateOf(HomeSpeechState.Ready) }
    var microphoneAmplitude by remember { mutableFloatStateOf(0f) }
    var recognizedText by remember { mutableStateOf<String?>(null) }
    var jarvisResponse by remember { mutableStateOf<String?>(null) }
    var permissionRequestAttempted by rememberSaveable { mutableStateOf(false) }

    val jarvisClient = remember(context) {
        AndroidJarvisChatClient(context.applicationContext)
    }
    val textToSpeech = remember(context) {
        AndroidTextToSpeech(context.applicationContext)
    }
    val kidBrain = remember { LooLooKidBrain() }

    fun sendToJarvis(childMessage: String) {
        val prompt = kidBrain.buildPrompt(childMessage)
        if (prompt == null) {
            recognizedText = null
            jarvisResponse = null
            speechState = HomeSpeechState.NoSpeech
            return
        }
        jarvisResponse = null
        speechState = HomeSpeechState.Thinking
        jarvisClient.send(prompt) { result ->
            when (result) {
                is JarvisChatResult.Success -> {
                    jarvisResponse = result.response
                    speechState = HomeSpeechState.Response
                    textToSpeech.speak(result.response)
                }

                is JarvisChatResult.Failure -> {
                    jarvisResponse = null
                    speechState = HomeSpeechState.NetworkError
                }
            }
        }
    }

    val speechRecognizer = remember(context) {
        AndroidSpeechRecognizer(
            context = context.applicationContext,
            onListening = { speechState = HomeSpeechState.Listening },
            onProcessing = {
                if (
                    speechState == HomeSpeechState.Starting ||
                    speechState == HomeSpeechState.Listening
                ) {
                    speechState = HomeSpeechState.Processing
                }
            },
            onPartialResult = { recognizedText = it },
            onFinalResult = {
                microphoneAmplitude = 0f
                recognizedText = it
                sendToJarvis(it)
            },
            onLevelChanged = { microphoneAmplitude = it },
            onFailure = { failure ->
                microphoneAmplitude = 0f
                recognizedText = null
                speechState = when (failure) {
                    SpeechRecognitionFailure.NoSpeech -> HomeSpeechState.NoSpeech
                    SpeechRecognitionFailure.Recognition -> HomeSpeechState.Error
                }
            },
        )
    }

    @SuppressLint("MissingPermission")
    fun startSpeechRecognition() {
        textToSpeech.stop()
        jarvisClient.cancel()
        microphoneAmplitude = 0f
        recognizedText = null
        jarvisResponse = null
        speechState = HomeSpeechState.Starting
        speechRecognizer.start()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startSpeechRecognition()
        } else {
            microphoneAmplitude = 0f
            recognizedText = null
            speechState = HomeSpeechState.PermissionDenied
        }
    }

    DisposableEffect(lifecycleOwner, speechRecognizer, jarvisClient, textToSpeech) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                speechRecognizer.cancel()
                jarvisClient.cancel()
                textToSpeech.stop()
                microphoneAmplitude = 0f
                if (
                    speechState == HomeSpeechState.Starting ||
                    speechState == HomeSpeechState.Listening ||
                    speechState == HomeSpeechState.Processing ||
                    speechState == HomeSpeechState.Thinking
                ) {
                    recognizedText = null
                    jarvisResponse = null
                    speechState = HomeSpeechState.Ready
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            speechRecognizer.close()
            jarvisClient.close()
            textToSpeech.close()
        }
    }

    val onPrimaryAction = {
        when (speechState) {
            HomeSpeechState.Listening -> {
                speechState = HomeSpeechState.Processing
                microphoneAmplitude = 0f
                speechRecognizer.stopListening()
            }

            HomeSpeechState.Starting,
            HomeSpeechState.Processing,
            HomeSpeechState.Thinking,
            -> Unit

            HomeSpeechState.NetworkError -> {
                recognizedText?.let(::sendToJarvis)
                Unit
            }

            else -> {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED

                when {
                    hasPermission -> startSpeechRecognition()
                    !permissionRequestAttempted -> {
                        permissionRequestAttempted = true
                        speechState = HomeSpeechState.Starting
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }

                    else -> speechState = HomeSpeechState.PermissionDenied
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

    fun leaveHome(destination: () -> Unit) {
        speechRecognizer.cancel()
        jarvisClient.cancel()
        textToSpeech.stop()
        microphoneAmplitude = 0f
        recognizedText = null
        jarvisResponse = null
        speechState = HomeSpeechState.Ready
        destination()
    }

    val openGames = { leaveHome(onGamesClick) }
    val openApps = { leaveHome(onAppsClick) }

    LooLooHomeContent(
        speechState = speechState,
        microphoneAmplitude = microphoneAmplitude,
        recognizedText = recognizedText,
        jarvisResponse = jarvisResponse,
        onPrimaryAction = onPrimaryAction,
        onSettingsClick = openSettings,
        onGamesClick = openGames,
        onAppsClick = openApps,
        onStoriesClick = { leaveHome(onStoriesClick) },
        onActivityClick = { activity -> leaveHome { onActivityClick(activity) } },
        modifier = modifier,
    )
}

@Composable
private fun LooLooHomeContent(
    speechState: HomeSpeechState,
    microphoneAmplitude: Float,
    recognizedText: String?,
    jarvisResponse: String?,
    onPrimaryAction: () -> Unit,
    onSettingsClick: () -> Unit,
    onGamesClick: () -> Unit,
    onAppsClick: () -> Unit,
    onStoriesClick: () -> Unit,
    onActivityClick: (HomeActivity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picker by rememberSaveable { mutableStateOf<HomeArtworkAction?>(null) }
    val childName = stringResource(R.string.child_name_olivia)
    val isListening = speechState == HomeSpeechState.Starting ||
        speechState == HomeSpeechState.Listening ||
        speechState == HomeSpeechState.Processing
    val greeting = when (speechState) {
        HomeSpeechState.Starting,
        HomeSpeechState.Listening,
        HomeSpeechState.Processing,
        -> stringResource(R.string.looloo_listening_greeting, childName)

        HomeSpeechState.Thinking -> stringResource(R.string.looloo_thinking_message)
        HomeSpeechState.Response -> stringResource(R.string.looloo_response_label)
        HomeSpeechState.NetworkError -> stringResource(R.string.looloo_network_error_message)
        HomeSpeechState.NoSpeech -> stringResource(R.string.looloo_no_speech_message)
        HomeSpeechState.PermissionDenied -> stringResource(R.string.looloo_microphone_permission_message)
        HomeSpeechState.Error -> stringResource(R.string.looloo_recognition_error_message)
        else -> stringResource(R.string.looloo_greeting, childName)
    }
    val helperText = when (speechState) {
        HomeSpeechState.Starting,
        HomeSpeechState.Listening,
        -> stringResource(R.string.looloo_listening_prompt)

        HomeSpeechState.Processing -> stringResource(R.string.looloo_processing_prompt)
        HomeSpeechState.NetworkError -> stringResource(R.string.looloo_network_retry_prompt)
        HomeSpeechState.PermissionDenied -> stringResource(R.string.looloo_microphone_permission_help)
        HomeSpeechState.NoSpeech,
        HomeSpeechState.Error,
        -> stringResource(R.string.looloo_microphone_retry_prompt)

        else -> null
    }
    val displayedSpeech = recognizedText?.let { speech ->
        when (speechState) {
            HomeSpeechState.Listening,
            HomeSpeechState.Processing,
            -> stringResource(R.string.looloo_recognized_speech, speech)

            HomeSpeechState.Thinking,
            HomeSpeechState.Response,
            HomeSpeechState.NetworkError,
            -> stringResource(R.string.looloo_child_said, childName, speech)

            else -> null
        }
    }
    val primaryActionLabel = when (speechState) {
        HomeSpeechState.Response -> R.string.looloo_talk_again_action
        HomeSpeechState.NetworkError -> R.string.looloo_try_again_action
        HomeSpeechState.NoSpeech,
        HomeSpeechState.Error,
        HomeSpeechState.PermissionDenied,
        -> R.string.looloo_try_again_action

        HomeSpeechState.Starting,
        HomeSpeechState.Listening,
        HomeSpeechState.Processing,
        -> R.string.looloo_done_action

        HomeSpeechState.Thinking,
        HomeSpeechState.Ready -> R.string.looloo_talk_action
    }

    BoxWithConstraints(
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
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Keep the portrait artwork readable; extra speech content scrolls instead
        // of squeezing the girls or the functional controls into the remaining space.
        val heroWidth = if (maxWidth >= 600.dp) {
            minOf(maxWidth - 40.dp, (maxHeight - 170.dp).coerceAtLeast(480.dp) * LOOLOO_IMAGE_ASPECT_RATIO)
        } else {
            maxWidth - 40.dp
        }
        WinterBackdrop(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                LooLooAvatar(
                    modifier = Modifier
                        .width(heroWidth)
                        .aspectRatio(LOOLOO_IMAGE_ASPECT_RATIO),
                    talkLabel = stringResource(primaryActionLabel),
                    talkEnabled = speechState != HomeSpeechState.Starting && speechState != HomeSpeechState.Processing && speechState != HomeSpeechState.Thinking,
                    onAction = { action ->
                        when (action) {
                            HomeArtworkAction.TALK -> onPrimaryAction()
                            HomeArtworkAction.GAMES -> onGamesClick()
                            HomeArtworkAction.STORIES -> onStoriesClick()
                            else -> picker = action
                        }
                    },
                )
            }

            if (speechState != HomeSpeechState.Ready) Text(
                text = greeting,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = SnowWhite,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 32.sp,
                textAlign = TextAlign.Center,
            )
            if (displayedSpeech != null) {
                Text(
                    text = displayedSpeech,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = IceBlue,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 23.sp,
                    maxLines = 3,
                    textAlign = TextAlign.Center,
                )
            }
            if (speechState == HomeSpeechState.Response && jarvisResponse != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = jarvisResponse,
                    modifier = Modifier
                        .heightIn(max = 96.dp)
                        .verticalScroll(rememberScrollState())
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    color = SnowWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 23.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (isListening || speechState == HomeSpeechState.Thinking) LooLooStatus(
                isListening = isListening,
                isThinking = speechState == HomeSpeechState.Thinking,
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppsButton(onClick = onAppsClick)
                Spacer(modifier = Modifier.width(6.dp))
                SettingsButton(onClick = onSettingsClick)
            }
        }
    }
    picker?.let { selected ->
        AlertDialog(
            onDismissRequest = { picker = null },
            title = { Text(stringResource(when (selected) {
                HomeArtworkAction.MUSIC -> R.string.home_music
                HomeArtworkAction.LEARN -> R.string.home_learn
                else -> R.string.home_stories
            })) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    val activities = if (selected == HomeArtworkAction.MUSIC) listOf(HomeActivity.PIANO, HomeActivity.DRUMS)
                        else listOf(HomeActivity.ABC, HomeActivity.MATH, HomeActivity.COUNTING, HomeActivity.SHAPES, HomeActivity.ANIMALS, HomeActivity.SPELLING)
                    activities.forEach { activity ->
                        TextButton(onClick = { picker = null; onActivityClick(activity) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(stringResource(when (activity) {
                                HomeActivity.PIANO -> R.string.home_piano
                                HomeActivity.DRUMS -> R.string.home_drums
                                HomeActivity.ABC -> R.string.home_abc
                                HomeActivity.MATH -> R.string.home_math
                                HomeActivity.COUNTING -> R.string.home_counting
                                HomeActivity.SHAPES -> R.string.home_shapes
                                HomeActivity.ANIMALS -> R.string.home_animals
                                HomeActivity.SPELLING -> R.string.home_spelling
                            }))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picker = null }) { Text(stringResource(R.string.home_close)) } },
        )
    }

}

@Composable
private fun LooLooAvatar(modifier: Modifier = Modifier, talkLabel: String, talkEnabled: Boolean, onAction: (HomeArtworkAction) -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    BoxWithConstraints(modifier) {
        val image = HomeArtwork.fit(maxWidth.value, maxHeight.value)
        Image(
            painter = painterResource(R.drawable.looloo_home_olivia_eliana),
            contentDescription = stringResource(R.string.looloo_avatar_content_description),
            modifier = Modifier.fillMaxSize().clip(shape).border(2.dp, SnowWhite.copy(alpha = .72f), shape),
            contentScale = ContentScale.Fit,
        )
        HomeArtworkAction.entries.forEach { action ->
            val region = HomeArtwork.region(action, image)
            val label = when (action) {
                HomeArtworkAction.TALK -> talkLabel
                HomeArtworkAction.GAMES -> stringResource(R.string.games_action)
                HomeArtworkAction.MUSIC -> stringResource(R.string.home_music)
                HomeArtworkAction.STORIES -> stringResource(R.string.home_stories)
                HomeArtworkAction.LEARN -> stringResource(R.string.home_learn)
            }
            Box(Modifier.offset(region.left.dp, region.top.dp).size(region.width.dp, region.height.dp)
                .clickable(enabled = action != HomeArtworkAction.TALK || talkEnabled, role = Role.Button, onClick = { onAction(action) })
                .semantics { contentDescription = label })
        }
    }
}

private const val LOOLOO_IMAGE_ASPECT_RATIO = HomeArtwork.ASPECT_RATIO

@Composable
private fun LooLooStatus(
    isListening: Boolean,
    isThinking: Boolean,
    microphoneAmplitude: Float,
    modifier: Modifier = Modifier,
) {
    val statusLabel = stringResource(
        when {
            isThinking -> R.string.looloo_thinking
            isListening -> R.string.looloo_listening
            else -> R.string.looloo_ready
        },
    )
    val statusDescription = stringResource(
        when {
            isThinking -> R.string.looloo_thinking_content_description
            isListening -> R.string.looloo_listening_content_description
            else -> R.string.looloo_ready_content_description
        },
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
private fun AppsButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SnowWhite.copy(alpha = 0.92f),
            contentColor = DeepIndigo,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    ) {
        AppsMiniIcon(modifier = Modifier.size(25.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.apps_action),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun AppsMiniIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val squareSize = size.minDimension * 0.35f
        val gap = size.minDimension * 0.16f
        val corner = CornerRadius(size.minDimension * 0.08f)
        listOf(
            Offset(0f, 0f),
            Offset(squareSize + gap, 0f),
            Offset(0f, squareSize + gap),
            Offset(squareSize + gap, squareSize + gap),
        ).forEach { topLeft ->
            drawRoundRect(
                color = DeepIndigo,
                topLeft = topLeft,
                size = Size(squareSize, squareSize),
                cornerRadius = corner,
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

@Preview(name = "Ready · Samsung SM-X610 portrait", showBackground = true, widthDp = 800, heightDp = 1280)
@Preview(name = "Ready · Galaxy S22 portrait", showBackground = true, widthDp = 360, heightDp = 780)
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
            speechState = HomeSpeechState.Ready,
            microphoneAmplitude = 0f,
            recognizedText = null,
            jarvisResponse = null,
            onPrimaryAction = {},
            onSettingsClick = {},
            onGamesClick = {},
            onAppsClick = {},
            onStoriesClick = {},
            onActivityClick = {},
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
            speechState = HomeSpeechState.Listening,
            microphoneAmplitude = 0.68f,
            recognizedText = null,
            jarvisResponse = null,
            onPrimaryAction = {},
            onSettingsClick = {},
            onGamesClick = {},
            onAppsClick = {},
            onStoriesClick = {},
            onActivityClick = {},
        )
    }
}
