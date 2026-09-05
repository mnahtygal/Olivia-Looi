package com.nahtygal.olivialooi.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.games.piano.PianoNote

internal enum class PianoAudioStatus { LOADING, READY, UNAVAILABLE }

/** One preloaded pool per piano screen. All entry points and load callbacks run on main.
 * SoundPool decodes short packaged PCM samples asynchronously; taps only call play().
 * Voices decay naturally and may overlap; the bounded pool steals oldest voices under load.
 * There is deliberately no playback queue and no retry backlog for taps during loading.
 */
internal class LocalPianoAudioPlayer(context: Context, private val onStatus: (PianoAudioStatus) -> Unit) : AutoCloseable {
    private var pool: SoundPool? = null
    private val samples = IntArray(PianoNote.entries.size)
    private val loaded = BooleanArray(PianoNote.entries.size)
    private val streams = IntArray(MAX_VOICES)
    private var nextStream = 0
    private var closed = false
    private var suspended = false
    private var failed = false

    init {
        try {
            val soundPool = SoundPool.Builder().setMaxStreams(MAX_VOICES)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                .build()
            pool = soundPool
            soundPool.setOnLoadCompleteListener { _, sampleId, status ->
                if (!closed && !failed) {
                    val index = samples.indexOf(sampleId)
                    if (status != 0 || index < 0) {
                        failed = true
                        onStatus(PianoAudioStatus.UNAVAILABLE)
                    } else {
                        loaded[index] = true
                        if (loaded.all { it }) onStatus(PianoAudioStatus.READY)
                    }
                }
            }
            PianoNote.entries.forEach { note ->
                samples[note.ordinal] = soundPool.load(context.applicationContext, rawResource(note), 1)
                if (samples[note.ordinal] == 0) failed = true
            }
            if (failed) onStatus(PianoAudioStatus.UNAVAILABLE)
        } catch (_: RuntimeException) {
            failed = true
            onStatus(PianoAudioStatus.UNAVAILABLE)
            pool?.release()
            pool = null
        }
    }

    fun play(note: PianoNote) {
        if (closed || suspended || failed || !loaded[note.ordinal]) return
        val soundPool = pool ?: return
        // A reused slot represents the oldest of at most 16 voices; it never grows a queue.
        if (streams[nextStream] != 0) soundPool.stop(streams[nextStream])
        streams[nextStream] = soundPool.play(samples[note.ordinal], .65f, .65f, 1, 0, 1f)
        nextStream = (nextStream + 1) % MAX_VOICES
    }

    fun suspend() {
        suspended = true
        stopAll()
    }
    fun resume() { suspended = false } // Do not replay voices paused by backgrounding.

    fun stopAll() {
        if (closed) return
        val soundPool = pool ?: return
        for (i in streams.indices) {
            if (streams[i] != 0) soundPool.stop(streams[i])
            streams[i] = 0
        }
        nextStream = 0
    }

    override fun close() {
        if (closed) return
        closed = true
        pool?.setOnLoadCompleteListener(null)
        pool?.release()
        pool = null
        streams.fill(0)
        loaded.fill(false)
    }

    private fun rawResource(note: PianoNote): Int = when (note) {
        PianoNote.C4 -> R.raw.piano_c4
        PianoNote.D4 -> R.raw.piano_d4
        PianoNote.E4 -> R.raw.piano_e4
        PianoNote.F4 -> R.raw.piano_f4
        PianoNote.G4 -> R.raw.piano_g4
        PianoNote.A4 -> R.raw.piano_a4
        PianoNote.B4 -> R.raw.piano_b4
        PianoNote.C5 -> R.raw.piano_c5
    }
    private companion object { const val MAX_VOICES = 16 }
}
