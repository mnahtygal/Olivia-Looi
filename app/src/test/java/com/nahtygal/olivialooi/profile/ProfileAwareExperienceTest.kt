package com.nahtygal.olivialooi.profile

import com.nahtygal.olivialooi.games.animals.AnimalPlayMode
import com.nahtygal.olivialooi.games.animals.AnimalQuizEngine
import com.nahtygal.olivialooi.games.animals.AnimalSoundsCatalog
import com.nahtygal.olivialooi.games.puzzles.PuzzleDifficulty
import com.nahtygal.olivialooi.games.puzzles.PuzzleEngine
import com.nahtygal.olivialooi.games.puzzles.PuzzlePicture
import com.nahtygal.olivialooi.games.puzzles.PuzzleStateCodec
import com.nahtygal.olivialooi.stories.StoryCatalog
import com.nahtygal.olivialooi.stories.StoryEngine
import com.nahtygal.olivialooi.stories.ReadingMode
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileAwareExperienceTest {
    private val olivia = KidExperiencePolicy.forProfile(KidProfile.OLIVIA)
    private val eliana = KidExperiencePolicy.forProfile(KidProfile.ELIANA)

    @Test
    fun `new puzzle defaults are conservative and profile aware`() {
        assertEquals(PuzzleDifficulty.MEDIUM, olivia.puzzle.difficulty(null))
        assertEquals(PuzzleDifficulty.EASY, eliana.puzzle.difficulty(null))
    }

    @Test
    fun `every puzzle size remains available to both profiles`() {
        assertEquals(listOf(6, 9, 12, 16, 20, 25), PuzzleDifficulty.entries.map { it.pieceCount })
        KidProfile.entries.forEach { profile ->
            val preference = KidExperiencePolicy.forProfile(profile).puzzle.newPuzzleDifficulty
            assertTrue(preference in PuzzleDifficulty.entries)
        }
    }

    @Test
    fun `restored and explicit puzzle sizes override profile defaults`() {
        val original = PuzzleEngine.newPuzzle(
            PuzzlePicture.BUTTERFLY,
            PuzzleDifficulty.BIG,
            Random(6),
        )
        val restored = PuzzleStateCodec.decode(PuzzleStateCodec.encode(original))!!

        assertEquals(PuzzleDifficulty.BIG, eliana.puzzle.difficulty(restored.difficulty))
        assertEquals(PuzzleDifficulty.SUPER, olivia.puzzle.difficulty(PuzzleDifficulty.SUPER))
    }

    @Test
    fun `profile switching does not mutate an in-progress puzzle`() {
        val started = PuzzleEngine.newPuzzle(
            PuzzlePicture.PUPPY,
            PuzzleDifficulty.GIANT,
            Random(20),
        )
        val inProgress = PuzzleEngine.placePiece(started, 0, 0)

        KidExperiencePolicy.forProfile(KidProfile.ELIANA)

        assertEquals(PuzzleDifficulty.GIANT, inProgress.difficulty)
        assertEquals(1, inProgress.completedPieceCount)
    }

    @Test
    fun `Story entry features each profile preference without filtering catalog`() {
        val oliviaStories = StoryCatalog.featuredFirst(olivia.storyTime.storyId(null))
        val elianaStories = StoryCatalog.featuredFirst(eliana.storyTime.storyId(null))

        assertEquals("moonlight_snowman", oliviaStories.first().id)
        assertEquals("eliana_barnyard_day", elianaStories.first().id)
        assertEquals(StoryCatalog.stories.map { it.id }.toSet(), oliviaStories.map { it.id }.toSet())
        assertEquals(StoryCatalog.stories.map { it.id }.toSet(), elianaStories.map { it.id }.toSet())
    }

    @Test
    fun `restored Story selection overrides profile feature`() {
        val active = StoryEngine.open("rainbow_sisters")
        val restored = StoryEngine.decode(StoryEngine.encode(active))!!

        assertEquals("rainbow_sisters", eliana.storyTime.storyId(restored.storyId))
    }

    @Test
    fun `profile switching does not replace an active Story`() {
        val active = StoryEngine.next(
            StoryEngine.chooseMode(
                StoryEngine.open("sleepy_puppy"),
                ReadingMode.MYSELF,
            ),
        )

        KidExperiencePolicy.forProfile(KidProfile.ELIANA)

        assertEquals("sleepy_puppy", active.storyId)
        assertEquals(1, active.page)
    }

    @Test
    fun `Animal Sounds entry modes are profile aware and all modes remain available`() {
        assertEquals(AnimalPlayMode.WHO_MAKES_THIS_SOUND, olivia.animalSounds.mode(null))
        assertEquals(AnimalPlayMode.FREE_PLAY, eliana.animalSounds.mode(null))
        assertEquals(
            listOf(
                AnimalPlayMode.FREE_PLAY,
                AnimalPlayMode.WHO_MAKES_THIS_SOUND,
                AnimalPlayMode.FIND_THE_ANIMAL,
            ),
            AnimalPlayMode.entries,
        )
        KidProfile.entries.forEach { profile ->
            val preference = KidExperiencePolicy.forProfile(profile).animalSounds.newSessionMode
            assertTrue(preference in AnimalPlayMode.entries)
        }
    }

    @Test
    fun `restored and explicit Animal Sounds modes override profile defaults`() {
        assertEquals(
            AnimalPlayMode.FIND_THE_ANIMAL,
            eliana.animalSounds.mode(AnimalPlayMode.FIND_THE_ANIMAL),
        )
        assertEquals(
            AnimalPlayMode.FREE_PLAY,
            olivia.animalSounds.mode(AnimalPlayMode.FREE_PLAY),
        )
    }

    @Test
    fun `profile switching does not reset an active Animal Sounds round`() {
        val active = AnimalQuizEngine.newRound(
            AnimalPlayMode.FIND_THE_ANIMAL,
            random = Random(4),
        )

        KidExperiencePolicy.forProfile(KidProfile.ELIANA)

        assertEquals(AnimalPlayMode.FIND_THE_ANIMAL, active.mode)
        assertEquals(4, active.choices.size)
    }

    @Test
    fun `no animal disappears for either profile`() {
        val animalIds = AnimalSoundsCatalog.animals.map { it.id }.toSet()

        assertEquals(8, animalIds.size)
        assertEquals(8, AnimalSoundsCatalog.animals.distinctBy { it.id }.size)
    }
}
