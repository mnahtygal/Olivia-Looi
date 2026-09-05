package com.nahtygal.olivialooi.games.piano

/** Resource names are plain identities; Android resource integers stay in the audio adapter. */
enum class PianoNote(
    val stableId: String,
    val displayLabel: String,
    val audioIdentity: String,
    val accessibilityDescription: String,
    val frequencyHz: Double,
) {
    C4("c4", "C", "piano_c4", "C piano key, middle C", 261.625565),
    D4("d4", "D", "piano_d4", "D piano key", 293.664768),
    E4("e4", "E", "piano_e4", "E piano key", 329.627557),
    F4("f4", "F", "piano_f4", "F piano key", 349.228231),
    G4("g4", "G", "piano_g4", "G piano key", 391.995436),
    A4("a4", "A", "piano_a4", "A piano key", 440.0),
    B4("b4", "B", "piano_b4", "B piano key", 493.883301),
    C5("c5", "C", "piano_c5", "C piano key, high C", 523.251131),
}

data class PianoSong(val stableId: String, val title: String, val notes: List<PianoNote>) {
    init { require(stableId.isNotBlank() && title.isNotBlank() && notes.isNotEmpty()) }
}

object PianoCatalog {
    val notes: List<PianoNote> = PianoNote.entries
    fun findNote(id: String): PianoNote? = notes.find { it.stableId == id }

    // Traditional melody pitches only, transposed to C major within C4–C5.
    // No recording, accompaniment, or modern arrangement is incorporated.
    val songs: List<PianoSong> = listOf(
        PianoSong("twinkle", "Twinkle Twinkle Little Star", sequence(
            "C4 C4 G4 G4 A4 A4 G4 F4 F4 E4 E4 D4 D4 C4 " +
                "G4 G4 F4 F4 E4 E4 D4 G4 G4 F4 F4 E4 E4 D4 " +
                "C4 C4 G4 G4 A4 A4 G4 F4 F4 E4 E4 D4 D4 C4",
        )),
        PianoSong("mary", "Mary Had a Little Lamb", sequence(
            "E4 D4 C4 D4 E4 E4 E4 D4 D4 D4 E4 G4 G4 " +
                "E4 D4 C4 D4 E4 E4 E4 E4 D4 D4 E4 D4 C4",
        )),
        PianoSong("row", "Row, Row, Row Your Boat", sequence(
            "C4 C4 C4 D4 E4 E4 D4 E4 F4 G4 " +
                "C5 C5 C5 G4 G4 G4 E4 E4 E4 C4 C4 C4 G4 F4 E4 D4 C4",
        )),
    )
    fun findSong(id: String): PianoSong? = songs.find { it.stableId == id }
    private fun sequence(pitches: String): List<PianoNote> = pitches.split(' ').map(PianoNote::valueOf)
}
