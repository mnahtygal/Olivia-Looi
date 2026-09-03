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
        assertTrue(prompt.contains("friendly AI"))
    }

    @Test
    fun promptIdentifiesOlivia() {
        val prompt = kidBrain.buildPrompt("Tell me a joke.").orEmpty()

        assertTrue(prompt.contains("young Olivia"))
        assertTrue(prompt.contains("Olivia:"))
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

        assertTrue(prompt.contains("1-3 simple"))
        assertTrue(prompt.contains("warm"))
        assertTrue(prompt.contains("child-safe"))
    }

    @Test
    fun safetyGuidanceIsPresent() {
        val prompt = kidBrain.buildPrompt("Can I build something?").orEmpty()

        assertTrue(prompt.contains("trusted grown-up"))
        assertTrue(prompt.contains("danger"))
        assertTrue(prompt.contains("Safety rules override Olivia"))
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
        assertEquals(1, prompt.countOccurrences("Olivia:"))
    }

    @Test
    fun promptNeverExceedsHandheldUtf8Limit() {
        val prompt = kidBrain.buildPrompt("🌨️".repeat(200)).orEmpty()

        assertTrue(prompt.toByteArray(Charsets.UTF_8).size <= LooLooKidBrain.MAX_PROMPT_UTF8_BYTES)
        assertTrue(prompt.endsWith(LooLooKidBrain.TRUNCATION_MARKER))
        assertTrue(!prompt.contains('\uFFFD'))
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
