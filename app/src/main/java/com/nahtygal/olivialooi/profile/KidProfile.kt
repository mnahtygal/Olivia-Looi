package com.nahtygal.olivialooi.profile

/** A child identity whose persisted ID is independent of display text. */
internal enum class KidProfile(
    val stableId: String,
    val displayName: String,
) {
    OLIVIA(stableId = "olivia", displayName = "Olivia"),
    ELIANA(stableId = "eliana", displayName = "Eliana"),
    ;

    internal companion object {
        /** Preserves the behavior of installations created before profiles existed. */
        val LEGACY_DEFAULT: KidProfile = OLIVIA

        fun fromStableId(stableId: String?): KidProfile? =
            entries.singleOrNull { profile -> profile.stableId == stableId }
    }
}
