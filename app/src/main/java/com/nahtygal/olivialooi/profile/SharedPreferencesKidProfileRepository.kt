package com.nahtygal.olivialooi.profile

import android.content.Context

internal class SharedPreferencesKidProfileRepository(context: Context) : KidProfileRepository {
    private val delegate = PersistentKidProfileRepository(
        SharedPreferencesKidProfileIdStore(context.applicationContext),
    )

    override val selectedProfile: KidProfile
        get() = delegate.selectedProfile

    override fun selectProfile(profile: KidProfile) {
        delegate.selectProfile(profile)
    }
}

private class SharedPreferencesKidProfileIdStore(context: Context) : KidProfileIdStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun readProfileId(): String? = runCatching {
        preferences.getString(SELECTED_PROFILE_ID_KEY, null)
    }.getOrNull()

    override fun writeProfileId(stableId: String) {
        preferences.edit().putString(SELECTED_PROFILE_ID_KEY, stableId).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "kid_profile"
        const val SELECTED_PROFILE_ID_KEY = "selected_profile_id"
    }
}
