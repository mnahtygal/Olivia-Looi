package com.nahtygal.olivialooi.games.abc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlphabetCatalogTest {
    @Test
    fun `catalog contains exactly A through Z in order`() {
        assertEquals(26, AlphabetCatalog.entries.size)
        assertEquals(('A'..'Z').toList(), AlphabetCatalog.entries.map(AlphabetEntry::letter))
    }

    @Test
    fun `stable IDs and letters are unique`() {
        assertEquals(26, AlphabetCatalog.entries.map(AlphabetEntry::stableId).distinct().size)
        assertEquals(26, AlphabetCatalog.entries.map(AlphabetEntry::letter).distinct().size)
    }

    @Test
    fun `every entry has complete local metadata`() {
        AlphabetCatalog.entries.forEach { entry ->
            assertTrue(entry.displayWord.isNotBlank())
            assertTrue(entry.spokenWord.isNotBlank())
            assertTrue(entry.visualSymbol.isNotBlank())
            assertTrue(entry.accessibilityDescription.isNotBlank())
            assertTrue(entry.displayWord.startsWith(entry.letter, ignoreCase = true))
            assertTrue(entry.spokenWord.startsWith(entry.letter, ignoreCase = true))
            assertTrue(
                listOf(
                    entry.stableId,
                    entry.displayWord,
                    entry.spokenWord,
                    entry.visualSymbol,
                    entry.accessibilityDescription,
                ).none { "://" in it },
            )
        }
    }

    @Test
    fun `catalog uses the exact requested words`() {
        assertEquals(
            listOf(
                "Apple", "Ball", "Cat", "Dog", "Egg", "Fish", "Goat", "Hat", "Ice Cream",
                "Juice", "Kite", "Lion", "Moon", "Nest", "Orange", "Pig", "Queen", "Rainbow",
                "Star", "Tree", "Umbrella", "Violin", "Whale", "Xylophone", "Yo-Yo", "Zebra",
            ),
            AlphabetCatalog.entries.map(AlphabetEntry::displayWord),
        )
    }

    @Test
    fun `lookup by letter and ID works and unknown values fail safely`() {
        assertEquals("Apple", AlphabetCatalog.findByLetter('A')?.displayWord)
        assertEquals("Zebra", AlphabetCatalog.findByLetter('z')?.displayWord)
        assertEquals('C', AlphabetCatalog.findById("c")?.letter)
        assertNull(AlphabetCatalog.findByLetter('?'))
        assertNull(AlphabetCatalog.findById("dragon"))
        assertNull(AlphabetCatalog.findById(""))
    }
}
