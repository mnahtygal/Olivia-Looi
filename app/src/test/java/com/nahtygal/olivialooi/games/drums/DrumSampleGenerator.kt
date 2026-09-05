package com.nahtygal.olivialooi.games.drums

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/** Original deterministic synthesis, run offline by this utility, never on a pad press. */
object DrumSampleGenerator {
    const val SAMPLE_RATE = 44100
    fun duration(sound: DrumSound): Double = when (sound) {
        DrumSound.KICK -> .55
        DrumSound.SNARE -> .28
        DrumSound.TOM -> .65
        DrumSound.CLAP -> .24
        DrumSound.TAMBOURINE -> .55
        DrumSound.CYMBAL -> 1.1
    }
    fun wav(sound: DrumSound): ByteArray {
        val duration = duration(sound)
        val frames = (duration * SAMPLE_RATE).toInt()
        val samples = DoubleArray(frames)
        val random = Random(2300 + sound.ordinal)
        var lastNoise = 0.0
        var phase = 0.0
        var peak = 0.0
        for (frame in samples.indices) {
            val t = frame.toDouble() / SAMPLE_RATE
            val noise = random.nextDouble(-1.0, 1.0)
            val bright = (noise - lastNoise) * .5
            lastNoise = noise
            val value = when (sound) {
                DrumSound.KICK -> {
                    phase += 2 * PI * (48 + 108 * exp(-48 * t)) / SAMPLE_RATE
                    sin(phase) * exp(-9 * t) + .10 * noise * exp(-160 * t)
                }
                DrumSound.SNARE -> .72 * bright * exp(-20 * t) + .25 * sin(2 * PI * 185 * t) * exp(-28 * t)
                DrumSound.TOM -> {
                    phase += 2 * PI * (126 + 58 * exp(-32 * t)) / SAMPLE_RATE
                    (sin(phase) + .23 * sin(phase * 1.59)) * exp(-7.5 * t)
                }
                DrumSound.CLAP -> {
                    var envelope = 0.0
                    for (start in doubleArrayOf(0.0, .011, .024, .038)) {
                        if (t >= start) envelope += exp(-(t - start) * if (start < .038) 220 else 37)
                    }
                    tanh(bright * 3) * envelope
                }
                DrumSound.TAMBOURINE -> {
                    val jingle = (sin(2 * PI * 3700 * t) + sin(2 * PI * 5231 * t) + sin(2 * PI * 7183 * t)) / 3
                    val shake = .45 + .55 * abs(sin(2 * PI * 18 * t))
                    (.65 * bright + .35 * jingle) * shake * exp(-7 * t)
                }
                DrumSound.CYMBAL -> {
                    val metal = (sin(2 * PI * 731 * t) + sin(2 * PI * 1133 * t) + sin(2 * PI * 1847 * t) + sin(2 * PI * 2911 * t)) / 4
                    (.55 * bright + .45 * metal) * exp(-4.8 * t)
                }
            }
            val attack = (t / .0015).coerceAtMost(1.0)
            val release = ((duration - t) / .04).coerceIn(0.0, 1.0)
            samples[frame] = value * attack * release
            peak = maxOf(peak, abs(samples[frame]))
        }
        val bytes = frames * 2
        val buffer = ByteBuffer.allocate(44 + bytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + bytes).put("WAVEfmt ".toByteArray()).putInt(16)
        buffer.putShort(1).putShort(1).putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort(2).putShort(16)
        buffer.put("data".toByteArray()).putInt(bytes)
        samples.forEach { buffer.putShort((it / peak * .62 * Short.MAX_VALUE).toInt().toShort()) }
        return buffer.array()
    }
    @JvmStatic fun main(args: Array<String>) {
        val directory = File(args.single()).also { require(it.isDirectory) }
        DrumCatalog.sounds.forEach { File(directory, "${it.audioIdentity}.wav").writeBytes(wav(it)) }
        println("Generated six original drum samples.")
    }
}
