package com.nahtygal.olivialooi.apps

import com.nahtygal.olivialooi.R

enum class KidAppIcon {
    Video,
    Magic,
}

data class KidApp(
    val id: String,
    val packageName: String,
    val displayNameResource: Int,
    val accessibilityDescriptionResource: Int,
    val icon: KidAppIcon,
)

object KidApps {
    val all: List<KidApp> = listOf(
        KidApp(
            id = "youtube_kids",
            packageName = "com.google.android.apps.youtube.kids",
            displayNameResource = R.string.youtube_kids_name,
            accessibilityDescriptionResource = R.string.open_youtube_kids_description,
            icon = KidAppIcon.Video,
        ),
        KidApp(
            id = "disney_plus",
            packageName = "com.disney.disneyplus",
            displayNameResource = R.string.disney_plus_name,
            accessibilityDescriptionResource = R.string.open_disney_plus_description,
            icon = KidAppIcon.Magic,
        ),
    )

    fun findByPackageName(packageName: String): KidApp? =
        all.firstOrNull { it.packageName == packageName }
}
