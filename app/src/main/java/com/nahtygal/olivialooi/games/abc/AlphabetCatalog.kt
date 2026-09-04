package com.nahtygal.olivialooi.games.abc

data class AlphabetEntry(
    val stableId: String,
    val letter: Char,
    val displayWord: String,
    val spokenWord: String,
    val visualSymbol: String,
    val accessibilityDescription: String,
)

object AlphabetCatalog {
    val entries: List<AlphabetEntry> = listOf(
        entry('A', "Apple", "🍎"),
        entry('B', "Ball", "⚽"),
        entry('C', "Cat", "🐱"),
        entry('D', "Dog", "🐶"),
        entry('E', "Egg", "🥚"),
        entry('F', "Fish", "🐟"),
        entry('G', "Goat", "🐐"),
        entry('H', "Hat", "🎩"),
        entry('I', "Ice Cream", "🍦"),
        entry('J', "Juice", "🧃"),
        entry('K', "Kite", "🪁"),
        entry('L', "Lion", "🦁"),
        entry('M', "Moon", "🌙"),
        entry('N', "Nest", "🪺"),
        entry('O', "Orange", "🍊"),
        entry('P', "Pig", "🐷"),
        entry('Q', "Queen", "👑"),
        entry('R', "Rainbow", "🌈"),
        entry('S', "Star", "⭐"),
        entry('T', "Tree", "🌳"),
        entry('U', "Umbrella", "☂️"),
        entry('V', "Violin", "🎻"),
        entry('W', "Whale", "🐋"),
        entry('X', "Xylophone", "🎵"),
        entry('Y', "Yo-Yo", "🪀"),
        entry('Z', "Zebra", "🦓"),
    )

    fun findByLetter(letter: Char): AlphabetEntry? =
        entries.firstOrNull { it.letter == letter.uppercaseChar() }

    fun findById(stableId: String): AlphabetEntry? =
        entries.firstOrNull { it.stableId == stableId }

    private fun entry(letter: Char, word: String, visual: String) = AlphabetEntry(
        stableId = letter.lowercaseChar().toString(),
        letter = letter,
        displayWord = word,
        spokenWord = word,
        visualSymbol = visual,
        accessibilityDescription = "Letter $letter. $word.",
    )
}
