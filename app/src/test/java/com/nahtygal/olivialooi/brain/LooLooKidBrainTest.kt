package com.nahtygal.olivialooi.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LooLooKidBrainTest {
    private val kidBrain = LooLooKidBrain()

    @Test
    fun promptContainsLooLooIdentity() {
        val prompt = kidBrain.buildPrompt("Why is the sky blue?").orEmpty()

        assertTrue(prompt.contains("You are LooLoo"))
        assertTrue(prompt.contains("friendly AI companion"))
    }

    @Test
    fun promptIdentifiesOlivia() {
        val prompt = kidBrain.buildPrompt("Tell me a joke.").orEmpty()

        assertTrue(prompt.contains("Olivia is a young child"))
        assertTrue(prompt.contains("Olivia said:"))
    }

    @Test
    fun childQuestionIsIncludedExactlyOnce() {
        val question = "How many legs does an octopus have?"
        val prompt = kidBrain.buildPrompt(question)

        assertNotNull(prompt)
        assertEquals(1, prompt.orEmpty().countOccurrences(question))
    }

    @Test
    fun instructionsRequestShortChildFriendlyResponses() {
        val prompt = kidBrain.buildPrompt("What is rain?").orEmpty()

        assertTrue(prompt.contains("1–3 short sentences"))
        assertTrue(prompt.contains("simple vocabulary"))
        assertTrue(prompt.contains("child-friendly language"))
    }

    @Test
    fun safetyGuidanceIsPresent() {
        val prompt = kidBrain.buildPrompt("Can I build something?").orEmpty()

        assertTrue(prompt.contains("trusted grown-up"))
        assertTrue(prompt.contains("dangerous activities"))
        assertTrue(prompt.contains("private identifying information"))
        assertTrue(prompt.contains("encourage secrets"))
        assertTrue(prompt.contains("adult sexual content"))
    }

    @Test
    fun blankInputIsHandledSafely() {
        assertNull(kidBrain.buildPrompt(""))
        assertNull(kidBrain.buildPrompt("  \n  "))
    }

    @Test
    fun promptBuilderDoesNotDuplicateChildMessage() {
        val message = "Please tell me one fun fact."
        val prompt = kidBrain.buildPrompt(message).orEmpty()

        assertEquals(1, prompt.countOccurrences(message))
        assertEquals(1, prompt.countOccurrences("[CHILD_MESSAGE]"))
    }

    private fun String.countOccurrences(value: String): Int {
        if (value.isEmpty()) return 0
        var count = 0
        var startIndex = 0
        while (true) {
            val match = indexOf(value, startIndex)
            if (match < 0) return count
            count += 1
            startIndex = match + value.length
        }
    }
}
