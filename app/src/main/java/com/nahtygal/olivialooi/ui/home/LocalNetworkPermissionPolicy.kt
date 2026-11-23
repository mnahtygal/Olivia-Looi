package com.nahtygal.olivialooi.ui.home

internal enum class LooLooPermission {
    LocalNetwork,
    Microphone,
}

internal sealed interface LooLooPermissionDecision {
    data object Ready : LooLooPermissionDecision

    data class Request(val permissions: Set<LooLooPermission>) : LooLooPermissionDecision

    data object LocalNetworkUnavailable : LooLooPermissionDecision

    data object MicrophoneUnavailable : LooLooPermissionDecision
}

internal object LocalNetworkPermissionPolicy {
    const val RUNTIME_PERMISSION_API = 37
    const val PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

    fun requiresRuntimePermission(sdkInt: Int): Boolean =
        sdkInt >= RUNTIME_PERMISSION_API

    fun decide(
        sdkInt: Int,
        localNetworkGranted: Boolean,
        microphoneGranted: Boolean,
        localNetworkRequestAttempted: Boolean,
        microphoneRequestAttempted: Boolean,
    ): LooLooPermissionDecision {
        val localNetworkMissing =
            requiresRuntimePermission(sdkInt) && !localNetworkGranted

        if (localNetworkMissing && localNetworkRequestAttempted) {
            return LooLooPermissionDecision.LocalNetworkUnavailable
        }
        if (!microphoneGranted && microphoneRequestAttempted) {
            return LooLooPermissionDecision.MicrophoneUnavailable
        }

        val permissions = buildSet {
            if (localNetworkMissing) add(LooLooPermission.LocalNetwork)
            if (!microphoneGranted) add(LooLooPermission.Microphone)
        }
        return if (permissions.isEmpty()) {
            LooLooPermissionDecision.Ready
        } else {
            LooLooPermissionDecision.Request(permissions)
        }
    }
}
