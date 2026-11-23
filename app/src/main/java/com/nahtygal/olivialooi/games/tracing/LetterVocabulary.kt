package com.nahtygal.olivialooi.games.tracing

import java.util.Collections

/** Local, fixed vocabulary shared by uppercase and lowercase handwriting speech. */
object LetterVocabulary {
    val words: Map<Char, String> = Collections.unmodifiableMap(
        linkedMapOf(
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
        ),
    )

    fun wordFor(letter: Char): String? = words[letter.uppercaseChar()]

    fun vocabularyPhrase(letter: Char): String {
        val uppercase = letter.uppercaseChar()
        val word = requireNotNull(words[uppercase]) { "No handwriting vocabulary for $letter" }
        return "$uppercase is for $word."
    }

    fun hearLetterPhrase(mode: TracingMode, letter: Char): String {
        val uppercase = letter.uppercaseChar()
        return when (mode) {
            TracingMode.NAME -> "This is the letter $uppercase. $uppercase!"
            TracingMode.ALPHABET -> "This is the letter $uppercase. $uppercase. ${vocabularyPhrase(uppercase)}"
            TracingMode.LOWERCASE -> "This is lowercase ${letter.lowercaseChar()}. ${vocabularyPhrase(uppercase)}"
        }
    }
}
