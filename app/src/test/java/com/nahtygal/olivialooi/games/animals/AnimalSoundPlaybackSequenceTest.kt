package com.nahtygal.olivialooi.games.animals

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimalSoundPlaybackSequenceTest {
    @Test
    fun `newer selection invalidates older selection`() {
        val sequence = AnimalSoundPlaybackSequence()
        val oldSelection = sequence.beginSelection()
        val newSelection = sequence.beginSelection()

        assertFalse(sequence.completeSpeech(oldSelection))
        assertTrue(sequence.completeSpeech(newSelection))
    }

    @Test
    fun `current completion may trigger audio`() {
        val sequence = AnimalSoundPlaybackSequence()
        val selection = sequence.beginSelection()

        assertTrue(sequence.completeSpeech(selection))
    }

    @Test
    fun `stale duplicate completion cannot trigger audio`() {
        val sequence = AnimalSoundPlaybackSequence()
        val selection = sequence.beginSelection()

        assertTrue(sequence.completeSpeech(selection))
        assertFalse(sequence.completeSpeech(selection))
    }

    @Test
    fun `leaving screen invalidates pending playback`() {
        val sequence = AnimalSoundPlaybackSequence()
        val selection = sequence.beginSelection()

        sequence.invalidate()

        assertFalse(sequence.completeSpeech(selection))
    }
}
