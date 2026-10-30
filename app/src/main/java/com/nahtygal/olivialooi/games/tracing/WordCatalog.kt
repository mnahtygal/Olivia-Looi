package com.nahtygal.olivialooi.games.tracing

import java.util.Collections
import java.util.Locale

data class WordCategory(
    val stableId: String,
    val wordLength: Int,
    val emoji: String,
    val title: String,
)

data class TraceWord(
    val stableId: String,
    val categoryId: String,
    val value: String,
)

object WordCatalog {
    const val THREE_LETTER_ID = "three-letter"
    const val FOUR_LETTER_ID = "four-letter"
    const val FIVE_LETTER_ID = "five-letter"

    val categories: List<WordCategory> = Collections.unmodifiableList(
        listOf(
            WordCategory(THREE_LETTER_ID, 3, "🐱", "3-Letter Words"),
            WordCategory(FOUR_LETTER_ID, 4, "🐸", "4-Letter Words"),
            WordCategory(FIVE_LETTER_ID, 5, "🍎", "5-Letter Words"),
        ),
    )

    private val wordsByCategory: Map<String, List<TraceWord>> = Collections.unmodifiableMap(
        categories.associate { category ->
            val values = when (category.stableId) {
                THREE_LETTER_ID -> listOf("CAT", "DOG", "SUN", "HAT", "BUG", "PIG", "COW", "FOX", "BED", "CAR")
                FOUR_LETTER_ID -> listOf("FISH", "BIRD", "DUCK", "FROG", "STAR", "MOON", "TREE", "BEAR", "LION", "BOAT")
                FIVE_LETTER_ID -> listOf("APPLE", "HOUSE", "HAPPY", "TIGER", "WATER", "SMILE", "GREEN", "TRAIN", "HEART", "PIZZA")
                else -> error("Unknown word category ${category.stableId}")
            }
            category.stableId to Collections.unmodifiableList(
                values.map { value ->
                    TraceWord(
                        stableId = "${category.stableId}-${value.lowercase(Locale.US)}",
                        categoryId = category.stableId,
                        value = value,
                    )
                },
            )
        },
    )

    val allWords: List<TraceWord> = Collections.unmodifiableList(categories.flatMap { words(it.stableId) })

    fun category(stableId: String): WordCategory? = categories.firstOrNull { it.stableId == stableId }

    fun words(categoryId: String): List<TraceWord> = wordsByCategory[categoryId].orEmpty()

    fun word(stableId: String): TraceWord? = allWords.firstOrNull { it.stableId == stableId }
}

object WordTracingSpeech {
    fun hearWord(word: TraceWord): String =
        "Let's write ${word.value}. ${word.value.toList().joinToString(", ")}. ${word.value.lowercase().replaceFirstChar(Char::uppercase)}!"

    fun completion(childName: String, word: TraceWord): String =
        "Hooray, $childName! You wrote ${word.value}! ${word.value.toList().joinToString(", ")} spells ${word.value.lowercase()}!"
}
