package com.nahtygal.olivialooi.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MicrophoneLevelTest {
    @Test
    fun silenceMapsToZero() {
        assertEquals(0f, pcm16RmsToLevel(ShortArray(320), 320), 0f)
    }

    @Test
    fun speechAndLoudInputProduceClearlyDifferentLevels() {
        val quiet = pcm16RmsToLevel(ShortArray(320) { 100 }, 320)
        val speech = pcm16RmsToLevel(ShortArray(320) { 1_000 }, 320)
        val loud = pcm16RmsToLevel(ShortArray(320) { 8_000 }, 320)

        assertTrue(quiet in 0f..0.2f)
        assertTrue(speech in 0.4f..0.7f)
        assertTrue(loud > 0.85f)
        assertTrue(quiet < speech && speech < loud)
    }
}
