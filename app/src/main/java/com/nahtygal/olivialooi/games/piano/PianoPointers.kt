package com.nahtygal.olivialooi.games.piano

/** Reusable pointer slots: no maps/lists allocated in pointer-move loops.
 * Two fingers on the same key are independent presses; releasing one leaves the other down.
 * Pointer IDs are opaque, and may include zero or any signed Long.
 */
class PianoPointers(private val capacity: Int = 16) {
    private val ids = LongArray(capacity)
    private val occupied = BooleanArray(capacity)
    private val keys = IntArray(capacity) { -1 }
    private val keyCounts = IntArray(PianoNote.entries.size)

    init { require(capacity > 0) }

    fun update(pointerId: Long, keyIndex: Int, pressed: Boolean, onPress: (PianoNote) -> Unit) {
        var slot = -1
        for (i in ids.indices) if (occupied[i] && ids[i] == pointerId) { slot = i; break }
        if (!pressed) {
            if (slot >= 0) { releaseKey(slot); occupied[slot] = false }
            return
        }
        val key = if (keyIndex in keyCounts.indices) keyIndex else -1
        if (slot < 0) {
            for (i in occupied.indices) if (!occupied[i]) { slot = i; break }
            if (slot < 0) return
            occupied[slot] = true
            ids[slot] = pointerId
        }
        if (keys[slot] == key) return
        releaseKey(slot)
        keys[slot] = key
        if (key >= 0) {
            keyCounts[key] += 1
            onPress(PianoNote.entries[key])
        }
    }

    fun pressedMask(): Int {
        var mask = 0
        for (i in keyCounts.indices) if (keyCounts[i] > 0) mask = mask or (1 shl i)
        return mask
    }
    fun clear() { occupied.fill(false); keys.fill(-1); keyCounts.fill(0) }
    private fun releaseKey(slot: Int) {
        if (keys[slot] >= 0) keyCounts[keys[slot]] -= 1
        keys[slot] = -1
    }
}

/** UI passes normalized key-strip coordinates; no platform classes are needed for hit testing. */
fun pianoKeyAt(x: Float, y: Float, width: Float, height: Float): Int =
    if (width <= 0 || height <= 0 || x < 0 || y < 0 || x >= width || y >= height || !x.isFinite() || !y.isFinite()) -1
    else (x / width * PianoNote.entries.size).toInt().coerceIn(0, PianoNote.entries.lastIndex)
