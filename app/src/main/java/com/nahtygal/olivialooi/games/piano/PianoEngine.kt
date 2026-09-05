package com.nahtygal.olivialooi.games.piano

enum class PianoMode { FREE_PLAY, SONGS }

data class PianoState(
    val mode: PianoMode = PianoMode.FREE_PLAY,
    val selectedSongId: String? = null,
    val songStarted: Boolean = false,
    val position: Int = 0,
    val sayNotes: Boolean = false,
    val openingSpoken: Boolean = false,
    val completionSpoken: Boolean = false,
) {
    val song: PianoSong? get() = selectedSongId?.let(PianoCatalog::findSong)
    val isComplete: Boolean get() = songStarted && song?.notes?.size == position
    val expectedNote: PianoNote? get() = if (songStarted) song?.notes?.getOrNull(position) else null
}

object PianoEngine {
    fun chooseSongs(state: PianoState): PianoState = state.copy(
        mode = PianoMode.SONGS, selectedSongId = null, songStarted = false, position = 0, completionSpoken = false,
    )
    fun freePlay(state: PianoState): PianoState = state.copy(
        mode = PianoMode.FREE_PLAY, selectedSongId = null, songStarted = false, position = 0, completionSpoken = false,
    )
    fun selectSong(state: PianoState, songId: String): PianoState {
        if (PianoCatalog.findSong(songId) == null) return state
        return state.copy(mode = PianoMode.SONGS, selectedSongId = songId, songStarted = false, position = 0, completionSpoken = false)
    }
    fun startSong(state: PianoState): PianoState =
        if (state.mode != PianoMode.SONGS || state.song == null || state.songStarted) state
        else state.copy(songStarted = true, position = 0, completionSpoken = false)

    /** Called once per new physical press/key crossing. Wrong notes remain normal musical input. */
    fun press(state: PianoState, note: PianoNote): PianoState =
        if (state.mode != PianoMode.SONGS || note != state.expectedNote || state.isComplete) state
        else state.copy(position = state.position + 1)

    fun restartSong(state: PianoState): PianoState =
        if (state.mode != PianoMode.SONGS || state.song == null) state
        else state.copy(songStarted = true, position = 0, completionSpoken = false)

    fun acknowledgeCompletion(state: PianoState): PianoState =
        if (state.isComplete && !state.completionSpoken) state.copy(completionSpoken = true) else state
}

/** Primitive saveable payload; musical voices and active fingers are deliberately transient. */
object PianoStateCodec {
    fun encode(state: PianoState): String = listOf(
        "1", state.mode.name, state.selectedSongId.orEmpty(), state.songStarted, state.position,
        state.sayNotes, state.openingSpoken, state.completionSpoken,
    ).joinToString("|")

    fun decode(payload: String): PianoState? = runCatching {
        val fields = payload.split('|')
        require(fields.size == 8 && fields[0] == "1")
        val state = PianoState(
            mode = PianoMode.valueOf(fields[1]), selectedSongId = fields[2].takeIf { it.isNotEmpty() },
            songStarted = fields[3].toBooleanStrict(), position = fields[4].toInt(),
            sayNotes = fields[5].toBooleanStrict(), openingSpoken = fields[6].toBooleanStrict(),
            completionSpoken = fields[7].toBooleanStrict(),
        )
        require(state.selectedSongId == null || state.song != null)
        require(state.position in 0..(state.song?.notes?.size ?: 0))
        require(!state.songStarted || (state.mode == PianoMode.SONGS && state.song != null))
        require(state.songStarted || state.position == 0)
        require(state.mode != PianoMode.FREE_PLAY || state.selectedSongId == null)
        require(!state.completionSpoken || state.isComplete)
        state
    }.getOrNull()
}
