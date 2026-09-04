package com.nahtygal.olivialooi.games.spelling

import kotlin.random.Random

enum class SpellingLevel {
    Level1,
    Level2,
    Level3,
}

data class SpellingWord(
    val text: String,
    val clue: String,
)

object SpellingWordCatalog {
    private val wordsByLevel = mapOf(
        SpellingLevel.Level1 to listOf(
            SpellingWord("CAT", "🐱"),
            SpellingWord("DOG", "🐶"),
            SpellingWord("COW", "🐮"),
            SpellingWord("PIG", "🐷"),
            SpellingWord("SUN", "☀️"),
            SpellingWord("CAR", "🚗"),
            SpellingWord("RED", "🔴"),
            SpellingWord("BED", "🛏️"),
            SpellingWord("HAT", "🎩"),
            SpellingWord("BUG", "🐞"),
        ),
        SpellingLevel.Level2 to listOf(
            SpellingWord("STAR", "⭐"),
            SpellingWord("BLUE", "🔵"),
            SpellingWord("PINK", "🌸"),
            SpellingWord("DUCK", "🦆"),
            SpellingWord("FISH", "🐟"),
            SpellingWord("FROG", "🐸"),
            SpellingWord("BIRD", "🐦"),
            SpellingWord("MOON", "🌙"),
            SpellingWord("BOOK", "📖"),
            SpellingWord("TREE", "🌳"),
        ),
        SpellingLevel.Level3 to listOf(
            SpellingWord("APPLE", "🍎"),
            SpellingWord("HOUSE", "🏠"),
            SpellingWord("PUPPY", "🐶"),
            SpellingWord("FLOWER", "🌸"),
            SpellingWord("HAPPY", "😊"),
            SpellingWord("GREEN", "🟢"),
            SpellingWord("WATER", "💧"),
            SpellingWord("HORSE", "🐴"),
            SpellingWord("HEART", "💜"),
            SpellingWord("LOOLOO", "❄️"),
        ),
    )

    fun words(level: SpellingLevel): List<SpellingWord> = wordsByLevel.getValue(level)
}

data class LetterTile(
    val id: Int,
    val letter: Char,
    val isAvailable: Boolean = true,
)

enum class SpellingRoundState {
    Playing,
    Correct,
    Incorrect,
    SessionComplete,
}

data class SpeakAndSpellState(
    val level: SpellingLevel,
    val currentWord: SpellingWord,
    val previousWord: SpellingWord? = null,
    val letterBank: List<LetterTile>,
    val enteredTileIds: List<Int> = emptyList(),
    val helpCount: Int = 0,
    val completedWordCount: Int = 0,
    val roundState: SpellingRoundState = SpellingRoundState.Playing,
) {
    val enteredLetters: List<Char>
        get() = enteredTileIds.map { tileId -> letterBank.first { it.id == tileId }.letter }

    val answerSlots: List<Char?>
        get() = List(currentWord.text.length) { index -> enteredLetters.getOrNull(index) }

    val firstLetterHint: Char?
        get() = currentWord.text.firstOrNull().takeIf { helpCount >= 2 }

    val inputLocked: Boolean
        get() = roundState != SpellingRoundState.Playing
}

object SpeakAndSpellEngine {
    private const val SESSION_WORD_COUNT = 5

    fun newGame(
        level: SpellingLevel,
        random: Random = Random.Default,
    ): SpeakAndSpellState = createRound(
        level = level,
        previousWord = null,
        completedWordCount = 0,
        random = random,
    )

