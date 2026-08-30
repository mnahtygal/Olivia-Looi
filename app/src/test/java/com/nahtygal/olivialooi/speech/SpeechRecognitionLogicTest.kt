package com.nahtygal.olivialooi.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechRecognitionLogicTest {
    @Test
    fun bestResultUsesFirstNonBlankCandidate() {
        assertEquals(
            "I want to color!",
            bestRecognitionResult(listOf("  ", " I want to color! ", "I want color")),
        )
    }

    @Test
    fun bestResultReturnsNullWithoutSpeech() {
        assertNull(bestRecognitionResult(null))
        assertNull(bestRecognitionResult(listOf("", "  ")))
    }

    @Test
    fun rmsLevelSeparatesSilenceSpeechAndLoudInput() {
        val silence = speechRecognizerRmsToLevel(0f)
        val speech = speechRecognizerRmsToLevel(5f)
        val loud = speechRecognizerRmsToLevel(10f)

        assertEquals(0f, silence, 0f)
        assertTrue(speech in 0.4f..0.5f)
        assertEquals(1f, loud, 0f)
    }
}
