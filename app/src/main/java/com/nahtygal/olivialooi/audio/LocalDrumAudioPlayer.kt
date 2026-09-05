package com.nahtygal.olivialooi.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.nahtygal.olivialooi.R
import com.nahtygal.olivialooi.games.drums.DrumSound

internal enum class DrumAudioStatus { LOADING, READY, UNAVAILABLE }

/** One preloaded pool per drum screen. All entry points and load callbacks run on main.
 * SoundPool decodes short packaged PCM samples asynchronously; taps only call play().
 * Voices decay naturally and may overlap; the bounded pool steals oldest voices under load.
 * There is deliberately no playback queue and no retry backlog for taps during loading.
 */
internal class LocalDrumAudioPlayer(context: Context, private val onStatus: (DrumAudioStatus) -> Unit) : AutoCloseable {
    private var pool: SoundPool? = null
    private val samples = IntArray(DrumSound.entries.size)
    private val loaded = BooleanArray(DrumSound.entries.size)
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
                        onStatus(DrumAudioStatus.UNAVAILABLE)
                    } else {
                        loaded[index] = true
                        if (loaded.all { it }) onStatus(DrumAudioStatus.READY)
                    }
                }
            }
            DrumSound.entries.forEach { note ->
                samples[note.ordinal] = soundPool.load(context.applicationContext, rawResource(note), 1)
                if (samples[note.ordinal] == 0) failed = true
            }
            if (failed) onStatus(DrumAudioStatus.UNAVAILABLE)
        } catch (_: RuntimeException) {
            failed = true
            onStatus(DrumAudioStatus.UNAVAILABLE)
            pool?.release()
            pool = null
        }
    }

    fun play(note: DrumSound) {
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

    private fun rawResource(note: DrumSound): Int = when (note) {
        DrumSound.KICK -> R.raw.drum_kick
        DrumSound.SNARE -> R.raw.drum_snare
        DrumSound.TOM -> R.raw.drum_tom
        DrumSound.CLAP -> R.raw.drum_clap
        DrumSound.TAMBOURINE -> R.raw.drum_tambourine
        DrumSound.CYMBAL -> R.raw.drum_cymbal
    }
    private companion object { const val MAX_VOICES = 16 }
}
