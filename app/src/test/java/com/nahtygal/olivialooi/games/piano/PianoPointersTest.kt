package com.nahtygal.olivialooi.games.piano

import org.junit.Assert.*
import org.junit.Test

class PianoPointersTest {
    private val pointers = PianoPointers()
    private val played = mutableListOf<PianoNote>()
    private val play: (PianoNote) -> Unit = { played += it }
    private fun down(id: Long, key: Int) = pointers.update(id, key, true, play)
    private fun up(id: Long) = pointers.update(id, -1, false, play)

    @Test fun `note fires on press and not on release`() {
        down(1, 0)
        assertEquals(listOf(PianoNote.C4), played)
        assertEquals(1, pointers.pressedMask())
        up(1)
        assertEquals(1, played.size)
        assertEquals(0, pointers.pressedMask())
    }
    @Test fun `stationary pointer never repeats playback`() {
        repeat(100) { down(1, 3) }
        assertEquals(listOf(PianoNote.F4), played)
    }
    @Test fun `rapid repeated presses all play`() {
        repeat(100) { down(1, 0); up(1) }
        assertEquals(100, played.size)
    }
    @Test fun `two finger chord retains both keys`() {
        down(1, 0); down(2, 2)
        assertEquals(listOf(PianoNote.C4, PianoNote.E4), played)
        assertEquals(5, pointers.pressedMask())
        up(2)
        assertEquals(1, pointers.pressedMask())
        down(3, 4)
        assertEquals(17, pointers.pressedMask())
    }
    @Test fun `two fingers on same key release independently`() {
        down(1, 0); down(2, 0)
        assertEquals(2, played.size)
        up(1)
        assertEquals(1, pointers.pressedMask())
        down(2, 0)
        assertEquals(2, played.size)
        up(2)
        assertEquals(0, pointers.pressedMask())
    }
    @Test fun `slides play only newly entered keys`() {
        down(1, 0); down(1, 1); down(1, 1); down(1, 2)
        assertEquals(listOf(PianoNote.C4, PianoNote.D4, PianoNote.E4), played)
        assertEquals(4, pointers.pressedMask())
    }
    @Test fun `slide outside releases visual and reenter plays again`() {
        down(1, 0); down(1, -1)
        assertEquals(0, pointers.pressedMask())
        down(1, 0)
        assertEquals(2, played.size)
    }
    @Test fun `independent sliding fingers do not interrupt one another`() {
        down(1, 1); down(2, 5); down(1, 2)
        assertEquals((1 shl 2) or (1 shl 5), pointers.pressedMask())
        assertEquals(listOf(PianoNote.D4, PianoNote.A4, PianoNote.E4), played)
    }
    @Test fun `all eight keys can be held together`() {
        repeat(8) { down(it.toLong(), it) }
        assertEquals(255, pointers.pressedMask())
        assertEquals(PianoCatalog.notes, played)
        repeat(8) { up(it.toLong()) }
        assertEquals(0, pointers.pressedMask())
    }
    @Test fun `cancel clears held keys and permits fresh gestures`() {
        down(1, 0); down(2, 3)
        pointers.clear()
        assertEquals(0, pointers.pressedMask())
        down(1, 0)
        assertEquals(3, played.size)
    }
    @Test fun `opaque signed pointer IDs supported`() {
        down(Long.MIN_VALUE, 0); down(0, 1); down(Long.MAX_VALUE, 2)
        assertEquals(7, pointers.pressedMask())
        up(0)
        assertEquals(5, pointers.pressedMask())
    }
    @Test fun `unknown release harmless`() {
        down(1, 0); up(200)
        assertEquals(1, pointers.pressedMask())
    }
    @Test fun `bounded capacity ignores excess without losing existing keys`() {
        val small = PianoPointers(2)
        small.update(1, 0, true, play); small.update(2, 1, true, play); small.update(3, 2, true, play)
        assertEquals(3, small.pressedMask())
        assertEquals(2, played.size)
    }
    @Test fun `out of range key never plays`() {
        down(1, -1); down(2, 8); down(3, Int.MAX_VALUE)
        assertTrue(played.isEmpty())
        assertEquals(0, pointers.pressedMask())
    }
    @Test fun `hit testing maps centers and rejects outside`() {
        repeat(8) { assertEquals(it, pianoKeyAt(it * 100f + 50f, 50f, 800f, 300f)) }
        assertEquals(-1, pianoKeyAt(-1f, 50f, 800f, 300f))
        assertEquals(-1, pianoKeyAt(800f, 50f, 800f, 300f))
        assertEquals(-1, pianoKeyAt(50f, 300f, 800f, 300f))
        assertEquals(-1, pianoKeyAt(50f, 50f, 0f, 0f))
        assertEquals(-1, pianoKeyAt(Float.NaN, 50f, 800f, 300f))
    }
    @Test fun `fractional phone widths keep eight reachable keys`() {
        repeat(8) { assertEquals(it, pianoKeyAt((it + .5f) * 317f / 8, 100f, 317f, 250f)) }
    }
}
