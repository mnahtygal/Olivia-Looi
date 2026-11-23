package com.nahtygal.olivialooi.games.tracing

object WordTracingStateCodec {
    fun encode(state: WordTracingState): String {
        require(state.isStructurallyValid())
        val progress = state.words.joinToString("!") { word ->
            val wordProgress = checkNotNull(state.progressByWordId[word.stableId])
            val handled = wordProgress.completionCelebrated || wordProgress.celebrationPending
            val letters = wordProgress.strokesByLetter.joinToString("~") { strokes ->
                strokes.joinToString(";") { stroke ->
                    stroke.points.joinToString("/") { point -> "${point.x},${point.y}" }
                }
            }
            "${word.stableId}^$handled^$letters"
        }
        return "1|${state.profileId}|${state.categoryId}|${state.currentWordId}|${state.letterPosition}|$progress"
    }

    fun decode(payload: String): WordTracingState? = runCatching {
        val fields = payload.split('|', limit = 6)
        require(fields.size == 6 && fields[0] == "1")
        val profileId = fields[1]
        val categoryId = fields[2]
        val words = WordCatalog.words(categoryId)
        require(words.isNotEmpty())
        val savedWords = fields[5].split('!')
        require(savedWords.size == words.size)
        val progress = savedWords.associate { savedWord ->
            val wordFields = savedWord.split('^', limit = 3)
            require(wordFields.size == 3)
            val word = requireNotNull(WordCatalog.word(wordFields[0]))
            require(word.categoryId == categoryId)
            val positions = wordFields[2].split('~')
            require(positions.size == word.value.length)
            word.stableId to WordProgress(
                strokesByLetter = positions.map(::decodeStrokes),
                completionCelebrated = wordFields[1].toBooleanStrict(),
                celebrationPending = false,
            )
        }
        require(progress.keys == words.map(TraceWord::stableId).toSet())
        WordTracingState(
            profileId = profileId,
            categoryId = categoryId,
            currentWordId = fields[3],
            letterPosition = fields[4].toInt(),
            progressByWordId = progress,
        ).also { require(it.isStructurallyValid()) }
    }.getOrNull()

    private fun decodeStrokes(value: String): List<TracingStroke> =
        if (value.isEmpty()) emptyList() else value.split(';').map { savedStroke ->
            TracingStroke(savedStroke.split('/').map { savedPoint ->
                val coordinates = savedPoint.split(',', limit = 2)
                require(coordinates.size == 2)
                TracingPoint(coordinates[0].toFloat(), coordinates[1].toFloat())
            })
        }
}
