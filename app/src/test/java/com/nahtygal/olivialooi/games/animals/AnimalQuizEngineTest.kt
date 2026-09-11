package com.nahtygal.olivialooi.games.animals

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class AnimalQuizEngineTest {
    @Test fun `catalog IDs are unique and audio assets are local`() {
        assertEquals(AnimalSoundsCatalog.animals.size, AnimalSoundsCatalog.animals.map { it.id }.toSet().size)
        AnimalSoundsCatalog.animals.forEach { assertTrue(it.localAudioAssetName.isNotBlank()) }
    }
    @Test fun `round has one answer and three distinct choices`() {
        val round = AnimalQuizEngine.newRound(AnimalPlayMode.FIND_THE_ANIMAL, random = Random(4))
        assertEquals(4, round.choices.size)
        assertEquals(4, round.choices.toSet().size)
        assertTrue(round.target in round.choices)
    }
    @Test fun `answers are friendly boolean state with no score`() {
        val round = AnimalQuizEngine.newRound(AnimalPlayMode.WHO_MAKES_THIS_SOUND, random = Random(2))
        assertTrue(AnimalQuizEngine.answer(round, round.target).lastAnswerCorrect == true)
        assertTrue(AnimalQuizEngine.answer(round, round.choices.first { it != round.target }).lastAnswerCorrect == false)
    }
    @Test fun `advance makes a new round`() {
        val round = AnimalQuizEngine.newRound(AnimalPlayMode.FIND_THE_ANIMAL, random = Random(2))
        val next = AnimalQuizEngine.advance(round, Random(3))
        assertEquals(1, next.round)
        assertEquals(round.mode, next.mode)
        assertNull(next.lastAnswerCorrect)
    }
}
