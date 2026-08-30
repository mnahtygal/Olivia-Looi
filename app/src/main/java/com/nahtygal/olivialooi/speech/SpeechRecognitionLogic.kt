package com.nahtygal.olivialooi.speech

internal fun bestRecognitionResult(candidates: List<String>?): String? =
    candidates
        ?.asSequence()
        ?.map(String::trim)
        ?.firstOrNull(String::isNotEmpty)

internal fun speechRecognizerRmsToLevel(rmsDecibels: Float): Float =
    ((rmsDecibels - SILENCE_RMS_DB) / (LOUD_RMS_DB - SILENCE_RMS_DB))
        .coerceIn(0f, 1f)

private const val SILENCE_RMS_DB = 1f
private const val LOUD_RMS_DB = 10f
