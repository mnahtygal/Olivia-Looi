package com.nahtygal.olivialooi.network

internal enum class JarvisEndpoint {
    HOME,
    AWAY,
}

internal object JarvisEndpointPolicy {
    const val AWAY_BASE_URL = "https://y-thor.tail4d5091.ts.net"

    fun preferredEndpoint(homeReachable: Boolean): JarvisEndpoint =
        if (homeReachable) JarvisEndpoint.HOME else JarvisEndpoint.AWAY

    fun baseUrl(
        endpoint: JarvisEndpoint,
        homeBaseUrl: String,
    ): String = when (endpoint) {
        JarvisEndpoint.HOME -> homeBaseUrl.trimEnd('/')
        JarvisEndpoint.AWAY -> AWAY_BASE_URL
    }

    fun shouldRetryOtherEndpoint(
        connectionFailed: Boolean,
        requestMayHaveReachedServer: Boolean,
    ): Boolean = connectionFailed && !requestMayHaveReachedServer
}
