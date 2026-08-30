package com.nahtygal.olivialooi.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Owns LooLoo's platform TextToSpeech engine and its bounded pending response. */
internal class AndroidTextToSpeech(context: Context) : AutoCloseable {
    private val applicationContext = context.applicationContext
    private val pendingSpeech = PendingSpeechQueue()
    private val isClosed = AtomicBoolean(false)
    private val speechGeneration = AtomicLong(0L)
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "looloo-tts").apply { isDaemon = true }
    }

    @Volatile
    private var textToSpeech: TextToSpeech? = null

    init {
        Log.d(TAG, "LOOLOO_TTS_INITIALIZING")
        executeUnlessClosed(::createTextToSpeech)
    }

    fun speak(text: String) {
        if (text.isBlank() || isClosed.get()) return
        val request = SpeechRequest(text, speechGeneration.incrementAndGet())
        pendingSpeech.offer(request)?.let { readyRequest ->
            executeUnlessClosed { speakIfCurrent(readyRequest) }
        }
    }

    fun stop() {
        if (isClosed.get()) return
        speechGeneration.incrementAndGet()
        pendingSpeech.stop()
        executeUnlessClosed {
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
            pendingSpeech.markUnavailable()
            Log.w(TAG, "LOOLOO_TTS_ERROR category=construction")
            null
        }
    }

    private fun finishInitialization(status: Int) {
        val engine = textToSpeech
        if (status != TextToSpeech.SUCCESS || engine == null) {
            pendingSpeech.markUnavailable()
            logError("initialization")
            return
        }

        val languageResult = safely { engine.setLanguage(Locale.US) }
        if (
            languageResult == null ||
            languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            pendingSpeech.markUnavailable()
            logError("english_unavailable")
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

    private fun speakIfCurrent(request: SpeechRequest) {
        if (isClosed.get() || speechGeneration.get() != request.generation) return
        val result = safely {
            textToSpeech?.speak(
                request.text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                RESPONSE_UTTERANCE_ID,
            )
        }
        if (result == TextToSpeech.SUCCESS) {
            Log.d(TAG, "LOOLOO_TTS_SPEAK")
        } else {
            logError("speak")
        }
    }

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
