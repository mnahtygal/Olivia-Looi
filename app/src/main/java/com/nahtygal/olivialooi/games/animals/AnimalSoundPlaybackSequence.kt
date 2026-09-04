package com.nahtygal.olivialooi.games.animals

/** Allows only the newest animal selection to advance from speech to local audio. */
class AnimalSoundPlaybackSequence {
    private var generation = 0L
    private var pendingGeneration: Long? = null

    @Synchronized
    fun beginSelection(): Long {
        generation += 1
        return generation.also { pendingGeneration = it }
    }

    /** Consumes a current completion so duplicate or stale callbacks cannot play audio. */
    @Synchronized
    fun completeSpeech(selectionGeneration: Long): Boolean {
        if (pendingGeneration != selectionGeneration) return false
        pendingGeneration = null
        return true
    }

    @Synchronized
    fun invalidate() {
        generation += 1
        pendingGeneration = null
    }
}
