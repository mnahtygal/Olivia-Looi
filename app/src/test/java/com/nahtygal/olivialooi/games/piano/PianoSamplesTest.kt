package com.nahtygal.olivialooi.games.piano

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.*
import org.junit.Test

class PianoSamplesTest {
    private val directory = File("src/main/res/raw").let { if (it.isDirectory) it else File("app", it.path) }
    private fun bytes(note: PianoNote) = File(directory, "${note.audioIdentity}.wav").readBytes()
    private fun pcm(note: PianoNote): ShortArray {
        val data = bytes(note)
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(44)
        return ShortArray((data.size - 44) / 2) { buffer.short }
    }
    @Test fun `all eight required packaged resources exist without extras`() {
        assertEquals(PianoCatalog.notes.map { "${it.audioIdentity}.wav" }.toSet(), directory.listFiles()!!.filter { it.name.startsWith("piano_") }.map { it.name }.toSet())
    }
    @Test fun `packaged WAV headers are mono 16 bit PCM at 44100 Hz`() {
        PianoCatalog.notes.forEach { note ->
            val data = bytes(note)
            val header = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals("RIFF", data.copyOfRange(0, 4).toString(Charsets.US_ASCII))
            assertEquals("WAVE", data.copyOfRange(8, 12).toString(Charsets.US_ASCII))
            assertEquals(1, header.getShort(20).toInt())
            assertEquals(1, header.getShort(22).toInt())
            assertEquals(44100, header.getInt(24))
            assertEquals(16, header.getShort(34).toInt())
            assertEquals(data.size - 44, header.getInt(40))
            assertEquals(132344, data.size)
        }
    }
    @Test fun `samples are non silent and have headroom with quiet endpoints`() {
        PianoCatalog.notes.forEach { note ->
            val samples = pcm(note)
            val peak = samples.maxOf { abs(it.toInt()) }
            assertTrue(peak in 5000..24000)
            assertEquals(0, samples.first().toInt())
            assertTrue(abs(samples.last().toInt()) <= 1)
            assertTrue(samples.takeLast(1000).maxOf { abs(it.toInt()) } < 100)
        }
    }
    @Test fun `assets reproduce exactly from original offline Kotlin generator`() {
        PianoCatalog.notes.forEach { assertArrayEquals(bytes(it), PianoSampleGenerator.wav(it)) }
    }
    @Test fun `sample fundamental matches declared note rather than neighboring pitch`() {
        fun energy(samples: ShortArray, frequency: Double): Double {
            var real = 0.0
            var imaginary = 0.0
            for (frame in 4410 until 13230) {
                val phase = 2 * PI * frequency * frame / 44100
                real += samples[frame] * cos(phase)
                imaginary += samples[frame] * sin(phase)
            }
            return real * real + imaginary * imaginary
        }
        PianoCatalog.notes.forEach { note ->
            val samples = pcm(note)
            val target = energy(samples, note.frequencyHz)
            PianoCatalog.notes.filter { it != note }.forEach { other ->
                assertTrue("${note.stableId} vs ${other.stableId}", target > energy(samples, other.frequencyHz) * 3)
            }
        }
    }
}
