package com.nahtygal.olivialooi.ui

import com.nahtygal.olivialooi.profile.KidProfile
import com.nahtygal.olivialooi.profile.KidProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ActiveKidProfileStateTest {
    @Test
    fun `active profile initially reflects repository selection`() {
        val state = ActiveKidProfileState(FakeRepository(KidProfile.ELIANA))

        assertEquals(KidProfile.ELIANA, state.activeProfile)
    }

    @Test
    fun `selecting Eliana persists and updates active state immediately`() {
        val repository = FakeRepository(KidProfile.OLIVIA)
        val state = ActiveKidProfileState(repository)

        state.selectProfile(KidProfile.ELIANA)

        assertEquals(KidProfile.ELIANA, repository.selectedProfile)
        assertEquals(KidProfile.ELIANA, state.activeProfile)
    }

    @Test
    fun `selecting Olivia persists and updates active state immediately`() {
        val repository = FakeRepository(KidProfile.ELIANA)
        val state = ActiveKidProfileState(repository)

        state.selectProfile(KidProfile.OLIVIA)

        assertEquals(KidProfile.OLIVIA, repository.selectedProfile)
        assertEquals(KidProfile.OLIVIA, state.activeProfile)
    }

    @Test
    fun `Olivia to Eliana to Olivia works in one state holder`() {
        val repository = FakeRepository(KidProfile.OLIVIA)
        val state = ActiveKidProfileState(repository)
        val originalStateHolder = state

        state.selectProfile(KidProfile.ELIANA)
        assertEquals(KidProfile.ELIANA, state.activeProfile)
        state.selectProfile(KidProfile.OLIVIA)

        assertSame(originalStateHolder, state)
        assertEquals(KidProfile.OLIVIA, repository.selectedProfile)
        assertEquals(KidProfile.OLIVIA, state.activeProfile)
        assertEquals(listOf(KidProfile.ELIANA, KidProfile.OLIVIA), repository.selections)
    }

    private class FakeRepository(initialProfile: KidProfile) : KidProfileRepository {
        override var selectedProfile: KidProfile = initialProfile
            private set
        val selections = mutableListOf<KidProfile>()

        override fun selectProfile(profile: KidProfile) {
            selections += profile
            selectedProfile = profile
        }
    }
}
