package com.nahtygal.olivialooi.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.nahtygal.olivialooi.profile.KidProfile

/** App-level profile source for later personalization missions. */
internal val LocalKidProfile = staticCompositionLocalOf { KidProfile.LEGACY_DEFAULT }
