package com.nahtygal.olivialooi.games.animals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimalSoundsCatalogTest {
    @Test
    fun `catalog contains exactly eight animals`() {
        assertEquals(8, AnimalSoundsCatalog.animals.size)
    }

    @Test
    fun `stable IDs are unique`() {
        val ids = AnimalSoundsCatalog.animals.map { it.id.stableId }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `display names are unique`() {
        val names = AnimalSoundsCatalog.animals.map(AnimalSound::displayName)
        assertEquals(names.size, names.distinct().size)
    }

    @Test
    fun `every animal has speech sound and local visual`() {
        AnimalSoundsCatalog.animals.forEach { animal ->
            assertTrue(animal.spokenPhrase.isNotBlank())
            assertTrue(animal.soundWord.isNotBlank())
            assertTrue(animal.visualSymbol.isNotBlank())
        }
    }

    @Test
    fun `each animal maps to its expected sound`() {
        val expectedSounds = mapOf(
            AnimalId.Cow to "Moo!",
            AnimalId.Dog to "Woof!",
            AnimalId.Cat to "Meow!",
            AnimalId.Pig to "Oink!",
            AnimalId.Duck to "Quack!",
            AnimalId.Sheep to "Baa!",
            AnimalId.Horse to "Neigh!",
            AnimalId.Frog to "Ribbit!",
        )

        assertEquals(expectedSounds, AnimalSoundsCatalog.animals.associate { it.id to it.soundWord })
    }

    @Test
    fun `each animal has its expected toddler phrase`() {
        val expectedPhrases = mapOf(
            AnimalId.Cow to "The cow says moo!",
            AnimalId.Dog to "The dog says woof woof!",
            AnimalId.Cat to "The cat says meow!",
            AnimalId.Pig to "The pig says oink oink!",
            AnimalId.Duck to "The duck says quack quack!",
            AnimalId.Sheep to "The sheep says baa!",
            AnimalId.Horse to "The horse says neigh!",
            AnimalId.Frog to "The frog says ribbit!",
        )

        assertEquals(
            expectedPhrases,
            AnimalSoundsCatalog.animals.associate { it.id to it.spokenPhrase },
        )
    }

    @Test
    fun `catalog ordering is deterministic`() {
        assertEquals(
            listOf(
                AnimalId.Cow,
                AnimalId.Dog,
                AnimalId.Cat,
                AnimalId.Pig,
                AnimalId.Duck,
                AnimalId.Sheep,
                AnimalId.Horse,
                AnimalId.Frog,
            ),
            AnimalSoundsCatalog.animals.map(AnimalSound::id),
        )
    }

    @Test
    fun `lookup finds known ID and unknown ID fails safely`() {
        assertEquals(AnimalId.Cow, AnimalSoundsCatalog.findById("cow")?.id)
        assertEquals(AnimalId.Frog, AnimalSoundsCatalog.findById("frog")?.id)
        assertNull(AnimalSoundsCatalog.findById("dragon"))
        assertNull(AnimalSoundsCatalog.findById(""))
    }

    @Test
    fun `each animal maps to its expected local audio asset`() {
        val expectedAssets = mapOf(
            AnimalId.Cow to "animal_cow_moo",
            AnimalId.Dog to "animal_dog_bark",
            AnimalId.Cat to "animal_cat_meow",
            AnimalId.Pig to "animal_pig_oink",
            AnimalId.Duck to "animal_duck_quack",
            AnimalId.Sheep to "animal_sheep_baa",
            AnimalId.Horse to "animal_horse_neigh",
            AnimalId.Frog to "animal_frog_ribbit",
        )

        assertEquals(
            expectedAssets,
            AnimalSoundsCatalog.animals.associate { it.id to it.localAudioAssetName },
        )
    }

    @Test
    fun `local audio asset names are present unique and local-only`() {
        val assetNames = AnimalSoundsCatalog.animals.map(AnimalSound::localAudioAssetName)
        assertEquals(assetNames.size, assetNames.distinct().size)
        AnimalSoundsCatalog.animals.forEach { animal ->
            val content = listOf(
                animal.displayName,
                animal.visualSymbol,
                animal.spokenPhrase,
                animal.soundWord,
                animal.localAudioAssetName,
            )
            assertTrue(content.none { "://" in it })
            assertTrue(animal.localAudioAssetName.isNotBlank())
        }
    }
}
