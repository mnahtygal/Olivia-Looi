package com.nahtygal.olivialooi.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class AwayModePermissionTest {

    @Test
    fun awayModeDoesNotRequireLocalNetworkPermission() {
        assertEquals(
            LooLooPermissionDecision.Ready,
            LocalNetworkPermissionPolicy.decide(
                sdkInt = 37,
                localNetworkGranted = false,
                microphoneGranted = true,
                localNetworkRequestAttempted = false,
                microphoneRequestAttempted = false,
                requiresHomeNetwork = false,
            ),
        )
    }

    @Test
    fun awayModeStillRequiresMicrophonePermission() {
        assertEquals(
            LooLooPermissionDecision.Request(
                setOf(LooLooPermission.Microphone),
            ),
            LocalNetworkPermissionPolicy.decide(
                sdkInt = 37,
                localNetworkGranted = false,
                microphoneGranted = false,
                localNetworkRequestAttempted = false,
                microphoneRequestAttempted = false,
                requiresHomeNetwork = false,
            ),
        )
    }
}
