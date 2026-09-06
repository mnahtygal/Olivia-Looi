package com.nahtygal.olivialooi.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Owns LooLoo's platform TextToSpeech engine and its bounded pending response. */
internal class AndroidTextToSpeech(
    context: Context,
    private val preferredVoiceName: String? = null,
    private val offlineOnly: Boolean = false,
) : AutoCloseable {
    private val applicationContext = context.applicationContext
    private val pendingSpeech = PendingSpeechQueue()
    private val isClosed = AtomicBoolean(false)
    private val speechGeneration = AtomicLong(0L)
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "looloo-tts").apply { isDaemon = true }
    }

    @Volatile
    private var textToSpeech: TextToSpeech? = null
    private var activeRequest: SpeechRequest? = null

    init {
        Log.d(TAG, "LOOLOO_TTS_INITIALIZING")
        executeUnlessClosed(::createTextToSpeech)
    }

    fun speak(text: String, onFinished: ((Boolean) -> Unit)? = null) {
        if (text.isBlank() || isClosed.get()) {
            onFinished?.invoke(false)
            return
        }
        val request = SpeechRequest(text, speechGeneration.incrementAndGet(), onFinished)
        when (val offer = pendingSpeech.offer(request)) {
            SpeechOffer.Queued -> Unit
            is SpeechOffer.Ready -> executeUnlessClosed { speakIfCurrent(offer.request) }
            SpeechOffer.Rejected -> onFinished?.invoke(false)
        }
    }

    fun stop() {
        if (isClosed.get()) return
        speechGeneration.incrementAndGet()
        pendingSpeech.stop()
        executeUnlessClosed {
            activeRequest = null
            val result = safely { textToSpeech?.stop() }
            if (result == TextToSpeech.ERROR) logError("stop")
            Log.d(TAG, "LOOLOO_TTS_STOP")
        }
    }

    override fun close() {
        if (!isClosed.compareAndSet(false, true)) return
        speechGeneration.incrementAndGet()
        pendingSpeech.close()
        try {
            executor.execute {
                val engine = textToSpeech
                textToSpeech = null
                safely { engine?.stop() }
                safely { engine?.shutdown() }
                Log.d(TAG, "LOOLOO_TTS_SHUTDOWN")
            }
        } catch (_: RejectedExecutionException) {
            logError("shutdown_rejected")
        } finally {
            executor.shutdown()
        }
    }

    private fun createTextToSpeech() {
        textToSpeech = try {
            TextToSpeech(applicationContext) { status ->
                executeUnlessClosed { finishInitialization(status) }
            }
        } catch (_: RuntimeException) {
            failPendingSpeech()
            Log.w(TAG, "LOOLOO_TTS_ERROR category=construction")
            null
        }
    }

    private fun finishInitialization(status: Int) {
        val engine = textToSpeech
        if (status != TextToSpeech.SUCCESS || engine == null) {
            failPendingSpeech()
            logError("initialization")
            return
        }

        val languageResult = safely { engine.setLanguage(Locale.US) }
        if (
            languageResult == null ||
            languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            failPendingSpeech()
            logError("english_unavailable")
            return
        }

        if (!discoverAndSelectVoice(engine)) {
            failPendingSpeech()
            return
        }
        val listenerResult = safely { engine.setOnUtteranceProgressListener(completionListener()) }
        if (listenerResult != TextToSpeech.SUCCESS) {
            failPendingSpeech()
            logError("completion_listener")
            return
        }

        if (safely { engine.setSpeechRate(SPEECH_RATE) } == TextToSpeech.ERROR) {
            logError("speech_rate")
        }
        if (safely { engine.setPitch(SPEECH_PITCH) } == TextToSpeech.ERROR) {
            logError("speech_pitch")
        }

        Log.d(TAG, "LOOLOO_TTS_READY")
        pendingSpeech.markReady()?.let(::speakIfCurrent)
    }

    private fun discoverAndSelectVoice(engine: TextToSpeech): Boolean {
        val engineName = safely { engine.defaultEngine }.orEmpty().ifBlank { "unknown" }
        Log.d(TAG, "LOOLOO_TTS_ENGINE package=$engineName")

        val androidVoices = safely { engine.voices }.orEmpty().sortedBy(Voice::getName)
        val candidates = androidVoices.map { voice ->
            LooLooVoiceCandidate(
                name = voice.name,
                locale = voice.locale,
                quality = voice.quality,
                latency = voice.latency,
                requiresNetwork = voice.isNetworkConnectionRequired,
                features = voice.features.orEmpty(),
            ).also(::logAvailableVoice)
        }
        val selected = LooLooVoiceSelector.select(candidates, preferredVoiceName, offlineOnly)
        if (selected == null) {
            Log.d(TAG, "LOOLOO_TTS_FALLBACK reason=no_english_voice")
            return !offlineOnly
        }

        val selectedVoice = androidVoices.firstOrNull { it.name == selected.name }
        val selectionResult = selectedVoice?.let { safely { engine.setVoice(it) } }
        if (selectionResult == TextToSpeech.SUCCESS) {
            val reason = if (selected.name == preferredVoiceName?.trim()) "preferred" else "ranked"
            Log.d(
                TAG,
                "LOOLOO_TTS_SELECTED name=${selected.name} " +
                    "locale=${selected.locale.toLanguageTag()} local=${!selected.requiresNetwork} " +
                    "quality=${selected.quality} latency=${selected.latency} reason=$reason",
            )
        } else {
            Log.d(TAG, "LOOLOO_TTS_FALLBACK reason=set_voice_failed name=${selected.name}")
        }
        // Strict local callers never fall back to an unspecified platform voice.
        return !offlineOnly || selectionResult == TextToSpeech.SUCCESS
    }

    private fun logAvailableVoice(voice: LooLooVoiceCandidate) {
        val features = voice.features.sorted().joinToString(",").ifBlank { "none" }
        Log.d(
            TAG,
            "LOOLOO_TTS_VOICE_AVAILABLE name=${voice.name} " +
                "locale=${voice.locale.toLanguageTag()} quality=${voice.quality} " +
                "latency=${voice.latency} network=${voice.requiresNetwork} features=$features",
        )
    }

    private fun speakIfCurrent(request: SpeechRequest) {
        if (isClosed.get() || speechGeneration.get() != request.generation) return
        val result = safely {
            textToSpeech?.speak(
                request.text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                utteranceId(request.generation),
            )
        }
        if (result == TextToSpeech.SUCCESS) {
            activeRequest = request
            Log.d(TAG, "LOOLOO_TTS_SPEAK")
        } else {
            logError("speak")
            finishRequestIfCurrent(request, succeeded = false)
        }
    }

    private fun completionListener() = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            executeUnlessClosed { finishSpeech(utteranceId, succeeded = true) }
        }

        @Deprecated("Deprecated by Android")
        override fun onError(utteranceId: String?) {
            executeUnlessClosed { finishSpeech(utteranceId, succeeded = false) }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            executeUnlessClosed { finishSpeech(utteranceId, succeeded = false) }
        }
    }

    private fun finishSpeech(utteranceId: String?, succeeded: Boolean) {
        val request = activeRequest ?: return
        if (utteranceId != utteranceId(request.generation)) return
        finishRequestIfCurrent(request, succeeded)
    }

    private fun finishRequestIfCurrent(request: SpeechRequest, succeeded: Boolean) {
        if (speechGeneration.get() != request.generation) return
        if (activeRequest === request) activeRequest = null
        request.onFinished?.invoke(succeeded)
    }

    private fun failPendingSpeech() {
        pendingSpeech.markUnavailable()?.let { request ->
            finishRequestIfCurrent(request, succeeded = false)
        }
    }

    private fun utteranceId(generation: Long) = "$RESPONSE_UTTERANCE_ID-$generation"

    private fun executeUnlessClosed(block: () -> Unit) {
        if (isClosed.get()) return
        try {
            executor.execute {
                if (!isClosed.get()) block()
            }
        } catch (_: RejectedExecutionException) {
            if (!isClosed.get()) logError("worker_rejected")
        }
    }

    private inline fun <T> safely(block: () -> T): T? = try {
        block()
    } catch (_: RuntimeException) {
        null
    }

    private fun logError(category: String) {
        Log.w(TAG, "LOOLOO_TTS_ERROR category=$category")
    }

    private companion object {
        const val TAG = "LooLooTts"
        const val SPEECH_RATE = 0.92f
        const val SPEECH_PITCH = 1.0f
        const val RESPONSE_UTTERANCE_ID = "looloo-jarvis-response"
    }
}
