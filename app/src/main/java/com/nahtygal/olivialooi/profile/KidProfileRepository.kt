package com.nahtygal.olivialooi.profile

internal interface KidProfileRepository {
    val selectedProfile: KidProfile

    fun selectProfile(profile: KidProfile)
}

/** Small persistence seam that keeps the repository independently testable. */
internal interface KidProfileIdStore {
    fun readProfileId(): String?

    fun writeProfileId(stableId: String)
}

internal class PersistentKidProfileRepository(
    private val store: KidProfileIdStore,
) : KidProfileRepository {
    override val selectedProfile: KidProfile
        get() = KidProfile.fromStableId(store.readProfileId()) ?: KidProfile.LEGACY_DEFAULT

    override fun selectProfile(profile: KidProfile) {
        store.writeProfileId(profile.stableId)
    }
}
