package com.nahtygal.olivialooi.games.billiards

import java.io.File
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class BilliardsInvariantTest {
    @Test fun `opponent selects only active object balls`() {
        val table=BilliardsPhysics.rack().let { it.copy(balls=it.balls.map { b -> if(b.id==BallId.ONE) b.copy(pocketId=0) else b }) }
        repeat(100) { val shot=LooLooOpponent.plan(table,Random(it))!!; assertNotEquals(BallId.CUE,shot.ballId); assertNotEquals(BallId.ONE,shot.ballId) }
    }
    @Test fun `opponent pocket IDs are valid`() { repeat(100) { assertTrue(LooLooOpponent.plan(BilliardsPhysics.rack(),Random(it))!!.pocketId in 0..5) } }
    @Test fun `opponent power is finite and bounded`() { repeat(100) { assertTrue(LooLooOpponent.plan(BilliardsPhysics.rack(),Random(it))!!.power in BilliardsPhysics.MIN_SPEED..BilliardsPhysics.MAX_SPEED) } }
    @Test fun `opponent direction is normalized`() { repeat(100) { assertEquals(1.0,LooLooOpponent.plan(BilliardsPhysics.rack(),Random(it))!!.direction.length(),1e-9) } }
    @Test fun `opponent randomness is deterministic`() { assertEquals(LooLooOpponent.plan(BilliardsPhysics.rack(),Random(24)),LooLooOpponent.plan(BilliardsPhysics.rack(),Random(24))) }
    @Test fun `opponent varies friendly aim`() {
        val table=BilliardsPhysics.rack()
        val plans=(0..50).map { LooLooOpponent.plan(table,Random(it))!! }
        assertTrue(plans.toSet().size>20)
        assertTrue(plans.any { shot ->
            val ball=table.balls.first { it.id==shot.ballId }
            val pocket=table.pockets.first { it.id==shot.pocketId }
            (shot.direction-(pocket.position-ball.position).normalized()).length()>.05
        })
    }
    @Test fun `planning does not mutate positions velocity or ownership`() {
        val table=BilliardsPhysics.rack(); val snapshot=table.copy(balls=table.balls.toList())
        LooLooOpponent.plan(table,Random(1)); assertEquals(snapshot,table)
    }
    @Test fun `no active object means no AI shot`() { val t=BilliardsPhysics.rack(); assertNull(LooLooOpponent.plan(t.copy(balls=t.balls.map { if(it.id!=BallId.CUE) it.copy(pocketId=0) else it }))) }
    @Test fun `random maximum shots stay finite bounded and eventually stop`() {
        repeat(80) { seed ->
            val random=Random(seed)
            var table=BilliardsPhysics.launch(BilliardsPhysics.rack(),BallShot(BallId.entries.random(random),Vec2(random.nextDouble(-1.0,1.0),random.nextDouble(-1.0,1.0)),650.0))
            val captured=mutableSetOf<BallId>()
            repeat(600) {
                val result=BilliardsPhysics.advance(table,random.nextDouble(.01,.04)); table=result.table
                result.pocketEvents.forEach { assertTrue(captured.add(it.ballId)) }
                table.balls.forEach { b ->
                    assertTrue(b.position.x.isFinite() && b.position.y.isFinite())
                    assertTrue(b.velocity.length().isFinite())
                    if(!b.pocketed) { assertTrue(b.position.x in 24.0..576.0); assertTrue(b.position.y in 24.0..976.0) }
                }
            }
            assertFalse("Seed $seed",table.moving)
        }
    }
    @Test fun `random matches preserve unique ownership and saveable state`() {
        repeat(12) { seed ->
            val random=Random(seed)
            var s=BilliardsEngine.newGame(BilliardsMode.VERSUS,random)
            repeat(12) {
                if(!s.sessionComplete) {
                    s=if(s.turn==TurnOwner.LOOLOO) BilliardsEngine.prepareAi(s,s.turnIdentity,random).let { BilliardsEngine.fireAi(it,it.turnIdentity) }
                    else LooLooOpponent.plan(s.table,random)?.let { BilliardsEngine.shoot(s,it.ballShot()) } ?: s
                    repeat(600) { if(s.shotActive) s=BilliardsEngine.tick(s,.025) }
                    assertFalse(s.shotActive)
                    assertTrue(s.oliviaBalls.intersect(s.looLooBalls).isEmpty())
                    assertFalse(BallId.CUE in s.oliviaBalls+s.looLooBalls)
                    assertEquals(s.table.balls.filter { it.pocketed && it.id!=BallId.CUE }.map { it.id }.toSet(),s.oliviaBalls+s.looLooBalls)
                    assertEquals(s,BilliardsStateCodec.decode(BilliardsStateCodec.encode(s)))
                }
            }
        }
    }
    @Test fun `pure engine has no Android Compose network Jarvis or speech dependencies`() {
        val root=listOf(File("src/main/java"),File("app/src/main/java")).first { it.exists() }
        val files=File(root,"com/nahtygal/olivialooi/games/billiards").listFiles()!!.filter { it.extension=="kt" }
        assertTrue(files.size>=3)
        files.forEach { file ->
            val imports=file.readLines().filter { it.startsWith("import ") }
            assertTrue(file.name,imports.all { it.startsWith("import kotlin.") || it.startsWith("import java.lang.") })
            assertFalse(file.readText().contains("Jarvis"))
        }
    }
    @Test fun `guided and free play do not award stars`() {
        listOf(BilliardsMode.FREE_PLAY,BilliardsMode.TRY_POCKET).forEach { mode ->
            val s=BilliardsEngine.newGame(mode,Random(1)); assertTrue(s.oliviaBalls.isEmpty()); assertTrue(s.looLooBalls.isEmpty())
        }
        assertFalse(BilliardsState::class.java.declaredFields.any { it.name.contains("star",true) || it.name.contains("score",true) })
    }
}