    fun selectTile(state: SpeakAndSpellState, tileId: Int): SpeakAndSpellState {
        if (state.inputLocked || state.enteredTileIds.size >= state.currentWord.text.length) return state
        val tileIndex = state.letterBank.indexOfFirst { it.id == tileId }
        if (tileIndex < 0 || !state.letterBank[tileIndex].isAvailable) return state

        val updatedBank = state.letterBank.toMutableList().also { bank ->
            bank[tileIndex] = bank[tileIndex].copy(isAvailable = false)
        }
        val enteredIds = state.enteredTileIds + tileId
        if (enteredIds.size < state.currentWord.text.length) {
            return state.copy(letterBank = updatedBank, enteredTileIds = enteredIds)
        }

        val answer = enteredIds.map { enteredId ->
            updatedBank.first { it.id == enteredId }.letter
        }.joinToString("")
        if (answer != state.currentWord.text) {
            return state.copy(
                letterBank = updatedBank,
                enteredTileIds = enteredIds,
                roundState = SpellingRoundState.Incorrect,
            )
        }

        val completedCount = state.completedWordCount + 1
        return state.copy(
            letterBank = updatedBank,
            enteredTileIds = enteredIds,
            completedWordCount = completedCount,
            roundState = if (completedCount == SESSION_WORD_COUNT) {
                SpellingRoundState.SessionComplete
            } else {
                SpellingRoundState.Correct
            },
        )
    }

    fun backspace(state: SpeakAndSpellState): SpeakAndSpellState {
        if (state.inputLocked || state.enteredTileIds.isEmpty()) return state
        val restoredId = state.enteredTileIds.last()
        return state.copy(
            letterBank = state.letterBank.map { tile ->
                if (tile.id == restoredId) tile.copy(isAvailable = true) else tile
            },
            enteredTileIds = state.enteredTileIds.dropLast(1),
        )
    }

    fun resetIncorrectAnswer(state: SpeakAndSpellState): SpeakAndSpellState {
        if (state.roundState != SpellingRoundState.Incorrect) return state
        return state.copy(
            letterBank = state.letterBank.map { it.copy(isAvailable = true) },
            enteredTileIds = emptyList(),
            roundState = SpellingRoundState.Playing,
        )
    }

    fun requestHelp(state: SpeakAndSpellState): SpeakAndSpellState {
        if (state.inputLocked) return state
        return state.copy(helpCount = state.helpCount + 1)
    }

    fun nextWord(
        state: SpeakAndSpellState,
        random: Random = Random.Default,
    ): SpeakAndSpellState {
        if (state.roundState != SpellingRoundState.Correct) return state
        return createRound(
            level = state.level,
            previousWord = state.currentWord,
            completedWordCount = state.completedWordCount,
            random = random,
        )
    }

    fun newSession(
        state: SpeakAndSpellState,
        random: Random = Random.Default,
    ): SpeakAndSpellState = createRound(
        level = state.level,
        previousWord = state.currentWord,
        completedWordCount = 0,
        random = random,
    )

    private fun createRound(
        level: SpellingLevel,
        previousWord: SpellingWord?,
        completedWordCount: Int,
        random: Random,
    ): SpeakAndSpellState {
        val catalog = SpellingWordCatalog.words(level)
        val choices = catalog.filter { it.text != previousWord?.text }.ifEmpty { catalog }
        val word = choices[random.nextInt(choices.size)]
        val distractorCount = if (word.text.length <= 4) 2 else 3
        val distractors = ('A'..'Z')
            .filterNot { it in word.text }
            .shuffled(random)
            .take(distractorCount)
        val bank = (word.text.toList() + distractors)
            .shuffled(random)
            .mapIndexed { id, letter -> LetterTile(id = id, letter = letter) }
        return SpeakAndSpellState(
            level = level,
            currentWord = word,
            previousWord = previousWord,
            letterBank = bank,
            completedWordCount = completedWordCount,
        )
    }
}

object SpeakAndSpellSpeech {
    fun roundPrompt(word: String): String = "Spell ${word.lowercase()}."

    fun correctAnswer(word: String): String {
        val spokenLetters = word.toList().joinToString(", ")
        return "You got it! $spokenLetters spells ${word.lowercase()}!"
    }

    fun firstLetterHelp(word: String): String {
        val spokenWord = word.lowercase().replaceFirstChar(Char::uppercase)
        return "$spokenWord starts with ${word.first()}."
    }
}
