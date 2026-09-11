package com.nahtygal.olivialooi.games.puzzles

import org.junit.Assert.*
import org.junit.Test

class PuzzleCatalogTest {
    @Test fun `six exact pictures`() {
        assertEquals(listOf("butterfly", "puppy", "snowman", "flower", "rainbow", "house"), PuzzleCatalog.pictures.map { it.stableId })
    }
    @Test fun `picture IDs unique`() {
        assertEquals(6, PuzzleCatalog.pictures.map { it.stableId }.toSet().size)
    }
    @Test fun `names nonblank`() {
        PuzzleCatalog.pictures.forEach { assertTrue(it.displayName.isNotBlank()); assertTrue(it.spokenName.isNotBlank()) }
    }
    @Test fun `lookup works`() {
        PuzzleCatalog.pictures.forEach { assertEquals(it, PuzzleCatalog.findById(it.stableId)) }
    }
    @Test fun `unknown lookup safe`() {
        listOf("", "missing", "BUTTERFLY").forEach { assertNull(PuzzleCatalog.findById(it)) }
    }
    @Test fun `six child friendly difficulties`() {
        assertEquals(listOf("EASY", "MEDIUM", "HARD", "BIG", "GIANT", "SUPER"), PuzzleDifficulty.entries.map { it.name })
    }
    @Test fun `easy six in two by three grid`() {
        assertEquals(6, PuzzleDifficulty.EASY.pieceCount)
        assertEquals(2, PuzzleDifficulty.EASY.columns)
        assertEquals(3, PuzzleDifficulty.EASY.rows)
    }
    @Test fun `medium nine in three by three grid`() {
        assertEquals(9, PuzzleDifficulty.MEDIUM.pieceCount)
        assertEquals(3, PuzzleDifficulty.MEDIUM.columns)
        assertEquals(3, PuzzleDifficulty.MEDIUM.rows)
    }
    @Test fun `hard twelve in three by four grid`() {
        assertEquals(12, PuzzleDifficulty.HARD.pieceCount)
        assertEquals(3, PuzzleDifficulty.HARD.columns)
        assertEquals(4, PuzzleDifficulty.HARD.rows)
    }
    @Test fun `larger grids expose sixteen twenty and twenty five pieces`() {
        assertEquals(listOf(16, 20, 25), PuzzleDifficulty.entries.drop(3).map { it.pieceCount })
        assertEquals(listOf("4x4", "4x5", "5x5"), PuzzleDifficulty.entries.drop(3).map { "${it.columns}x${it.rows}" })
    }
    @Test fun `counts match grid dimensions`() {
        PuzzleDifficulty.entries.forEach { assertEquals(it.rows * it.columns, it.pieceCount) }
    }
    @Test fun `engine and catalogs have no platform or network dependencies`() {
        val relative = "src/main/java/com/nahtygal/olivialooi/games/puzzles"
        val root = java.io.File(relative).let { if (it.isDirectory) it else java.io.File("app", relative) }
        assertTrue(root.isDirectory)
        val files = root.listFiles()!!.filter { it.extension == "kt" }
        assertEquals(3, files.size)
        files.forEach { file ->
            file.readLines().filter { it.startsWith("import ") }.forEach {
                assertTrue("Unexpected dependency: $it", it.startsWith("import kotlin."))
            }
            assertFalse(file.readText().contains("http://"))
            assertFalse(file.readText().contains("https://"))
        }
    }
    @Test fun `local speech has required phrases`() {
        assertEquals("Let’s build the butterfly puzzle!", PuzzleSpeech.phrase(PuzzlePicture.BUTTERFLY, PuzzleMilestone.OPENING))
        assertEquals("Great start!", PuzzleSpeech.phrase(PuzzlePicture.PUPPY, PuzzleMilestone.FIRST_PIECE))
        assertEquals("You’re doing great!", PuzzleSpeech.phrase(PuzzlePicture.FLOWER, PuzzleMilestone.HALFWAY))
        assertEquals("You did it, Olivia! Great puzzle!", PuzzleSpeech.phrase(PuzzlePicture.HOUSE, PuzzleMilestone.COMPLETE))
        assertEquals("Here’s the picture!", PuzzleSpeech.PREVIEW)
    }
}
