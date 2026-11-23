package com.nahtygal.olivialooi.games.tracing

data class WordProgress(
    val strokesByLetter: List<List<TracingStroke>>,
    val completionCelebrated: Boolean = false,
    val celebrationPending: Boolean = false,
)

data class WordTracingState(
    val profileId: String,
    val categoryId: String,
    val currentWordId: String,
    val letterPosition: Int = 0,
    val progressByWordId: Map<String, WordProgress>,
) {
    val words: List<TraceWord> get() = WordCatalog.words(categoryId)
    val wordPosition: Int get() = words.indexOfFirst { it.stableId == currentWordId }
    val currentWord: TraceWord get() = checkNotNull(WordCatalog.word(currentWordId))
    val currentProgress: WordProgress get() = checkNotNull(progressByWordId[currentWordId])
    val currentLetter: Char get() = currentWord.value[letterPosition]
    val currentStrokes: List<TracingStroke> get() = currentProgress.strokesByLetter[letterPosition]
    val isCurrentWordComplete: Boolean get() = currentProgress.isComplete(currentWord)
}

object WordTracingEngine {
    fun newSession(profileId: String, categoryId: String): WordTracingState {
        require(profileId.isNotBlank())
        val words = WordCatalog.words(categoryId)
        require(words.isNotEmpty())
        return WordTracingState(
            profileId = profileId,
            categoryId = categoryId,
            currentWordId = words.first().stableId,
            progressByWordId = words.associate { word ->
                word.stableId to WordProgress(List(word.value.length) { emptyList() })
            },
        )
    }

    fun addStroke(state: WordTracingState, points: List<TracingPoint>): WordTracingState {
        if (!state.isStructurallyValid() || points.isEmpty() || points.any { !it.isValid }) return state
        val progress = state.currentProgress
        val strokes = progress.strokesByLetter.toMutableList()
        strokes[state.letterPosition] = strokes[state.letterPosition] + TracingStroke(points.toList())
        val updatedProgress = progress.copy(strokesByLetter = strokes)
        val newlyComplete =
            updatedProgress.isComplete(state.currentWord) &&
                !progress.isComplete(state.currentWord) &&
                !progress.completionCelebrated
        return state.withCurrentProgress(
            updatedProgress.copy(celebrationPending = progress.celebrationPending || newlyComplete),
        )
    }

    fun undo(state: WordTracingState): WordTracingState {
        if (!state.isStructurallyValid() || state.currentStrokes.isEmpty()) return state
        val progress = state.currentProgress
        val strokes = progress.strokesByLetter.toMutableList()
        strokes[state.letterPosition] = state.currentStrokes.dropLast(1)
        return state.withCurrentProgress(progress.copy(strokesByLetter = strokes, celebrationPending = false))
    }

    fun clear(state: WordTracingState): WordTracingState {
        if (!state.isStructurallyValid() || state.currentStrokes.isEmpty()) return state
        val progress = state.currentProgress
        val strokes = progress.strokesByLetter.toMutableList()
        strokes[state.letterPosition] = emptyList()
        return state.withCurrentProgress(progress.copy(strokesByLetter = strokes, celebrationPending = false))
    }

    fun previousLetter(state: WordTracingState): WordTracingState =
        if (state.letterPosition > 0) state.copy(letterPosition = state.letterPosition - 1) else state

    fun nextLetter(state: WordTracingState): WordTracingState =
        if (state.letterPosition < state.currentWord.value.lastIndex) {
            state.copy(letterPosition = state.letterPosition + 1)
        } else {
            state
        }

    fun previousWord(state: WordTracingState): WordTracingState = moveWord(state, state.wordPosition - 1)

    fun nextWord(state: WordTracingState): WordTracingState = moveWord(state, state.wordPosition + 1)

    fun selectWord(state: WordTracingState, wordId: String): WordTracingState {
        if (state.words.none { it.stableId == wordId }) return state
        return state.copy(currentWordId = wordId, letterPosition = 0)
    }

    fun consumeCelebration(state: WordTracingState): WordTracingState {
        val progress = state.currentProgress
        return if (progress.celebrationPending) {
            state.withCurrentProgress(progress.copy(completionCelebrated = true, celebrationPending = false))
        } else {
            state
        }
    }

    fun practiceWordAgain(state: WordTracingState): WordTracingState = state.withCurrentProgress(
        WordProgress(List(state.currentWord.value.length) { emptyList() }),
    ).copy(letterPosition = 0)

    private fun moveWord(state: WordTracingState, position: Int): WordTracingState =
        if (position in state.words.indices) {
            state.copy(currentWordId = state.words[position].stableId, letterPosition = 0)
        } else {
            state
        }

    private fun WordTracingState.withCurrentProgress(progress: WordProgress): WordTracingState =
        copy(progressByWordId = progressByWordId + (currentWordId to progress))
}

internal fun WordTracingState.isStructurallyValid(): Boolean {
    val category = WordCatalog.category(categoryId) ?: return false
    val categoryWords = WordCatalog.words(categoryId)
    if (
        profileId.isBlank() || currentWordId !in categoryWords.map(TraceWord::stableId) ||
        progressByWordId.keys != categoryWords.map(TraceWord::stableId).toSet() ||
        letterPosition !in currentWord.value.indices
    ) return false
    return categoryWords.all { word ->
        val progress = progressByWordId[word.stableId] ?: return@all false
        word.value.length == category.wordLength &&
            progress.strokesByLetter.size == word.value.length &&
            (!progress.celebrationPending || (progress.isComplete(word) && !progress.completionCelebrated)) &&
            progress.strokesByLetter.flatten().all { stroke ->
                stroke.points.isNotEmpty() && stroke.points.all(TracingPoint::isValid)
            }
    }
}

internal fun WordProgress.isComplete(word: TraceWord): Boolean =
    strokesByLetter.size == word.value.length &&
        word.value.indices.all { position ->
            LetterCompletionPolicy.isComplete(word.value[position], strokesByLetter[position])
        }
