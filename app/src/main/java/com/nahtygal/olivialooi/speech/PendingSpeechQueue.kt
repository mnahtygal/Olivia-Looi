package com.nahtygal.olivialooi.speech

/** Keeps at most one response while the platform TTS engine initializes. */
internal class PendingSpeechQueue {
    private var state = State.Initializing
    private var pendingRequest: SpeechRequest? = null

    @Synchronized
    fun offer(request: SpeechRequest): SpeechOffer = when (state) {
        State.Initializing -> {
            pendingRequest = request
            SpeechOffer.Queued
        }

        State.Ready -> SpeechOffer.Ready(request)
        State.Unavailable,
        State.Closed,
        -> SpeechOffer.Rejected
    }

    @Synchronized
    fun markReady(): SpeechRequest? {
        if (state != State.Initializing) return null
        state = State.Ready
        return pendingRequest.also { pendingRequest = null }
    }

    @Synchronized
    fun markUnavailable(): SpeechRequest? {
        if (state == State.Closed) return null
        state = State.Unavailable
        return pendingRequest.also { pendingRequest = null }
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
    val onFinished: ((Boolean) -> Unit)? = null,
)

internal sealed interface SpeechOffer {
    data object Queued : SpeechOffer
    data class Ready(val request: SpeechRequest) : SpeechOffer
    data object Rejected : SpeechOffer
}
