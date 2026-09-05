package com.nahtygal.olivialooi.games.drums

/** Pure identities; Android resource IDs and drawing objects belong to the adapters. */
enum class DrumSound(val stableId: String, val displayName: String, val audioIdentity: String, val accessibilityDescription: String) {
    KICK("kick", "Big Drum", "drum_kick", "Big Drum, kick drum pad"),
    SNARE("snare", "Snare", "drum_snare", "Snare drum pad"),
    TOM("tom", "Tom", "drum_tom", "Tom drum pad"),
    CLAP("clap", "Clap", "drum_clap", "Hand clap pad"),
    TAMBOURINE("tambourine", "Tambourine", "drum_tambourine", "Tambourine pad"),
    CYMBAL("cymbal", "Cymbal", "drum_cymbal", "Cymbal pad"),
}

object DrumCatalog {
    val sounds: List<DrumSound> = DrumSound.entries
    fun findById(id: String): DrumSound? = sounds.find { it.stableId == id }
}

/** Grid coordinates are supplied by Compose; this model has no platform dependencies.
 * Gutters are not pads. Crossing one releases the previous pad before entering the next.
 */
fun drumPadAt(x: Float, y: Float, width: Float, height: Float, columns: Int, gap: Float): Int {
    if ((columns != 2 && columns != 3) || !width.isFinite() || !height.isFinite() ||
        !x.isFinite() || !y.isFinite() || !gap.isFinite() || gap < 0 ||
        x < 0 || y < 0 || x >= width || y >= height) return -1
    val rows = DrumSound.entries.size / columns
    val padWidth = (width - gap * (columns - 1)) / columns
    val padHeight = (height - gap * (rows - 1)) / rows
    if (padWidth <= 0 || padHeight <= 0) return -1
    val column = (x / (padWidth + gap)).toInt()
    val row = (y / (padHeight + gap)).toInt()
    if (column >= columns || row >= rows || x - column * (padWidth + gap) >= padWidth ||
        y - row * (padHeight + gap) >= padHeight) return -1
    return row * columns + column
}
