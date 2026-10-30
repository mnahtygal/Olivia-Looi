package com.nahtygal.olivialooi.games.tracing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LetterVocabularyTest {
    private val expected = linkedMapOf(
        'A' to "Apple",
        'B' to "Boat",
        'C' to "Cookie",
        'D' to "Daddy",
        'E' to "Eliana",
        'F' to "Frog",
        'G' to "Gigi",
        'H' to "Happy",
        'I' to "Ice Cream",
        'J' to "Juice",
        'K' to "Kelly",
        'L' to "Lion",
        'M' to "Mommy",
        'N' to "Nahtygal",
        'O' to "Olivia",
        'P' to "Papa",
        'Q' to "Queen",
        'R' to "Ring",
        'S' to "Sun",
        'T' to "Turtle",
        'U' to "Umbrella",
        'V' to "Vegetables",
        'W' to "Whale",
        'X' to "X-ray",
        'Y' to "Yellow",
        'Z' to "Zoo",
    )

    @Test
    fun `catalog has exactly all 26 specified uppercase entries`() {
        assertEquals(26, LetterVocabulary.words.size)
        assertEquals(('A'..'Z').toSet(), LetterVocabulary.words.keys)
        assertEquals(expected, LetterVocabulary.words)
    }

    @Test
    fun `every lowercase letter resolves to the same uppercase vocabulary word`() {
        ('a'..'z').forEach { lowercase ->
            assertEquals(expected[lowercase.uppercaseChar()], LetterVocabulary.wordFor(lowercase))
        }
    }

    @Test
    fun `personalized vocabulary phrases are exact`() {
        assertEquals("G is for Gigi.", LetterVocabulary.vocabularyPhrase('G'))
        assertEquals("N is for Nahtygal.", LetterVocabulary.vocabularyPhrase('N'))
        assertEquals("O is for Olivia.", LetterVocabulary.vocabularyPhrase('O'))
        assertEquals("P is for Papa.", LetterVocabulary.vocabularyPhrase('P'))
    }

    @Test
    fun `uppercase speech uses required complete format for all letters`() {
        expected.forEach { (letter, word) ->
            assertEquals(
                "This is the letter $letter. $letter. $letter is for $word.",
                LetterVocabulary.hearLetterPhrase(TracingMode.ALPHABET, letter),
            )
        }
    }

    @Test
    fun `lowercase speech uses required complete format for all letters`() {
        expected.forEach { (uppercase, word) ->
            val lowercase = uppercase.lowercaseChar()
            assertEquals(
                "This is lowercase $lowercase. $uppercase is for $word.",
                LetterVocabulary.hearLetterPhrase(TracingMode.LOWERCASE, lowercase),
            )
        }
    }

    @Test
    fun `name tracing speech remains unchanged and has no vocabulary suffix`() {
        assertEquals(
            "This is the letter O. O!",
            LetterVocabulary.hearLetterPhrase(TracingMode.NAME, 'O'),
        )
        assertEquals(
            "This is the letter A. A!",
            LetterVocabulary.hearLetterPhrase(TracingMode.NAME, 'A'),
        )
    }

    @Test
    fun `catalog rejects unsupported characters and cannot be mutated`() {
        assertThrows(IllegalArgumentException::class.java) { LetterVocabulary.vocabularyPhrase('?') }
        @Suppress("UNCHECKED_CAST")
        val mutableView = LetterVocabulary.words as MutableMap<Char, String>
        assertThrows(UnsupportedOperationException::class.java) { mutableView['A'] = "Ant" }
    }
}
