package com.nahtygal.olivialooi.games.piano

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class PianoCatalogTest {
    @Test fun `exactly eight white notes in C4 to C5 order`() {
        assertEquals(listOf("c4", "d4", "e4", "f4", "g4", "a4", "b4", "c5"), PianoCatalog.notes.map { it.stableId })
        assertEquals(listOf("C", "D", "E", "F", "G", "A", "B", "C"), PianoCatalog.notes.map { it.displayLabel })
    }
    @Test fun `note IDs unique`() { assertEquals(8, PianoCatalog.notes.map { it.stableId }.toSet().size) }
    @Test fun `audio identities present and unique`() {
        assertTrue(PianoCatalog.notes.all { it.audioIdentity.isNotBlank() })
        assertEquals(8, PianoCatalog.notes.map { it.audioIdentity }.toSet().size)
    }
    @Test fun `accessibility descriptions present and distinguish two Cs`() {
        assertTrue(PianoCatalog.notes.all { it.accessibilityDescription.isNotBlank() })
        assertNotEquals(PianoNote.C4.accessibilityDescription, PianoNote.C5.accessibilityDescription)
    }
    @Test fun `note lookup succeeds and unknown fails safely`() {
        PianoCatalog.notes.forEach { assertEquals(it, PianoCatalog.findNote(it.stableId)) }
        assertNull(PianoCatalog.findNote("C#4"))
        assertNull(PianoCatalog.findNote(""))
    }
    @Test fun `exactly three requested songs`() {
        assertEquals(listOf("Twinkle Twinkle Little Star", "Mary Had a Little Lamb", "Row, Row, Row Your Boat"), PianoCatalog.songs.map { it.title })
        assertEquals(3, PianoCatalog.songs.map { it.stableId }.toSet().size)
    }
    @Test fun `song names populated and all notes in catalog`() {
        PianoCatalog.songs.forEach { song ->
            assertTrue(song.title.isNotBlank())
            assertTrue(song.notes.isNotEmpty())
            assertTrue(song.notes.all { it in PianoCatalog.notes })
        }
    }
    @Test fun `song lookup succeeds and unknown fails safely`() {
        PianoCatalog.songs.forEach { assertEquals(it, PianoCatalog.findSong(it.stableId)) }
        assertNull(PianoCatalog.findSong("unknown"))
    }
    @Test fun `melody phrase boundaries and lengths retained`() {
        val twinkle = PianoCatalog.findSong("twinkle")!!.notes
        assertEquals(42, twinkle.size)
        assertEquals(listOf(PianoNote.C4, PianoNote.C4, PianoNote.G4, PianoNote.G4, PianoNote.A4, PianoNote.A4, PianoNote.G4), twinkle.take(7))
        assertEquals(twinkle.take(14), twinkle.takeLast(14))
        val mary = PianoCatalog.findSong("mary")!!.notes
        assertEquals(26, mary.size)
        assertEquals(listOf(PianoNote.E4, PianoNote.D4, PianoNote.C4, PianoNote.D4), mary.take(4))
        val row = PianoCatalog.findSong("row")!!.notes
        assertEquals(27, row.size)
        assertEquals(listOf(PianoNote.C5, PianoNote.C5, PianoNote.C5), row.subList(10, 13))
        assertEquals(listOf(PianoNote.G4, PianoNote.F4, PianoNote.E4, PianoNote.D4, PianoNote.C4), row.takeLast(5))
    }
    @Test fun `free play is default and has no scoring or progress`() {
        var state = PianoState()
        PianoCatalog.notes.forEach { state = PianoEngine.press(state, it) }
        assertEquals(PianoState(), state)
        assertEquals(PianoMode.FREE_PLAY, state.mode)
        assertFalse(state.sayNotes)
        assertNull(state.expectedNote)
        assertFalse(state.isComplete)
    }
    @Test fun `model exposes no score Stars or penalty fields`() {
        val names = PianoState::class.java.declaredFields.map { it.name.lowercase() }
        assertTrue(names.none { it in setOf("score", "stars", "points", "penalty", "sessionstars", "totalstars") })
    }
    @Test fun `pure production model has no Android Compose or network imports`() {
        val relative = "src/main/java/com/nahtygal/olivialooi/games/piano"
        val root = File(relative).let { if (it.isDirectory) it else File("app", relative) }
        assertTrue(root.isDirectory)
        root.listFiles()!!.filter { it.extension == "kt" }.forEach { file ->
            file.readLines().filter { it.startsWith("import ") }.forEach { assertTrue(it, it.startsWith("import kotlin.")) }
            assertFalse(file.readText().contains("http://"))
            assertFalse(file.readText().contains("https://"))
        }
    }
    @Test fun `invalid song definition fails early`() {
        assertTrue(runCatching { PianoSong("empty", "Empty", emptyList()) }.isFailure)
        assertTrue(runCatching { PianoSong("", "Missing ID", listOf(PianoNote.C4)) }.isFailure)
    }
}
