package com.nahtygal.olivialooi.speech

import java.util.Locale

internal data class LooLooVoiceCandidate(
    val name: String,
    val locale: Locale,
    val quality: Int,
    val latency: Int,
    val requiresNetwork: Boolean,
    val features: Set<String> = emptySet(),
)

/** Pure, deterministic voice ranking kept separate from the Android TTS lifecycle. */
internal object LooLooVoiceSelector {
    fun select(
        voices: Collection<LooLooVoiceCandidate>,
        preferredVoiceName: String? = null,
    ): LooLooVoiceCandidate? {
        val configuredName = preferredVoiceName?.trim().orEmpty()
        if (configuredName.isNotEmpty()) {
            voices.firstOrNull { it.name == configuredName }?.let { return it }
        }

        return voices
            .asSequence()
            .filter { it.locale.language.equals(Locale.ENGLISH.language, ignoreCase = true) }
            .sortedWith(
                compareByDescending<LooLooVoiceCandidate> {
                    it.locale.country.equals(Locale.US.country, ignoreCase = true)
                }
                    .thenBy { it.requiresNetwork }
                    .thenByDescending { it.quality }
                    .thenBy { it.latency }
                    .thenBy { it.name },
            )
            .firstOrNull()
    }
}
