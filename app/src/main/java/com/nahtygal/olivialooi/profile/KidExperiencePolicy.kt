package com.nahtygal.olivialooi.profile

import com.nahtygal.olivialooi.games.animals.AnimalPlayMode
import com.nahtygal.olivialooi.games.puzzles.PuzzleDifficulty

internal data class PuzzleExperiencePreference(
    val newPuzzleDifficulty: PuzzleDifficulty,
) {
    fun difficulty(currentDifficulty: PuzzleDifficulty?): PuzzleDifficulty =
        currentDifficulty ?: newPuzzleDifficulty
}

internal data class StoryExperiencePreference(
    val featuredStoryId: String,
) {
    fun storyId(currentStoryId: String?): String = currentStoryId ?: featuredStoryId
}

internal data class AnimalSoundsExperiencePreference(
    val newSessionMode: AnimalPlayMode,
) {
    fun mode(currentMode: AnimalPlayMode?): AnimalPlayMode = currentMode ?: newSessionMode
}

/** Pure, immutable experience defaults derived from the active child identity. */
internal data class KidExperiencePolicy(
    val puzzle: PuzzleExperiencePreference,
    val storyTime: StoryExperiencePreference,
    val animalSounds: AnimalSoundsExperiencePreference,
) {
    companion object {
        private val OLIVIA = KidExperiencePolicy(
            puzzle = PuzzleExperiencePreference(PuzzleDifficulty.MEDIUM),
            storyTime = StoryExperiencePreference("moonlight_snowman"),
            animalSounds = AnimalSoundsExperiencePreference(
                AnimalPlayMode.WHO_MAKES_THIS_SOUND,
            ),
        )
        private val ELIANA = KidExperiencePolicy(
            puzzle = PuzzleExperiencePreference(PuzzleDifficulty.EASY),
            storyTime = StoryExperiencePreference("eliana_barnyard_day"),
            animalSounds = AnimalSoundsExperiencePreference(AnimalPlayMode.FREE_PLAY),
        )

        fun forProfile(profile: KidProfile): KidExperiencePolicy = when (profile) {
            KidProfile.OLIVIA -> OLIVIA
            KidProfile.ELIANA -> ELIANA
        }
    }
}
