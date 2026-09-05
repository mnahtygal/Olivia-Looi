package com.nahtygal.olivialooi.games.piano

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Offline build-time utility for our own synthesized, gently decaying piano-like tones.
 * Not invoked by the app. Packaged samples need no synthesis, file writes, or decoding on tap.
 */
object PianoSampleGenerator {
    const val SAMPLE_RATE = 44100
    const val DURATION_SECONDS = 1.5

    fun wav(note: PianoNote): ByteArray {
        val frames = (SAMPLE_RATE * DURATION_SECONDS).toInt()
        val dataBytes = frames * 2
        val buffer = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII)).putInt(36 + dataBytes)
        buffer.put("WAVEfmt ".toByteArray(Charsets.US_ASCII)).putInt(16)
        buffer.putShort(1).putShort(1).putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort(2).putShort(16)
        buffer.put("data".toByteArray(Charsets.US_ASCII)).putInt(dataBytes)
        repeat(frames) { frame ->
            val t = frame.toDouble() / SAMPLE_RATE
            val phase = 2 * PI * note.frequencyHz * t
            val attack = (t / .004).coerceAtMost(1.0)
            val fade = ((DURATION_SECONDS - t) / .12).coerceIn(0.0, 1.0)
            val fundamental = sin(phase) * exp(-3.0 * t)
            val second = .30 * sin(2 * phase) * exp(-4.5 * t)
            val third = .14 * sin(3 * phase) * exp(-6 * t)
            val fourth = .07 * sin(4 * phase) * exp(-8 * t)
            val sample = ((fundamental + second + third + fourth) * attack * fade * .42 * Short.MAX_VALUE).toInt()
            buffer.putShort(sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
        }
        return buffer.array()
    }

    @JvmStatic fun main(args: Array<String>) {
        val directory = File(args.single()).also { require(it.isDirectory) }
        PianoCatalog.notes.forEach { File(directory, "${it.audioIdentity}.wav").writeBytes(wav(it)) }
        println("Generated eight original mono PCM piano samples.")
    }
}
