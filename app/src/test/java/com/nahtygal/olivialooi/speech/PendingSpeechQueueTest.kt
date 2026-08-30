package com.nahtygal.olivialooi.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingSpeechQueueTest {
    @Test
    fun initializationKeepsOnlyNewestPendingResponse() {
        val queue = PendingSpeechQueue()
        val first = SpeechRequest("First response", generation = 1)
        val newest = SpeechRequest("Newest response", generation = 2)

        assertNull(queue.offer(first))
        assertNull(queue.offer(newest))

        assertEquals(newest, queue.markReady())
        assertNull(queue.markReady())
    }

    @Test
    fun readyQueueReturnsTextForImmediateSpeech() {
        val queue = PendingSpeechQueue()
        queue.markReady()
        val request = SpeechRequest("LooLoo response", generation = 1)

        assertEquals(request, queue.offer(request))
    }

    @Test
    fun stopClearsPendingResponse() {
        val queue = PendingSpeechQueue()
        queue.offer(SpeechRequest("Do not speak this", generation = 1))

        queue.stop()

        assertNull(queue.markReady())
    }

    @Test
    fun unavailableAndClosedQueuesRejectSpeech() {
        val unavailable = PendingSpeechQueue()
        unavailable.offer(SpeechRequest("Pending", generation = 1))
        unavailable.markUnavailable()

        assertNull(unavailable.offer(SpeechRequest("Later", generation = 2)))
        assertNull(unavailable.markReady())

        val closed = PendingSpeechQueue()
        closed.close()
        assertNull(closed.offer(SpeechRequest("Never speak", generation = 1)))
        assertNull(closed.markReady())
    }
}
