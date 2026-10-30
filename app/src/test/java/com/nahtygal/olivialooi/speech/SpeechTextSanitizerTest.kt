package com.nahtygal.olivialooi.speech

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechTextSanitizerTest {
    @Test
    fun `single emoji is removed`() {
        assertEquals("That was funny!", SpeechTextSanitizer.sanitize("That was funny! 😂"))
    }

    @Test
    fun `multiple emojis are removed without extra spaces`() {
        assertEquals("Great job!", SpeechTextSanitizer.sanitize("Great job! 😀 😃 🎉"))
    }

    @Test
    fun `emoji inside a sentence leaves natural spacing`() {
        assertEquals("Hello there!", SpeechTextSanitizer.sanitize("Hello 😊 there!"))
        assertEquals("Wow! Really?", SpeechTextSanitizer.sanitize("Wow 😂! Really?"))
    }

    @Test
    fun `joined skin tone flag and keycap sequences are removed completely`() {
        assertEquals(
            "Astronaut family flag number done.",
            SpeechTextSanitizer.sanitize(
                "Astronaut 👩🏽‍🚀 family 👨‍👩‍👧‍👦 flag 🇺🇸 number 1️⃣ done.",
            ),
        )
    }

    @Test
    fun `text without emojis is unchanged`() {
        val text = "Why don't skeletons fight each other? They don't have the guts!"
        assertEquals(text, SpeechTextSanitizer.sanitize(text))
    }

    @Test
    fun `normal punctuation and numbers are preserved`() {
        val text = "Room 10, choice #1: 2 × 3 = 6; yes—really!"
        assertEquals(text, SpeechTextSanitizer.sanitize(text))
    }

    @Test
    fun `accented and meaningful Unicode text is preserved`() {
        val text = "Olivia said, “Très bien, Gigi!” Café mañana."
        assertEquals(text, SpeechTextSanitizer.sanitize(text))
    }

    @Test
    fun `text symbols remain unless explicitly presented as emoji`() {
        assertEquals("© 2026 ACME®", SpeechTextSanitizer.sanitize("© 2026 ACME®"))
        assertEquals("2026 ACME", SpeechTextSanitizer.sanitize("©️ 2026 ACME®️"))
    }

    @Test
    fun `emoji-only and empty input produce empty speech text`() {
        assertEquals("", SpeechTextSanitizer.sanitize("🧒🏾‍🚀🎈"))
        assertEquals("", SpeechTextSanitizer.sanitize(""))
    }
}
