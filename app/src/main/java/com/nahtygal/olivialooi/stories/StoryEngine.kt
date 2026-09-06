package com.nahtygal.olivialooi.stories

enum class ReadingMode { TO_ME, MYSELF }
data class StoryPlaybackState(
    val storyId: String,
    val mode: ReadingMode? = null,
    val page: Int = 0,
    val generation: Long = 0,
    val narrating: Boolean = false,
    val complete: Boolean = false,
    val speechFailed: Boolean = false,
)

object StoryEngine {
    fun open(id: String, previous: StoryPlaybackState? = null): StoryPlaybackState {
        require(StoryCatalog.find(id) != null)
        return StoryPlaybackState(id, generation = (previous?.generation ?: -1) + 1)
    }
    fun chooseMode(s: StoryPlaybackState, mode: ReadingMode) = s.copy(mode = mode, generation = s.generation + 1,
        narrating = mode == ReadingMode.TO_ME && !s.complete, speechFailed = false)
    fun stop(s: StoryPlaybackState) = s.copy(generation = s.generation + 1, narrating = false, speechFailed = false)
    fun say(s: StoryPlaybackState): StoryPlaybackState = if (s.complete || s.mode == null) s else
        s.copy(generation = s.generation + 1, narrating = true, speechFailed = false)
    fun next(s: StoryPlaybackState): StoryPlaybackState {
        if (s.complete || s.mode == null) return s
        val last = StoryCatalog.find(s.storyId)!!.pages.lastIndex
        return s.copy(page = (s.page + 1).coerceAtMost(last), complete = s.page == last,
            generation = s.generation + 1, narrating = s.mode == ReadingMode.TO_ME && s.page < last, speechFailed = false)
    }
    fun previous(s: StoryPlaybackState): StoryPlaybackState = if (s.page == 0 || s.mode == null || s.complete) s else
        s.copy(page = s.page - 1, generation = s.generation + 1, narrating = s.mode == ReadingMode.TO_ME, speechFailed = false)
    fun finished(s: StoryPlaybackState, identity: Long, success: Boolean): StoryPlaybackState {
        if (!s.narrating || s.generation != identity || s.complete) return s
        if (!success) return stop(s).copy(speechFailed = true)
        return if (s.mode == ReadingMode.TO_ME) next(s) else stop(s)
    }
    fun again(s: StoryPlaybackState) = chooseMode(open(s.storyId, s), s.mode ?: ReadingMode.MYSELF)
    fun opening(s: StoryPlaybackState) = stop(s).copy(mode = null)

    fun encode(s: StoryPlaybackState) = listOf("1", s.storyId, s.mode?.name.orEmpty(), s.page.toString(), s.complete.toString(), s.generation.toString()).joinToString("|")
    /** Restore visual state only. Resume explicitly restarts the current page. */
    fun decode(value: String): StoryPlaybackState? = runCatching {
        val f = value.split('|'); require(f.size == 6 && f[0] == "1")
        val story = StoryCatalog.find(f[1]) ?: error("Unknown story")
        val mode = f[2].takeIf { it.isNotEmpty() }?.let(ReadingMode::valueOf)
        val page = f[3].toInt(); val complete = f[4].toBooleanStrict(); val generation = f[5].toLong()
        require(page in story.pages.indices && generation in 0 until Long.MAX_VALUE)
        require(!complete || page == story.pages.lastIndex)
        StoryPlaybackState(story.id, mode, page, generation + 1, complete = complete)
    }.getOrNull()
}
