package com.nahtygal.olivialooi.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.nahtygal.olivialooi.profile.KidProfile
import com.nahtygal.olivialooi.profile.KidProfileRepository

/** App-level profile source for later personalization missions. */
internal val LocalKidProfile = staticCompositionLocalOf { KidProfile.LEGACY_DEFAULT }

/** Compose-facing session state; all mutations still flow through the repository. */
internal class ActiveKidProfileState(
    private val repository: KidProfileRepository,
) {
    var activeProfile by mutableStateOf(repository.selectedProfile)
        private set

    fun selectProfile(profile: KidProfile) {
        repository.selectProfile(profile)
        activeProfile = repository.selectedProfile
    }
}
