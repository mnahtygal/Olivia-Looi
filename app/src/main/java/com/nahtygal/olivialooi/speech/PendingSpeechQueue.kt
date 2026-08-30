package com.nahtygal.olivialooi.speech

/** Keeps at most one response while the platform TTS engine initializes. */
internal class PendingSpeechQueue {
    private var state = State.Initializing
    private var pendingRequest: SpeechRequest? = null

    @Synchronized
    fun offer(request: SpeechRequest): SpeechRequest? = when (state) {
        State.Initializing -> {
            pendingRequest = request
            null
        }

        State.Ready -> request
        State.Unavailable,
        State.Closed,
        -> null
    }

    @Synchronized
    fun markReady(): SpeechRequest? {
        if (state != State.Initializing) return null
        state = State.Ready
        return pendingRequest.also { pendingRequest = null }
    }

    @Synchronized
    fun markUnavailable() {
        if (state == State.Closed) return
        state = State.Unavailable
        pendingRequest = null
    }

    @Synchronized
    fun stop() {
        pendingRequest = null
    }

    @Synchronized
    fun close() {
        state = State.Closed
        pendingRequest = null
    }

    private enum class State {
        Initializing,
        Ready,
        Unavailable,
        Closed,
    }
}

internal data class SpeechRequest(
    val text: String,
    val generation: Long,
)
