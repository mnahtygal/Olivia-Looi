package com.nahtygal.olivialooi.profile

import com.nahtygal.olivialooi.games.animals.AnimalPlayMode
import com.nahtygal.olivialooi.games.puzzles.PuzzleDifficulty
import com.nahtygal.olivialooi.stories.StoryCatalog
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KidExperiencePolicyTest {
    @Test
    fun `Olivia maps deterministically to Olivia experience defaults`() {
        assertEquals(
            KidExperiencePolicy(
                PuzzleExperiencePreference(PuzzleDifficulty.MEDIUM),
                StoryExperiencePreference("moonlight_snowman"),
                AnimalSoundsExperiencePreference(AnimalPlayMode.WHO_MAKES_THIS_SOUND),
            ),
            KidExperiencePolicy.forProfile(KidProfile.OLIVIA),
        )
    }

    @Test
    fun `Eliana maps deterministically to Eliana experience defaults`() {
        assertEquals(
            KidExperiencePolicy(
                PuzzleExperiencePreference(PuzzleDifficulty.EASY),
                StoryExperiencePreference("eliana_barnyard_day"),
                AnimalSoundsExperiencePreference(AnimalPlayMode.FREE_PLAY),
            ),
            KidExperiencePolicy.forProfile(KidProfile.ELIANA),
        )
    }

    @Test
    fun `policy has no Android or Compose dependencies`() {
        val source = projectFile(
            "src/main/java/com/nahtygal/olivialooi/profile/KidExperiencePolicy.kt",
        ).readText()

        assertFalse(source.contains("import android."))
        assertFalse(source.contains("import androidx."))
        assertFalse(source.contains("compose", ignoreCase = true))
    }

    @Test
    fun `derivation does not mutate profiles`() {
        val identities = KidProfile.entries.map { it.stableId to it.displayName }

        KidProfile.entries.forEach(KidExperiencePolicy::forProfile)

        assertEquals(identities, KidProfile.entries.map { it.stableId to it.displayName })
    }

    @Test
    fun `repeated derivation returns equivalent policies`() {
        KidProfile.entries.forEach { profile ->
            assertEquals(
                KidExperiencePolicy.forProfile(profile),
                KidExperiencePolicy.forProfile(profile),
            )
        }
    }

    @Test
    fun `puzzle defaults are supported difficulties`() {
        KidProfile.entries.forEach { profile ->
            assertTrue(
                KidExperiencePolicy.forProfile(profile).puzzle.newPuzzleDifficulty in
                    PuzzleDifficulty.entries,
            )
        }
    }

    @Test
    fun `featured story IDs exist in the bundled catalog`() {
        KidProfile.entries.forEach { profile ->
            assertTrue(
                StoryCatalog.find(
                    KidExperiencePolicy.forProfile(profile).storyTime.featuredStoryId,
                ) != null,
            )
        }
    }

    @Test
    fun `Animal Sounds defaults are supported modes`() {
        KidProfile.entries.forEach { profile ->
            assertTrue(
                KidExperiencePolicy.forProfile(profile).animalSounds.newSessionMode in
                    AnimalPlayMode.entries,
            )
        }
    }

    private fun projectFile(relativePath: String): File = File(relativePath).let { direct ->
        if (direct.exists()) direct else File("app", relativePath)
    }
}
