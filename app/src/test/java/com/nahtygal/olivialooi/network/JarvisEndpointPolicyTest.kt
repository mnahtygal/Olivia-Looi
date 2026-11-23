package com.nahtygal.olivialooi.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisEndpointPolicyTest {

    @Test
    fun homeReachableSelectsHome() {
        assertEquals(
            JarvisEndpoint.HOME,
            JarvisEndpointPolicy.preferredEndpoint(true),
        )
    }

    @Test
    fun homeUnavailableSelectsAway() {
        assertEquals(
            JarvisEndpoint.AWAY,
            JarvisEndpointPolicy.preferredEndpoint(false),
        )
    }

    @Test
    fun homeUsesConfiguredAddress() {
        assertEquals(
            "https://10.0.0.213",
            JarvisEndpointPolicy.baseUrl(
                JarvisEndpoint.HOME,
                "https://10.0.0.213/",
            ),
        )
    }

    @Test
    fun awayUsesTailscaleHostname() {
        assertEquals(
            "https://y-thor.tail4d5091.ts.net",
            JarvisEndpointPolicy.baseUrl(
                JarvisEndpoint.AWAY,
                "https://10.0.0.213",
            ),
        )
    }

    @Test
    fun safeConnectionFailureCanRetry() {
        assertTrue(
            JarvisEndpointPolicy.shouldRetryOtherEndpoint(
                connectionFailed = true,
                requestMayHaveReachedServer = false,
            ),
        )
    }

    @Test
    fun uncertainDeliveryMustNotRetry() {
        assertFalse(
            JarvisEndpointPolicy.shouldRetryOtherEndpoint(
                connectionFailed = true,
                requestMayHaveReachedServer = true,
            ),
        )
    }
}
