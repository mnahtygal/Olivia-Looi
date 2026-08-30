package com.nahtygal.olivialooi.network

import com.nahtygal.olivialooi.BuildConfig

internal object JarvisChatConfig {
    val baseUrl: String = BuildConfig.JARVIS_BASE_URL.trimEnd('/')
    val bearerToken: String = BuildConfig.JARVIS_HANDHELD_TOKEN
    const val CHAT_PATH = "/handheld/v1/chat"
}
