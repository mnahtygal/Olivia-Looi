package com.nahtygal.olivialooi.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalNetworkPermissionPolicyTest {
    @Test
    fun `policy uses the Android local network permission name`() {
        assertEquals(
            "android.permission.ACCESS_LOCAL_NETWORK",
            LocalNetworkPermissionPolicy.PERMISSION,
        )
    }

    @Test
    fun `older Android versions do not require local network permission`() {
        assertEquals(
            LooLooPermissionDecision.Ready,
            decide(sdkInt = 36, localNetworkGranted = false),
        )
    }

    @Test
    fun `Android 17 requests local network permission before connecting`() {
        assertEquals(
            LooLooPermissionDecision.Request(setOf(LooLooPermission.LocalNetwork)),
            decide(sdkInt = 37, localNetworkGranted = false),
        )
    }

    @Test
    fun `missing local network and microphone permissions are requested together`() {
        assertEquals(
            LooLooPermissionDecision.Request(
                setOf(
                    LooLooPermission.LocalNetwork,
                    LooLooPermission.Microphone,
                ),
            ),
            decide(
                sdkInt = 37,
                localNetworkGranted = false,
                microphoneGranted = false,
            ),
        )
    }

    @Test
    fun `granted local network permission preserves microphone request behavior`() {
        assertEquals(
            LooLooPermissionDecision.Request(setOf(LooLooPermission.Microphone)),
            decide(
                sdkInt = 37,
                localNetworkGranted = true,
                microphoneGranted = false,
            ),
        )
    }

    @Test
    fun `denied local network permission is not requested repeatedly`() {
        assertEquals(
            LooLooPermissionDecision.LocalNetworkUnavailable,
            decide(
                sdkInt = 37,
                localNetworkGranted = false,
                localNetworkRequestAttempted = true,
            ),
        )
    }

    @Test
    fun `granted permissions allow LooLoo to continue`() {
        assertEquals(
            LooLooPermissionDecision.Ready,
            decide(sdkInt = 37, localNetworkGranted = true),
        )
    }

    private fun decide(
        sdkInt: Int,
        localNetworkGranted: Boolean,
        microphoneGranted: Boolean = true,
        localNetworkRequestAttempted: Boolean = false,
        microphoneRequestAttempted: Boolean = false,
    ): LooLooPermissionDecision = LocalNetworkPermissionPolicy.decide(
        sdkInt = sdkInt,
        localNetworkGranted = localNetworkGranted,
        microphoneGranted = microphoneGranted,
        localNetworkRequestAttempted = localNetworkRequestAttempted,
        microphoneRequestAttempted = microphoneRequestAttempted,
    )
}
