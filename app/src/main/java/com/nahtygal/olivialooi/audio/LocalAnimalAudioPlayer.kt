package com.nahtygal.olivialooi.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.nahtygal.olivialooi.R
import java.util.concurrent.atomic.AtomicBoolean

/** Owns at most one packaged animal sound and performs all MediaPlayer work on the main thread. */
internal class LocalAnimalAudioPlayer(context: Context) : AutoCloseable {
    private val applicationContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isClosed = AtomicBoolean(false)
    private var mediaPlayer: MediaPlayer? = null

    fun play(localAudioAssetName: String) = onMainThread {
        if (isClosed.get()) return@onMainThread
        releaseCurrent()
        val resourceId = resolveRawResource(localAudioAssetName)
        if (resourceId == null) {
            logError("resource")
            return@onMainThread
        }

        try {
            val player = MediaPlayer.create(applicationContext, resourceId)
            if (player == null) {
                logError("create")
                return@onMainThread
            }
            mediaPlayer = player
            player.setOnCompletionListener { completed ->
                if (mediaPlayer === completed) releaseCurrent()
            }
            player.setOnErrorListener { failed, _, _ ->
                if (mediaPlayer === failed) releaseCurrent()
                logError("playback")
                true
            }
            player.start()
        } catch (_: RuntimeException) {
            releaseCurrent()
            logError("start")
        }
    }

    fun stop() = onMainThread { releaseCurrent() }

    override fun close() {
        if (!isClosed.compareAndSet(false, true)) return
        onMainThread { releaseCurrent() }
    }

    private fun releaseCurrent() {
        val player = mediaPlayer ?: return
        mediaPlayer = null
        try {
            player.stop()
        } catch (_: RuntimeException) {
            // A failed or completed player may already be stopped.
        }
        try {
            player.release()
        } catch (_: RuntimeException) {
            logError("release")
        }
    }

    private fun onMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private fun resolveRawResource(localAudioAssetName: String): Int? = when (localAudioAssetName) {
        "animal_cow_moo" -> R.raw.animal_cow_moo
        "animal_dog_bark" -> R.raw.animal_dog_bark
        "animal_cat_meow" -> R.raw.animal_cat_meow
        "animal_pig_oink" -> R.raw.animal_pig_oink
        "animal_duck_quack" -> R.raw.animal_duck_quack
        "animal_sheep_baa" -> R.raw.animal_sheep_baa
        "animal_horse_neigh" -> R.raw.animal_horse_neigh
        "animal_frog_ribbit" -> R.raw.animal_frog_ribbit
        else -> null
    }

    private fun logError(category: String) {
        Log.w(TAG, "LOOLOO_ANIMAL_AUDIO_ERROR category=$category")
    }

    private companion object {
        const val TAG = "LooLooAnimalAudio"
    }
}
