package com.nahtygal.olivialooi.games.drums

import org.junit.Assert.*
import org.junit.Test

class DrumPointersTest {
    private val pointers = DrumPointers()
    private val played = mutableListOf<DrumSound>()
    private val play: (DrumSound) -> Unit = { played += it }
    private fun down(id: Long, key: Int) = pointers.update(id, key, true, play)
    private fun up(id: Long) = pointers.update(id, -1, false, play)

    @Test fun `note fires on press and not on release`() {
        down(1, 0)
        assertEquals(listOf(DrumSound.KICK), played)
        assertEquals(1, pointers.pressedMask())
        up(1)
        assertEquals(1, played.size)
        assertEquals(0, pointers.pressedMask())
    }
    @Test fun `stationary pointer never repeats playback`() {
        repeat(100) { down(1, 3) }
        assertEquals(listOf(DrumSound.CLAP), played)
    }
    @Test fun `rapid repeated presses all play`() {
        repeat(100) { down(1, 0); up(1) }
        assertEquals(100, played.size)
    }
    @Test fun `two finger chord retains both keys`() {
        down(1, 0); down(2, 2)
        assertEquals(listOf(DrumSound.KICK, DrumSound.TOM), played)
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
        assertEquals(listOf(DrumSound.KICK, DrumSound.SNARE, DrumSound.TOM), played)
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
        assertEquals(listOf(DrumSound.SNARE, DrumSound.CYMBAL, DrumSound.TOM), played)
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
        val small = DrumPointers(2)
        small.update(1, 0, true, play); small.update(2, 1, true, play); small.update(3, 2, true, play)
        assertEquals(3, small.pressedMask())
        assertEquals(2, played.size)
    }
    @Test fun `out of range key never plays`() {
        down(1, -1); down(2, 8); down(3, Int.MAX_VALUE)
        assertTrue(played.isEmpty())
        assertEquals(0, pointers.pressedMask())
    }
    @Test fun `all six pads can be held`() {
        repeat(6) { down(it.toLong(), it) }
        assertEquals(63, pointers.pressedMask())
        repeat(6) { up(it.toLong()) }
        assertEquals(0, pointers.pressedMask())
    }
    @Test fun `tablet and phone geometry includes gutters`() {
        for (columns in listOf(2,3)) {
            val rows = 6 / columns
            val width = columns * 100f + (columns - 1) * 12
            val height = rows * 100f + (rows - 1) * 12
            repeat(6) { index -> assertEquals(index, drumPadAt(index % columns * 112f + 50, index / columns * 112f + 50, width, height, columns, 12f)) }
            assertEquals(-1, drumPadAt(105f,50f,width,height,columns,12f))
            assertEquals(-1, drumPadAt(50f,105f,width,height,columns,12f))
            assertEquals(-1, drumPadAt(-1f,50f,width,height,columns,12f))
        }
    }
}
