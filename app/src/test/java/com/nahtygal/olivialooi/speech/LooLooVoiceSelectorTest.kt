package com.nahtygal.olivialooi.speech

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LooLooVoiceSelectorTest {
    @Test
    fun usEnglishIsPreferred() {
        val british = voice("en-gb", Locale.UK, quality = 500)
        val american = voice("en-us", Locale.US, quality = 300)

        assertEquals(american, LooLooVoiceSelector.select(listOf(british, american)))
    }

    @Test
    fun localVoiceIsPreferredOverNetworkVoiceForSameLocale() {
        val network = voice("network", Locale.US, quality = 500, requiresNetwork = true)
        val local = voice("local", Locale.US, quality = 300, requiresNetwork = false)

        assertEquals(local, LooLooVoiceSelector.select(listOf(network, local)))
    }

    @Test
    fun higherQualityLocalVoiceIsPreferred() {
        val normal = voice("normal", Locale.US, quality = 300)
        val high = voice("high", Locale.US, quality = 500)

        assertEquals(high, LooLooVoiceSelector.select(listOf(normal, high)))
    }

    @Test
    fun configuredPreferredVoiceWinsWhenAvailable() {
        val rankedWinner = voice("ranked", Locale.US, quality = 500)
        val configured = voice("configured", Locale.UK, quality = 200, requiresNetwork = true)

        assertEquals(
            configured,
            LooLooVoiceSelector.select(listOf(rankedWinner, configured), "configured"),
        )
    }

    @Test
    fun unavailablePreferredVoiceFallsBackToRanking() {
        val fallback = voice("fallback", Locale.US)

        assertEquals(fallback, LooLooVoiceSelector.select(listOf(fallback), "missing"))
    }

    @Test
    fun noEnglishVoiceReturnsNull() {
        assertNull(LooLooVoiceSelector.select(listOf(voice("french", Locale.FRANCE))))
    }

    @Test
    fun emptyVoiceListReturnsNull() {
        assertNull(LooLooVoiceSelector.select(emptyList()))
    }

    private fun voice(
        name: String,
        locale: Locale,
        quality: Int = 300,
        requiresNetwork: Boolean = false,
    ) = LooLooVoiceCandidate(
        name = name,
        locale = locale,
        quality = quality,
        latency = 200,
        requiresNetwork = requiresNetwork,
    )
}
