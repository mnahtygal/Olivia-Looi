package com.nahtygal.olivialooi.screenshots

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nahtygal.olivialooi.ui.theme.OliviaLooiTheme
import java.io.File
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real device screenshots of production composables. No generated/mock image output. */
class ShowcaseCaptureTest {
    @get:Rule val compose = createAndroidComposeRule<CaptureActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private fun click(label: String) {
        val matcher = hasText(label) or hasContentDescription(label)
        compose.waitUntil(10_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
        val node=compose.onAllNodes(matcher).onFirst()
        runCatching { node.performScrollTo() }
        node.performClick()
        compose.mainClock.advanceTimeBy(64)
    }
    private fun textPresent(value: String): Boolean = compose.onAllNodes(hasText(value,substring=true) or hasContentDescription(value,substring=true),useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()
    private fun prepare(id: String) {
        when(id) {
            "home_music_picker" -> click("Music")
            "home_learn_picker" -> click("Learn")
            "games_main_lower" -> {
                val scroll=compose.onAllNodes(hasScrollAction()).onFirst()
                scroll.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f,10000f) }
            }
            "story_read_to_me" -> { click("Resume"); check(textPresent("Pause")) }
            "billiards_aiming" -> {
                val ball=compose.onNodeWithContentDescription("Cue ball",useUnmergedTree=true)
                // Use the production ball's semantic bounds; hold a real slingshot gesture.
                val center=ball.fetchSemanticsNode().boundsInRoot.center
                compose.onNodeWithTag("capture-ready").performTouchInput { down(center); moveTo(center+Offset(0f,85f)) }
                compose.mainClock.advanceTimeByFrame()
            }
            "settings_main" -> {
                compose.runOnIdle { compose.activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${compose.activity.packageName}"))) }
                instrumentation.waitForIdleSync()
                compose.waitUntil(10_000) {
                    instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() == "com.android.settings"
                }
            }
        }
    }

    @Test fun fixtureContracts() {
        val inventory=JSONArray(instrumentation.context.assets.open("looloo_screenshots.json").bufferedReader().use { it.readText() })
        val ids=mutableSetOf<String>()
        for (index in 0 until inventory.length()) {
            val id=inventory.getJSONObject(index).getString("id")
            assertTrue(ids.add(id))
            // Includes completion transitions and codec round trips, before rendering.
            CaptureFixtures.create(id)
        }
        assertEquals(65,ids.size)
        assertNull(CaptureFixtures.create("animal_sounds_main").registry.consumeRestored("unused"))
        repeat(2) {
            val registry = CaptureFixtures.create("animal_sounds_selected").registry
            assertEquals("cow", (registry.consumeRestored("animal") as State<*>).value)
            val version = registry.consumeRestored("feedback")
            assertTrue(version is MutableIntState)
            assertEquals(0, (version as MutableIntState).intValue)
            assertEquals(true, (registry.consumeRestored("showWord") as State<*>).value)
        }
        val billiards = CaptureFixtures.create("billiards_free_play_rack").registry
        billiards.consumeRestored("table")
        assertTrue(billiards.consumeRestored("spokenIdentity") is MutableLongState)
    }

    @Test fun captureInventory() {
        val args=InstrumentationRegistry.getArguments()
        // The host owns batching: an uncaught Compose exception can kill this process.
        val screenId=requireNotNull(args.getString("screen_id")) { "Use the host capture script; one screen_id per instrumentation process is required" }
        val utilities=args.getString("utilities","false")=="true"
        val inventory=JSONArray(instrumentation.context.assets.open("looloo_screenshots.json").bufferedReader().use { it.readText() })
        assertEquals(65,inventory.length())
        val output=File(instrumentation.targetContext.filesDir,"looloo-capture").apply { mkdirs() }
        val results=JSONObject()
        var current by mutableStateOf<Pair<String,CaptureFixture>?>(null)
        // Virtual time is controlled: transient demo/AI/aim states never race wall-clock sleeps.
        compose.mainClock.autoAdvance=false
        compose.setContent {
            current?.let { (id,fixture) -> key(id) {
                CompositionLocalProvider(LocalSaveableStateRegistry provides fixture.registry) {
                    OliviaLooiTheme { Box(Modifier.fillMaxSize().testTag("capture-ready")) { fixture.content() } }
                }
            } }
        }
        val info=instrumentation.targetContext.packageManager.getPackageInfo(instrumentation.targetContext.packageName,0)
        require((0 until inventory.length()).any { inventory.getJSONObject(it).getString("id") == screenId })
        var failures=0
        for(index in 0 until inventory.length()) {
            val entry=inventory.getJSONObject(index)
            if(screenId!=entry.getString("id")) continue
            val id=entry.getString("id")
            val record=JSONObject().put("app_version",info.versionName).put("app_label",instrumentation.targetContext.applicationInfo.loadLabel(instrumentation.targetContext.packageManager).toString()).put("timestamp",Instant.now().toString())
            try {
                if(!entry.getBoolean("public_default") && !utilities) {
                    record.put("status","skipped").put("notes","Utility screen excluded by default; opt in after privacy review")
                } else {
                    val fixture=CaptureFixtures.create(id)
                    compose.runOnIdle { current=null }
                    compose.mainClock.advanceTimeByFrame()
                    compose.runOnIdle { current=id to fixture }
                    compose.mainClock.advanceTimeBy(64)
                    compose.waitUntil(10_000) { compose.onAllNodesWithTag("capture-ready").fetchSemanticsNodes().size==1 }
                    compose.runOnIdle { fixture.registry.assertConsumed() }
                    if(id.startsWith("piano_") || id.startsWith("drums_")) {
                        compose.waitUntil(15_000) { !textPresent("Getting the") }
                    }
                    compose.mainClock.advanceTimeBy(if(id=="drums_copy_beat_demo") 450 else 128)
                    prepare(id)
                    // Flush measure/draw after semantic state is available; never arbitrary wall-clock sleep.
                    if (id != "settings_main") compose.waitForIdle()
                    instrumentation.waitForIdleSync()
                    if(id=="drums_copy_beat_demo") check(textPresent("Listen")) { "Replay phase was not visible" }
                    if(id=="drums_copy_beat_your_turn") check(textPresent("Your turn"))
                    if(id=="billiards_versus_looloo_turn") check(textPresent("LooLoo"))
                    val expectedPackage = if (id == "settings_main") "com.android.settings" else instrumentation.targetContext.packageName
                    compose.waitUntil(10_000) {
                        instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() == expectedPackage
                    }
                    val bitmap=checkNotNull(instrumentation.uiAutomation.takeScreenshot()) { "Device screenshot unavailable" }
                    check(bitmap.height>bitmap.width) { "Capture requires portrait orientation" }
                    File(output,entry.getString("filename")).outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
                    bitmap.recycle()
                    record.put("status","captured").put("notes",when(id) {
                        "settings_main" -> "OS app settings; explicitly opted in. Review for publication."
                        "apps_main" -> "Live installed-app availability; explicitly opted in. Review for publication."
                        "billiards_aiming" -> "Held production slingshot gesture; screenshot taken before release."
                        "story_read_to_me" -> "Production reader resumed on page one."
                        else -> "Production UI; seed 26 used for randomized fixture state / existing saved-state restoration."
                    })
                    if(id=="billiards_aiming") compose.onNodeWithTag("capture-ready").performTouchInput { cancel() }
                }
            } catch(error: Throwable) {
                failures++
                // Avoid exporting raw stack traces/private paths into the shareable manifest.
                File(output,entry.getString("filename")).delete()
                record.put("status","failed").put("error_type",error.javaClass.simpleName)
                    .put("notes","$id failed: ${error.javaClass.simpleName}; see the per-fixture instrumentation log for details")
                error.printStackTrace()
            } finally {
                results.put(id,record)
                File(output,"results.json").writeText(results.toString(2))
            }
        }
        try {
            instrumentation.runOnMainSync { current=null; compose.activity.finish() }
        } catch (_: Exception) {
            // Host-side force-stop is the fallback when the Compose owner is already broken.
        }
        assertEquals("Some requested screenshots failed; inspect manifest",0,failures)
    }
}
