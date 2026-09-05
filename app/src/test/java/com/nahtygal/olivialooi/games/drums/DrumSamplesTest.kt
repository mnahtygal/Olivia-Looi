package com.nahtygal.olivialooi.games.drums

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class DrumSamplesTest {
    private val directory = File("src/main/res/raw").let { if (it.isDirectory) it else File("app", it.path) }
    private fun bytes(sound: DrumSound) = File(directory, "${sound.audioIdentity}.wav").readBytes()
    private fun pcm(sound: DrumSound): ShortArray {
        val data = bytes(sound)
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).also { it.position(44) }
        return ShortArray((data.size - 44) / 2) { buffer.short }
    }
    @Test fun `exactly six packaged drum resources`() {
        assertEquals(DrumCatalog.sounds.map { "${it.audioIdentity}.wav" }.toSet(), directory.listFiles()!!.filter { it.name.startsWith("drum_") }.map { it.name }.toSet())
    }
    @Test fun `short mono PCM WAVs have valid lengths`() {
        DrumCatalog.sounds.forEach {
            val data = bytes(it)
            val header = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals("RIFF", data.copyOfRange(0,4).toString(Charsets.US_ASCII))
            assertEquals("WAVE", data.copyOfRange(8,12).toString(Charsets.US_ASCII))
            assertEquals(1, header.getShort(20).toInt())
            assertEquals(1, header.getShort(22).toInt())
            assertEquals(44100, header.getInt(24))
            assertEquals(16, header.getShort(34).toInt())
            assertEquals(data.size - 44, header.getInt(40))
            assertTrue((data.size - 44) / 88200.0 in .2..1.11)
        }
    }
    @Test fun `non silent samples have headroom and quiet endpoints`() {
        DrumCatalog.sounds.forEach {
            val samples = pcm(it)
            assertTrue(samples.maxOf { abs(it.toInt()) } in 19000..21000)
            assertEquals(0, samples.first().toInt())
            assertTrue(abs(samples.last().toInt()) <= 3)
        }
    }
    @Test fun `samples match original deterministic generator`() {
        DrumCatalog.sounds.forEach { assertArrayEquals(bytes(it), DrumSampleGenerator.wav(it)) }
    }
    @Test fun `all six samples are distinct`() {
        assertEquals(6, DrumCatalog.sounds.map { java.security.MessageDigest.getInstance("SHA-256").digest(bytes(it)).toList() }.toSet().size)
    }
    private fun crossings(sound: DrumSound): Int {
        val data = pcm(sound)
        return (4411 until 8820).count { data[it - 1] < 0 && data[it] >= 0 }
    }
    @Test fun `kick has lower resonance than tom and noise instruments`() {
        assertTrue(crossings(DrumSound.KICK) < crossings(DrumSound.TOM))
        assertTrue(crossings(DrumSound.TOM) < crossings(DrumSound.SNARE))
        assertTrue(crossings(DrumSound.TOM) < crossings(DrumSound.TAMBOURINE))
    }
    @Test fun `cymbal has longer decay than sharp snare and clap`() {
        assertTrue(pcm(DrumSound.CYMBAL).size > pcm(DrumSound.SNARE).size * 3)
        assertTrue(pcm(DrumSound.CYMBAL).size > pcm(DrumSound.CLAP).size * 3)
        assertTrue(pcm(DrumSound.CYMBAL).sliceArray(22050..26460).any { abs(it.toInt()) > 100 })
    }
}
