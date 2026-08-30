package com.nahtygal.olivialooi.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.MainThread
import androidx.annotation.RequiresPermission
import java.util.Locale

enum class SpeechRecognitionFailure {
    NoSpeech,
    Recognition,
}

/**
 * Owns at most one Android speech-recognition session.
 *
 * A fresh platform recognizer is created for each session. Destroying the old instance before a
 * retry prevents late callbacks from an earlier session from updating the current UI.
 */
class AndroidSpeechRecognizer(
    context: Context,
    private val onListening: () -> Unit,
    private val onProcessing: () -> Unit,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onLevelChanged: (Float) -> Unit,
    private val onFailure: (SpeechRecognitionFailure) -> Unit,
) : AutoCloseable {
    private val applicationContext = context.applicationContext
    private var activeSession: Session? = null
    private var nextSessionId = 0L
    private var isClosed = false
    private var smoothedLevel = 0f

    @MainThread
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        if (isClosed) return
        cancelActiveSession()

        if (!SpeechRecognizer.isRecognitionAvailable(applicationContext)) {
            onFailure(SpeechRecognitionFailure.Recognition)
            return
        }

        val sessionId = ++nextSessionId
        val recognizer = try {
            SpeechRecognizer.createSpeechRecognizer(applicationContext)
        } catch (_: IllegalArgumentException) {
            onFailure(SpeechRecognitionFailure.Recognition)
            return
        } catch (_: IllegalStateException) {
            onFailure(SpeechRecognitionFailure.Recognition)
            return
        } catch (_: UnsupportedOperationException) {
            onFailure(SpeechRecognitionFailure.Recognition)
            return
        }

        activeSession = Session(sessionId, recognizer)
        smoothedLevel = 0f
        recognizer.setRecognitionListener(listenerFor(sessionId, recognizer))

        try {
            recognizer.startListening(recognitionIntent())
        } catch (_: SecurityException) {
            finishWithFailure(sessionId, recognizer, SpeechRecognitionFailure.Recognition)
        } catch (_: IllegalArgumentException) {
            finishWithFailure(sessionId, recognizer, SpeechRecognitionFailure.Recognition)
        } catch (_: IllegalStateException) {
            finishWithFailure(sessionId, recognizer, SpeechRecognitionFailure.Recognition)
        } catch (_: UnsupportedOperationException) {
            finishWithFailure(sessionId, recognizer, SpeechRecognitionFailure.Recognition)
        }
    }

    /** Requests a final result after the child taps the existing "I'm Done" button. */
    @MainThread
    fun stopListening() {
        val session = activeSession ?: return
        session.isStopRequested = true
        try {
            session.recognizer.stopListening()
            ifCurrent(session.id, session.recognizer) {
                onProcessing()
                onLevelChanged(0f)
            }
        } catch (_: IllegalStateException) {
            finishWithFailure(
                session.id,
                session.recognizer,
                SpeechRecognitionFailure.Recognition,
            )
        }
    }

    @MainThread
    fun cancel() {
        cancelActiveSession()
        onLevelChanged(0f)
    }

    @MainThread
    override fun close() {
        if (isClosed) return
        isClosed = true
        cancelActiveSession()
    }

    private fun listenerFor(sessionId: Long, recognizer: SpeechRecognizer) =
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                ifAcceptingAudio(sessionId, recognizer, onListening)
            }

            override fun onBeginningOfSpeech() {
                ifAcceptingAudio(sessionId, recognizer, onListening)
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (!isAcceptingAudio(sessionId, recognizer)) return

                val level = speechRecognizerRmsToLevel(rmsdB)
                val smoothing = if (level > smoothedLevel) ATTACK_SMOOTHING else RELEASE_SMOOTHING
                smoothedLevel += (level - smoothedLevel) * smoothing
                if (level == 0f && smoothedLevel < ZERO_SNAP_LEVEL) smoothedLevel = 0f
                onLevelChanged(smoothedLevel)
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                ifCurrent(sessionId, recognizer) {
                    activeSession?.isStopRequested = true
                    onProcessing()
                    onLevelChanged(0f)
                }
            }

            override fun onError(error: Int) {
                val failure = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    -> SpeechRecognitionFailure.NoSpeech

                    else -> SpeechRecognitionFailure.Recognition
                }
                finishWithFailure(sessionId, recognizer, failure)
            }

            override fun onResults(results: Bundle?) {
                val recognizedText = bestRecognitionResult(
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION),
                )
                if (recognizedText == null) {
                    finishWithFailure(sessionId, recognizer, SpeechRecognitionFailure.NoSpeech)
                } else if (finishSession(sessionId, recognizer)) {
                    onLevelChanged(0f)
                    onFinalResult(recognizedText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partialText = bestRecognitionResult(
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION),
                ) ?: return
                ifCurrent(sessionId, recognizer) {
                    if (isAcceptingAudio(sessionId, recognizer)) onListening()
                    onPartialResult(partialText)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }

    private fun recognitionIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_RESULTS)
    }

    private fun finishWithFailure(
        sessionId: Long,
        recognizer: SpeechRecognizer,
        failure: SpeechRecognitionFailure,
    ) {
        if (!finishSession(sessionId, recognizer)) return
        onLevelChanged(0f)
        onFailure(failure)
    }

    private fun finishSession(sessionId: Long, recognizer: SpeechRecognizer): Boolean {
        if (!isCurrent(sessionId, recognizer)) return false
        activeSession = null
        recognizer.destroy()
        return true
    }

    private fun cancelActiveSession() {
        val session = activeSession ?: return
        activeSession = null
        try {
            session.recognizer.cancel()
        } catch (_: IllegalStateException) {
            // The platform service may already have ended this session.
        } finally {
            session.recognizer.destroy()
        }
    }

    private fun isCurrent(sessionId: Long, recognizer: SpeechRecognizer): Boolean {
        val session = activeSession
        return !isClosed && session?.id == sessionId && session.recognizer === recognizer
    }

    private fun isAcceptingAudio(sessionId: Long, recognizer: SpeechRecognizer): Boolean =
        isCurrent(sessionId, recognizer) && activeSession?.isStopRequested == false

    private inline fun ifCurrent(
        sessionId: Long,
        recognizer: SpeechRecognizer,
        callback: () -> Unit,
    ) {
        if (isCurrent(sessionId, recognizer)) callback()
    }

    private inline fun ifAcceptingAudio(
        sessionId: Long,
        recognizer: SpeechRecognizer,
        callback: () -> Unit,
    ) {
        if (isAcceptingAudio(sessionId, recognizer)) callback()
    }

    private data class Session(
        val id: Long,
        val recognizer: SpeechRecognizer,
        var isStopRequested: Boolean = false,
    )

    private companion object {
        const val MAX_RESULTS = 3
        const val ATTACK_SMOOTHING = 0.55f
        const val RELEASE_SMOOTHING = 0.2f
        const val ZERO_SNAP_LEVEL = 0.015f
    }
}
