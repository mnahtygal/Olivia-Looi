package com.nahtygal.olivialooi.games.drums

/** Reusable pointer slots: no maps/lists allocated in pointer-move loops.
 * Two fingers on the same pad are independent presses; releasing one leaves the other down.
 * Pointer IDs are opaque, and may include zero or any signed Long.
 */
class DrumPointers(private val capacity: Int = 16) {
    private val ids = LongArray(capacity)
    private val occupied = BooleanArray(capacity)
    private val pads = IntArray(capacity) { -1 }
    private val padCounts = IntArray(DrumSound.entries.size)

    init { require(capacity > 0) }

    fun update(pointerId: Long, padIndex: Int, pressed: Boolean, onPress: (DrumSound) -> Unit) {
        var slot = -1
        for (i in ids.indices) if (occupied[i] && ids[i] == pointerId) { slot = i; break }
        if (!pressed) {
            if (slot >= 0) { releasePad(slot); occupied[slot] = false }
            return
        }
        val pad = if (padIndex in padCounts.indices) padIndex else -1
        if (slot < 0) {
            for (i in occupied.indices) if (!occupied[i]) { slot = i; break }
            if (slot < 0) return
            occupied[slot] = true
            ids[slot] = pointerId
        }
        if (pads[slot] == pad) return
        releasePad(slot)
        pads[slot] = pad
        if (pad >= 0) {
            padCounts[pad] += 1
            onPress(DrumSound.entries[pad])
        }
    }

    fun pressedMask(): Int {
        var mask = 0
        for (i in padCounts.indices) if (padCounts[i] > 0) mask = mask or (1 shl i)
        return mask
    }
    fun clear() { occupied.fill(false); pads.fill(-1); padCounts.fill(0) }
    private fun releasePad(slot: Int) {
        if (pads[slot] >= 0) padCounts[pads[slot]] -= 1
        pads[slot] = -1
    }
}

