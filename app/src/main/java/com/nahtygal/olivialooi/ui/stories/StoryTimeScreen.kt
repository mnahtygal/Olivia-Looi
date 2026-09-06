package com.nahtygal.olivialooi.ui.stories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nahtygal.olivialooi.speech.AndroidTextToSpeech
import com.nahtygal.olivialooi.stories.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.cos
import kotlin.math.sin

private val Night = Color(0xFF263650)
private val Cream = Color(0xFFF4F0E7)
private val StorySaver = Saver<StoryPlaybackState, String>(save = { StoryEngine.encode(it) }, restore = StoryEngine::decode)

@Composable
fun StoryTimeScreen(onHome: () -> Unit) {
    var library by rememberSaveable { mutableStateOf(true) }
    var state by rememberSaveable(stateSaver = StorySaver) { mutableStateOf(StoryEngine.open(StoryCatalog.stories.first().id)) }
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val speech = remember(context) { AndroidTextToSpeech(context.applicationContext, offlineOnly = true) }
    var foreground by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    fun change(next: StoryPlaybackState) { speech.stop(); state = next }
    fun toLibrary() { change(StoryEngine.stop(state)); library = true }
    fun home() { change(StoryEngine.stop(state)); onHome() }
    BackHandler(!library) { if (state.mode != null && !state.complete) change(StoryEngine.opening(state)) else toLibrary() }
    DisposableEffect(owner, speech) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> foreground = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    foreground = false
                    speech.stop()
                    state = StoryEngine.stop(state)
                }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); speech.close() }
    }
    LaunchedEffect(state.generation, state.narrating, foreground, library) {
        speech.stop()
        if (foreground && !library && state.narrating) {
            val identity = state.generation
            val text = StoryCatalog.find(state.storyId)!!.pages[state.page].text
            val success = suspendCancellableCoroutine { continuation ->
                speech.speak(text) { succeeded ->
                    if (continuation.isActive) continuation.resume(succeeded)
                }
                continuation.invokeOnCancellation { speech.stop() }
            }
            if (foreground && !library) state = StoryEngine.finished(state, identity, success)
        }
    }
    val scroll = rememberScrollState()
    LaunchedEffect(library, state.storyId, state.page, state.mode, state.complete) { scroll.scrollTo(0) }
    BoxWithConstraints(Modifier.fillMaxSize().background(Cream).statusBarsPadding().navigationBarsPadding()) {
        val tablet = maxWidth >= 600.dp
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(if (tablet) 28.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Story Time", color = Night, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            if (library) {
                Text("Pick a story!", color = Night, fontSize = 23.sp)
                StoryCatalog.stories.chunked(if (tablet) 2 else 1).forEach { row ->
                    Row(Modifier.widthIn(max = 960.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        row.forEach { story ->
                            Card(onClick = { change(StoryEngine.open(story.id, state)); library = false }, modifier = Modifier.weight(1f)
                                .semantics(mergeDescendants = true) { contentDescription = "${story.title}. ${story.shortDescription}" },
                                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    StoryIllustration(story.coverVisual, 6, Modifier.fillMaxWidth().height(170.dp))
                                    Text(story.title, color = Night, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                                    Text(story.shortDescription, color = Night, fontSize = 18.sp)
                                }
                            }
                        }
                        if (tablet && row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                StoryButton("Back Home", onClick = ::home)
            } else {
                val story = StoryCatalog.find(state.storyId)!!
                Text(story.title, color = Night, fontSize = if (tablet) 30.sp else 25.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
                StoryIllustration(story.coverVisual, if (state.mode == null || state.complete) 6 else state.page + 1,
                    Modifier.widthIn(max = 720.dp).fillMaxWidth().height(if (tablet) 320.dp else 220.dp))
                when {
                    state.complete -> {
                        Text("The End", color = Night, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Text("Great story time, Olivia & Eliana!", color = Night, fontSize = 23.sp, textAlign = TextAlign.Center)
                        StoryButton("Read It Again") { change(StoryEngine.again(state)) }
                        StoryButton("Pick Another Story", onClick = ::toLibrary)
                        StoryButton("Back Home", onClick = ::home)
                    }
                    state.mode == null -> {
                        StoryButton("Read to Me") { change(StoryEngine.chooseMode(state, ReadingMode.TO_ME)) }
                        StoryButton("Read Myself") { change(StoryEngine.chooseMode(state, ReadingMode.MYSELF)) }
                        StoryButton("Back to Stories", onClick = ::toLibrary)
                    }
                    else -> {
                        Text("Page ${state.page + 1} of ${story.pages.size}", color = Night, fontSize = 18.sp,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        Text(story.pages[state.page].text, color = Night, fontSize = if (tablet) 28.sp else 24.sp,
                            lineHeight = if (tablet) 39.sp else 34.sp, modifier = Modifier.widthIn(max = 780.dp).fillMaxWidth())
                        if (state.speechFailed) Text("Voice is unavailable. You can still read and turn the pages.", color = Night, fontSize = 18.sp)
                        Row(Modifier.widthIn(max = 720.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StoryButton("Previous", Modifier.weight(1f), enabled = state.page > 0) { change(StoryEngine.previous(state)) }
                            StoryButton("Next", Modifier.weight(1f)) { change(StoryEngine.next(state)) }
                        }
                        if (state.mode == ReadingMode.TO_ME) {
                            StoryButton(if (state.narrating) "Pause" else "Resume") {
                                change(if (state.narrating) StoryEngine.stop(state) else StoryEngine.say(state))
                            }
                            if (!state.narrating) Text("Resume reads this page from the beginning.", color = Night, fontSize = 16.sp)
                            StoryButton("Say Again") { change(StoryEngine.say(state)) }
                            StoryButton("Stop Reading") { change(StoryEngine.opening(state)) }
                        } else {
                            StoryButton("Say It") { change(StoryEngine.say(state)) }
                        }
                        StoryButton("Back to Stories", onClick = ::toLibrary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, modifier.widthIn(max = 720.dp).fillMaxWidth().heightIn(min = 52.dp), enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Night, contentColor = Cream)) {
        Text(label, fontSize = 20.sp, textAlign = TextAlign.Center)
    }
}

/** Original vector scenes. Page number adds the objects as they enter each story. */
@Composable
private fun StoryIllustration(visual: StoryVisual, page: Int, modifier: Modifier) {
    val starPath = remember {
        Path().apply {
            repeat(10) { i ->
                val angle = -Math.PI / 2 + i * Math.PI / 5
                val radius = if (i % 2 == 0) 1f else .45f
                val x = cos(angle).toFloat() * radius
                val y = sin(angle).toFloat() * radius
                if (i == 0) moveTo(x,y) else lineTo(x,y)
            }
            close()
        }
    }
    val description = when (visual) {
        StoryVisual.SNOWMAN -> "A moonlit snowman${if (page >= 3) " with little snow friends" else ""}"
        StoryVisual.DUCK -> "A yellow duck on a pond${if (page >= 4) " with its duck family" else ""}"
        StoryVisual.RAINBOW -> "A blanket rainbow with ${if (page == 1) 6 else (page + 1).coerceAtMost(6)} colors"
        StoryVisual.PUPPY -> "A cozy puppy${if (page >= 4) " resting beneath the moon" else " with a soft ball"}"
        StoryVisual.STAR -> "A gentle star${if (page >= 3) " beside a drifting cloud" else " above the rooftops"}"
    }
    Canvas(modifier.semantics { contentDescription = description }) {
        val scale = minOf(size.width / 400f, size.height / 260f)
        withTransform({ translate((size.width-400*scale)/2, (size.height-260*scale)/2); scale(scale,scale,Offset.Zero) }) {
            drawRoundRect(Color(0xFFDCE7ED), size = Size(400f,260f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f))
            fun star(x: Float,y: Float,r: Float) { withTransform({ translate(x,y); scale(r,r,Offset.Zero) }) { drawPath(starPath, Color(0xFFE7BB5C)) } }
            fun moon() { drawCircle(Color(0xFFFFE8AC),32f,Offset(330f,48f)); drawCircle(Color(0xFFDCE7ED),28f,Offset(342f,38f)) }
            fun duck(x: Float,y: Float,r: Float) {
                drawOval(Color(0xFFF5D261),Offset(x-r,y-r*.4f),Size(r*2,r*1.2f))
                drawCircle(Color(0xFFF5D261),r*.6f,Offset(x+r*.45f,y-r*.55f))
                drawLine(Color(0xFFE39849),Offset(x+r*.9f,y-r*.5f),Offset(x+r*1.3f,y-r*.4f),r*.22f)
                drawCircle(Night,2.5f,Offset(x+r*.65f,y-r*.65f))
            }
            when (visual) {
                StoryVisual.SNOWMAN -> {
                    moon(); drawOval(Color.White,Offset(0f,190f),Size(400f,90f))
                    drawCircle(Color.White,54f,Offset(200f,168f)); drawCircle(Color.White,36f,Offset(200f,96f))
                    drawRect(Night,Offset(174f,46f),Size(52f,23f)); drawLine(Night,Offset(164f,69f),Offset(236f,69f),7f)
                    drawCircle(Night,3f,Offset(188f,92f)); drawCircle(Night,3f,Offset(211f,92f))
                    drawLine(Color(0xFFE39849),Offset(200f,102f),Offset(218f,106f),6f)
                    if (page >= 2) drawLine(Color(0xFF7F9CC0),Offset(167f,126f),Offset(236f,134f),12f)
                    repeat(3) { drawCircle(Night,4f,Offset(200f,151f+it*18f)) }
                    if (page >= 3) { drawCircle(Color.White,22f,Offset(105f,208f)); drawOval(Color.White,Offset(89f,155f),Size(12f,40f)); drawOval(Color.White,Offset(109f,155f),Size(12f,40f)) }
                    if (page >= 4) { drawCircle(Color.White,16f,Offset(290f,210f)); drawLine(Color(0xFFE39849),Offset(304f,206f),Offset(317f,210f),5f) }
                    if (page >= 5) { drawCircle(Color(0xFFD993AA),8f,Offset(194f,237f)); drawCircle(Color(0xFFD993AA),8f,Offset(206f,237f)); drawLine(Color(0xFFD993AA),Offset(188f,237f),Offset(200f,250f),9f); drawLine(Color(0xFFD993AA),Offset(212f,237f),Offset(200f,250f),9f) }
                }
                StoryVisual.DUCK -> {
                    drawOval(Color(0xFF9FCAD5),Offset(15f,110f),Size(370f,135f)); duck(190f,145f,45f)
                    if (page >= 2) drawOval(Color(0xFF7DAD76),Offset(260f,175f),Size(40f,15f))
                    if (page >= 4) { duck(95f,195f,23f); duck(310f,210f,20f) }
                    if (page >= 3) { drawLine(Color(0xFF7DAD76),Offset(60f,60f),Offset(60f,110f),5f); repeat(5) { i -> val a=i*Math.PI*2/5; drawCircle(Color(0xFFF5D261),12f,Offset(60f+cos(a).toFloat()*14,55f+sin(a).toFloat()*14)) }; drawCircle(Color.White,8f,Offset(60f,55f)) }
                }
                StoryVisual.RAINBOW -> {
                    val colors=listOf(0xFFE18F96,0xFFE9AE75,0xFFEED67F,0xFF8AB991,0xFF8DAED4,0xFFB79BCB)
                    val count=if(page==1)6 else (page+1).coerceAtMost(6)
                    colors.take(count).forEachIndexed { i,c -> drawArc(Color(c),180f,180f,false,Offset(45f+i*17,45f+i*17),Size(310f-i*34,310f-i*34),style=Stroke(15f)) }
                    drawOval(Color.White,Offset(25f,190f),Size(100f,40f)); drawOval(Color.White,Offset(280f,190f),Size(100f,40f))
                    if(page>=5) { drawCircle(Color(0xFFBA926E),16f,Offset(175f,190f)); drawCircle(Color(0xFFBA926E),12f,Offset(225f,200f)); drawLine(Color(0xFF8DAED4),Offset(175f,208f),Offset(175f,240f),24f); drawLine(Color(0xFFB79BCB),Offset(225f,214f),Offset(225f,240f),20f) }
                }
                StoryVisual.PUPPY -> {
                    if(page>=4)moon()
                    drawOval(Color(0xFFBC9B88),Offset(85f,180f),Size(230f,60f))
                    if(page>=3)drawOval(Color(0xFF9BAFD0),Offset(95f,183f),Size(210f,43f))
                    drawOval(Color(0xFFC99E72),Offset(140f,128f),Size(130f,80f)); drawCircle(Color(0xFFDDB68E),42f,Offset(150f,134f))
                    drawOval(Color(0xFF967354),Offset(107f,98f),Size(25f,66f)); drawOval(Color(0xFF967354),Offset(177f,98f),Size(25f,66f))
                    if(page>=5) { drawLine(Night,Offset(132f,130f),Offset(143f,130f),3f); drawLine(Night,Offset(158f,130f),Offset(169f,130f),3f) }
                    else { drawCircle(Night,3f,Offset(139f,130f)); drawCircle(Night,3f,Offset(162f,130f)) }
                    drawCircle(Night,5f,Offset(150f,146f)); drawCircle(Color(0xFFAA8FBC),18f,Offset(if(page==1)290f else 330f,220f))
                }
                StoryVisual.STAR -> {
                    moon(); star(160f,95f,if(page>=4)43f else 55f)
                    drawCircle(Night,3f,Offset(148f,94f)); drawCircle(Night,3f,Offset(172f,94f))
                    if(page>=3) { drawCircle(Color.White,26f,Offset(245f,160f)); drawOval(Color.White,Offset(207f,156f),Size(90f,31f)) }
                    repeat(4) { i -> drawRect(Color(0xFF8FA4BA),Offset(35f+i*87,219f),Size(60f,41f)); drawRect(Color(0xFFFFE8AC),Offset(53f+i*87,232f),Size(15f,15f)) }
                    repeat(page) { i -> star(25f+i*54,28f,5f) }
                }
            }
        }
    }
}
