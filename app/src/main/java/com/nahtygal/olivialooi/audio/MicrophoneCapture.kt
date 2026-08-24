package com.nahtygal.olivialooi.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import androidx.annotation.RequiresPermission
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

/**
 * Owns one in-memory microphone capture session at a time.
 *
 * Audio samples are used only to calculate a small UI level indicator. They are never persisted
 * or exposed outside this class.
 */
class MicrophoneCapture(
    private val callbackExecutor: Executor,
    private val onStarted: () -> Unit,
    private val onAmplitude: (Float) -> Unit,
    private val onError: () -> Unit,
) : AutoCloseable {
    private val captureExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "LooLooMicrophone").apply { isDaemon = true }
    }
    private val isActive = AtomicBoolean(false)
    private val sessionGeneration = AtomicLong(0L)

    @Volatile
    private var isClosed = false

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        if (isClosed || !isActive.compareAndSet(false, true)) return

        val generation = sessionGeneration.incrementAndGet()
        try {
            captureExecutor.execute { captureAudio(generation) }
        } catch (_: RejectedExecutionException) {
            if (sessionGeneration.get() == generation) isActive.set(false)
        }
    }

    /** Signals the capture loop to stop. AudioRecord is stopped and released on its worker thread. */
    fun stop() {
        isActive.set(false)
        sessionGeneration.incrementAndGet()
    }

    override fun close() {
        if (isClosed) return
        isClosed = true
        stop()
        captureExecutor.shutdownNow()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun captureAudio(generation: Long) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

        var audioRecord: AudioRecord? = null
        var recordingStarted = false
        try {
            val minimumBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            if (minimumBufferSize <= 0) throw IllegalStateException("Unsupported audio format")

            audioRecord = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE_HZ)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(maxOf(minimumBufferSize, READ_BUFFER_BYTES))
                .build()

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                throw IllegalStateException("AudioRecord did not initialize")
            }
            if (!isCurrentSession(generation)) return

            audioRecord.startRecording()
            if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw IllegalStateException("AudioRecord did not start")
            }
            recordingStarted = true
            postIfCurrent(generation, onStarted)

            val samples = ShortArray(SAMPLES_PER_READ)
            var smoothedAmplitude = 0f
            var lastAmplitudeUpdateNanos = 0L

            while (isCurrentSession(generation)) {
                val sampleCount = audioRecord.read(
                    samples,
                    0,
                    samples.size,
                    AudioRecord.READ_BLOCKING,
                )
                if (sampleCount < 0) throw IllegalStateException("AudioRecord read failed: $sampleCount")
                if (sampleCount == 0) continue

                val amplitude = calculateAmplitude(samples, sampleCount)
                smoothedAmplitude = smoothedAmplitude * 0.7f + amplitude * 0.3f

                val now = System.nanoTime()
                if (now - lastAmplitudeUpdateNanos >= AMPLITUDE_UPDATE_INTERVAL_NANOS) {
                    val level = smoothedAmplitude
                    postIfCurrent(generation) { onAmplitude(level) }
                    lastAmplitudeUpdateNanos = now
                }
            }
        } catch (_: SecurityException) {
            postErrorIfCurrent(generation)
        } catch (_: IllegalArgumentException) {
            postErrorIfCurrent(generation)
        } catch (_: IllegalStateException) {
            postErrorIfCurrent(generation)
        } catch (_: UnsupportedOperationException) {
            postErrorIfCurrent(generation)
        } finally {
            if (recordingStarted) {
                try {
                    audioRecord?.stop()
                } catch (_: IllegalStateException) {
                    // The recorder may already have stopped after an audio-device failure.
                }
            }
            audioRecord?.release()

            if (sessionGeneration.get() == generation) {
                isActive.set(false)
            }
        }
    }

    private fun isCurrentSession(generation: Long): Boolean =
        !isClosed && isActive.get() && sessionGeneration.get() == generation

    private fun postIfCurrent(generation: Long, callback: () -> Unit) {
        callbackExecutor.execute {
            if (isCurrentSession(generation)) callback()
        }
    }

    private fun postErrorIfCurrent(generation: Long) {
        if (!isCurrentSession(generation)) return

        isActive.set(false)
        val errorGeneration = generation + 1L
        if (!sessionGeneration.compareAndSet(generation, errorGeneration)) return
        callbackExecutor.execute {
            if (
                !isClosed &&
                !isActive.get() &&
                sessionGeneration.get() == errorGeneration
            ) {
                onError()
            }
        }
    }

    private fun calculateAmplitude(samples: ShortArray, sampleCount: Int): Float {
        var sumOfSquares = 0.0
        for (index in 0 until sampleCount) {
            val sample = samples[index].toDouble()
            sumOfSquares += sample * sample
        }

        val rootMeanSquare = sqrt(sumOfSquares / sampleCount)
        return (rootMeanSquare / AMPLITUDE_SCALE).toFloat().coerceIn(0f, 1f)
    }

    private companion object {
        const val SAMPLE_RATE_HZ = 16_000
        const val SAMPLES_PER_READ = 320
        const val READ_BUFFER_BYTES = SAMPLES_PER_READ * 2
        const val AMPLITUDE_SCALE = 8_000.0
        const val AMPLITUDE_UPDATE_INTERVAL_NANOS = 80_000_000L
    }
}
