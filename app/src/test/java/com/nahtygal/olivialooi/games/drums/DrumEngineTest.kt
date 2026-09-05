package com.nahtygal.olivialooi.games.drums

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class DrumEngineTest {
    private fun fresh(seed: Int = 23) = DrumEngine.copyBeat(DrumState(), Random(seed))
    private fun ready(s: DrumState) = DrumEngine.finishReplay(s, s.replayIdentity)
    private fun complete(s: DrumState) = s.pattern.drop(s.position).fold(ready(s), DrumEngine::hit)
    @Test fun `six exact unique sounds and audio identities`() {
        assertEquals(listOf("kick", "snare", "tom", "clap", "tambourine", "cymbal"), DrumCatalog.sounds.map { it.stableId })
        assertEquals(6, DrumCatalog.sounds.map { it.audioIdentity }.toSet().size)
        DrumCatalog.sounds.forEach { assertTrue(it.displayName.isNotBlank()); assertTrue(it.accessibilityDescription.isNotBlank()); assertTrue(it.audioIdentity.isNotBlank()); assertEquals(it, DrumCatalog.findById(it.stableId)) }
        assertNull(DrumCatalog.findById("unknown"))
    }
    @Test fun `five rounds have exact progressive lengths`() {
        var s = fresh()
        repeat(5) { i ->
            assertEquals(i + 1, s.round)
            assertEquals(i + 2, s.pattern.size)
            s = complete(s)
            assertEquals(i + 1, s.completedRounds)
            assertEquals(i == 4, s.sessionComplete)
            if (i < 4) s = DrumEngine.nextBeat(s, Random(i))
        }
        assertSame(s, DrumEngine.nextBeat(s))
    }
    @Test fun `seeded patterns deterministic and varied`() {
        assertEquals(fresh(13), fresh(13))
        assertTrue((0..20).map { fresh(it).pattern }.toSet().size > 1)
    }
    @Test fun `all patterns valid and not all identical`() {
        repeat(300) { seed ->
            var s = fresh(seed)
            repeat(5) {
                assertTrue(s.pattern.all { it in DrumCatalog.sounds })
                assertTrue(s.pattern.distinct().size > 1)
                s = DrumEngine.nextBeat(complete(s), Random(seed + it))
            }
        }
    }
    @Test fun `repeated drums permitted`() {
        var s = fresh()
        repeat(4) { s = DrumEngine.nextBeat(complete(s), Random(it)) }
        assertTrue((0..100).any { seed ->
            var round = fresh(seed)
            repeat(4) { round = DrumEngine.nextBeat(complete(round), Random(seed + it)) }
            round.pattern.toSet().size < round.pattern.size
        })
    }
    @Test fun `selecting active Copy Beat mode does not regenerate pattern`() {
        val s = fresh()
        assertSame(s, DrumEngine.copyBeat(s))
        val partial = DrumEngine.hit(ready(s), s.pattern.first())
        assertSame(partial, DrumEngine.copyBeat(partial))
    }
    @Test fun `correct next hit advances`() {
        val s = ready(fresh())
        assertEquals(1, DrumEngine.hit(s, s.expected!!).position)
    }
    @Test fun `wrong hit resets only attempt on same pattern`() {
        val s = ready(fresh())
        val partial = DrumEngine.hit(s, s.expected!!)
        val wrong = DrumSound.entries.first { it != partial.expected }
        val retry = DrumEngine.hit(partial, wrong)
        assertEquals(0, retry.position)
        assertEquals(s.pattern, retry.pattern)
        assertEquals(0, retry.completedRounds)
        assertEquals(s.round, retry.round)
        assertFalse(retry.inputLocked)
    }
    @Test fun `completed pattern locks until Next Beat`() {
        val s = complete(fresh())
        assertEquals(BeatPhase.COMPLETE, s.phase)
        assertTrue(s.inputLocked)
        DrumSound.entries.forEach { assertSame(s, DrumEngine.hit(s, it)) }
        val next = DrumEngine.nextBeat(s)
        assertEquals(2, next.round)
        assertSame(next, DrumEngine.nextBeat(next))
    }
    @Test fun `session cannot advance after completion`() {
        var s = fresh()
        repeat(5) { s = complete(s); if (it < 4) s = DrumEngine.nextBeat(s) }
        DrumSound.entries.forEach { assertSame(s, DrumEngine.hit(s, it)) }
        assertSame(s, DrumEngine.replay(s))
        assertSame(s, DrumEngine.nextBeat(s))
        val again = DrumEngine.copyBeat(s)
        assertEquals(0, again.completedRounds)
        assertEquals(2, again.pattern.size)
        assertTrue(again.replayIdentity > s.replayIdentity)
    }
    @Test fun `replay preserves pattern and partial progress`() {
        val s = ready(fresh())
        val partial = DrumEngine.hit(s, s.expected!!)
        val replay = DrumEngine.replay(partial)
        assertTrue(replay.inputLocked)
        assertEquals(partial.position, replay.position)
        assertEquals(partial.completedRounds, replay.completedRounds)
        assertEquals(partial.pattern, replay.pattern)
        assertEquals(partial.copy(replayIdentity = replay.replayIdentity), ready(replay))
    }
    @Test fun `repeated replay requests and stale callbacks ignored`() {
        val first = fresh()
        assertSame(first, DrumEngine.replay(first))
        assertSame(first, DrumEngine.hit(first, first.pattern.first()))
        val second = DrumEngine.replay(ready(first))
        assertSame(second, DrumEngine.finishReplay(second, first.replayIdentity))
        val free = DrumEngine.freePlay(second)
        assertSame(free, DrumEngine.finishReplay(free, second.replayIdentity))
        val newSession = DrumEngine.copyBeat(free)
        assertSame(newSession, DrumEngine.finishReplay(newSession, second.replayIdentity))
    }
    @Test fun `interrupted replay invalidates its previous callback`() {
        val replay = fresh()
        val interrupted = DrumEngine.interruptReplay(replay)
        assertSame(interrupted, DrumEngine.finishReplay(interrupted, replay.replayIdentity))
        assertEquals(replay.pattern, interrupted.pattern)
        assertEquals(replay.position, interrupted.position)
        assertEquals(BeatPhase.YOUR_TURN, ready(interrupted).phase)
    }
    @Test fun `free play is stateless and both modes have no scoring fields`() {
        val s = DrumState()
        DrumSound.entries.forEach { assertSame(s, DrumEngine.hit(s, it)) }
        assertFalse(s.inputLocked)
        assertNull(s.expected)
        assertTrue(DrumState::class.java.declaredFields.none { it.name.lowercase() in setOf("stars", "points", "score", "penalty") })
    }
    @Test fun `state restoration preserves every phase and position`() {
        var s = fresh()
        repeat(5) {
            assertEquals(s, DrumStateCodec.decode(DrumStateCodec.encode(s)))
            s = ready(s)
            while (!s.inputLocked) {
                assertEquals(s, DrumStateCodec.decode(DrumStateCodec.encode(s)))
                s = DrumEngine.hit(s, s.expected!!)
            }
            assertEquals(s, DrumStateCodec.decode(DrumStateCodec.encode(s)))
            if (!s.sessionComplete) s = DrumEngine.nextBeat(s)
        }
        assertEquals(DrumState(), DrumStateCodec.decode(DrumStateCodec.encode(DrumState())))
    }
    @Test fun `malformed saved state fails safely`() {
        val f = DrumStateCodec.encode(fresh()).split('|')
        fun changed(i: Int, v: String) = f.toMutableList().also { it[i] = v }.joinToString("|")
        listOf("", "broken", changed(0,"9"), changed(2,"kick"), changed(3,"9"), changed(4,"-1"), changed(6,"-1")).forEach { assertNull(DrumStateCodec.decode(it)) }
    }
    @Test fun `random transitions maintain bounds`() {
        val random = Random(45)
        var s = fresh()
        repeat(3000) {
            s = when (random.nextInt(5)) {
                0 -> DrumEngine.replay(s)
                1 -> ready(s)
                2 -> DrumEngine.nextBeat(s, random)
                3 -> if (s.sessionComplete) DrumEngine.copyBeat(s, random) else DrumEngine.hit(s, s.expected ?: DrumSound.KICK)
                else -> DrumEngine.hit(s, DrumSound.entries.random(random))
            }
            assertTrue(s.completedRounds in 0..5)
            assertTrue(s.position in 0..s.pattern.size)
            assertEquals(s.round + 1, s.pattern.size)
            assertEquals(s, DrumStateCodec.decode(DrumStateCodec.encode(s)))
        }
    }
    @Test fun `pure model has only Kotlin dependencies`() {
        val relative = "src/main/java/com/nahtygal/olivialooi/games/drums"
        val root = java.io.File(relative).let { if (it.isDirectory) it else java.io.File("app", relative) }
        assertTrue(root.isDirectory)
        root.listFiles()!!.filter { it.extension == "kt" }.forEach { file ->
            file.readLines().filter { it.startsWith("import ") }.forEach { assertTrue(it, it.startsWith("import kotlin.")) }
        }
    }
}
