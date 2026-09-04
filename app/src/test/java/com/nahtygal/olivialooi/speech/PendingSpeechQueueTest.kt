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

        assertEquals(SpeechOffer.Queued, queue.offer(first))
        assertEquals(SpeechOffer.Queued, queue.offer(newest))

        assertEquals(newest, queue.markReady())
        assertNull(queue.markReady())
    }

    @Test
    fun readyQueueReturnsTextForImmediateSpeech() {
        val queue = PendingSpeechQueue()
        queue.markReady()
        val request = SpeechRequest("LooLoo response", generation = 1)

        assertEquals(SpeechOffer.Ready(request), queue.offer(request))
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

        assertEquals(SpeechOffer.Rejected, unavailable.offer(SpeechRequest("Later", generation = 2)))
        assertNull(unavailable.markReady())

        val closed = PendingSpeechQueue()
        closed.close()
        assertEquals(SpeechOffer.Rejected, closed.offer(SpeechRequest("Never speak", generation = 1)))
        assertNull(closed.markReady())
    }
}
