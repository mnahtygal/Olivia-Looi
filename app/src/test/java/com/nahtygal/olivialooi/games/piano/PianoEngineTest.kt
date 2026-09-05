package com.nahtygal.olivialooi.games.piano

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PianoEngineTest(private val song: PianoSong) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun songs(): List<Array<PianoSong>> = PianoCatalog.songs.map { arrayOf(it) }
    }
    private fun selected() = PianoEngine.selectSong(PianoState(), song.stableId)
    private fun started() = PianoEngine.startSong(selected())
    private fun complete() = song.notes.fold(started(), PianoEngine::press)

    @Test fun `selection waits for Start Song`() {
        val state = selected()
        assertEquals(0, state.position)
        assertFalse(state.songStarted)
        assertNull(state.expectedNote)
        assertSame(state, PianoEngine.press(state, song.notes.first()))
    }
    @Test fun `song starts at zero with expected first note`() {
        val state = started()
        assertEquals(0, state.position)
        assertEquals(song.notes.first(), state.expectedNote)
        assertFalse(state.isComplete)
    }
    @Test fun `correct note advances exactly one`() {
        val state = PianoEngine.press(started(), song.notes.first())
        assertEquals(1, state.position)
        assertEquals(song.notes[1], state.expectedNote)
    }
    @Test fun `wrong note does not advance or complete`() {
        val state = started()
        PianoCatalog.notes.filter { it != state.expectedNote }.forEach {
            assertSame(state, PianoEngine.press(state, it))
            assertFalse(PianoEngine.press(state, it).isComplete)
        }
    }
    @Test fun `final correct note completes and removes expected note`() {
        val before = song.notes.dropLast(1).fold(started(), PianoEngine::press)
        assertFalse(before.isComplete)
        assertEquals(song.notes.last(), before.expectedNote)
        val final = PianoEngine.press(before, song.notes.last())
        assertTrue(final.isComplete)
        assertEquals(song.notes.size, final.position)
        assertNull(final.expectedNote)
    }
    @Test fun `cannot advance beyond completion`() {
        val done = complete()
        repeat(100) { assertSame(done, PianoEngine.press(done, PianoCatalog.notes[it % 8])) }
    }
    @Test fun `restart resets position completion and spoken flag`() {
        val reset = PianoEngine.restartSong(PianoEngine.acknowledgeCompletion(complete()))
        assertEquals(0, reset.position)
        assertFalse(reset.isComplete)
        assertFalse(reset.completionSpoken)
        assertEquals(song.stableId, reset.selectedSongId)
        assertEquals(song.notes.first(), reset.expectedNote)
    }
    @Test fun `start twice does not discard progress`() {
        val state = PianoEngine.press(started(), song.notes.first())
        assertSame(state, PianoEngine.startSong(state))
    }
    @Test fun `exit resets song without changing notes preference`() {
        val state = PianoEngine.freePlay(started().copy(sayNotes = true, openingSpoken = true))
        assertEquals(PianoMode.FREE_PLAY, state.mode)
        assertNull(state.song)
        assertNull(state.expectedNote)
        assertEquals(0, state.position)
        assertTrue(state.sayNotes)
        assertTrue(state.openingSpoken)
    }
    @Test fun `song menu clears active song`() {
        val state = PianoEngine.chooseSongs(complete())
        assertEquals(PianoMode.SONGS, state.mode)
        assertNull(state.song)
        assertFalse(state.isComplete)
    }
    @Test fun `invalid selection leaves state intact`() {
        val state = started()
        assertSame(state, PianoEngine.selectSong(state, "unknown"))
    }
    @Test fun `completion announcement acknowledged once`() {
        val done = complete()
        assertFalse(done.completionSpoken)
        val acknowledged = PianoEngine.acknowledgeCompletion(done)
        assertTrue(acknowledged.completionSpoken)
        assertSame(acknowledged, PianoEngine.acknowledgeCompletion(acknowledged))
        val active = started()
        assertSame(active, PianoEngine.acknowledgeCompletion(active))
    }
    @Test fun `save restoration covers every song position`() {
        var state = started().copy(openingSpoken = true, sayNotes = true)
        for (note in song.notes) {
            assertEquals(state, PianoStateCodec.decode(PianoStateCodec.encode(state)))
            state = PianoEngine.press(state, note)
        }
        state = PianoEngine.acknowledgeCompletion(state)
        assertEquals(state, PianoStateCodec.decode(PianoStateCodec.encode(state)))
    }
    @Test fun `random wrong notes preserve ordered melody progress`() {
        var state = started()
        val random = Random(1212)
        song.notes.forEachIndexed { index, expected ->
            repeat(15) {
                val wrong = PianoCatalog.notes.filter { it != expected }.random(random)
                state = PianoEngine.press(state, wrong)
                assertEquals(index, state.position)
            }
            state = PianoEngine.press(state, expected)
            assertEquals(index + 1, state.position)
            assertEquals(index == song.notes.lastIndex, state.isComplete)
        }
    }
    @Test fun `consecutive repeated pitches each need new input`() {
        var state = started()
        val inputs = mutableListOf<PianoNote>()
        val pointers = PianoPointers()
        val onPress: (PianoNote) -> Unit = { inputs += it; state = PianoEngine.press(state, it) }
        val first = song.notes.first().ordinal
        pointers.update(7, first, true, onPress)
        repeat(100) { pointers.update(7, first, true, onPress) }
        assertEquals(1, inputs.size)
        assertEquals(1, state.position)
        pointers.update(7, first, false, onPress)
        pointers.update(7, song.notes[1].ordinal, true, onPress)
        assertEquals(2, state.position)
    }
    @Test fun `saved invalid positions and flags rejected`() {
        val fields = PianoStateCodec.encode(started()).split('|')
        fun changed(index: Int, value: String) = fields.toMutableList().also { it[index] = value }.joinToString("|")
        listOf("", "broken", changed(0, "2"), changed(1, "UNKNOWN"), changed(2, "unknown"),
            changed(4, "-1"), changed(4, (song.notes.size + 1).toString()), changed(5, "maybe"), changed(7, "true"),
        ).forEach { assertNull(it, PianoStateCodec.decode(it)) }
    }
}
