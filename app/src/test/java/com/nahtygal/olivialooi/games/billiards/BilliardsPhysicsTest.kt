package com.nahtygal.olivialooi.games.billiards

import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class BilliardsPhysicsTest {
    private fun ball(x: Double = 300.0, y: Double = 500.0, vx: Double = 0.0, vy: Double = 0.0, id: BallId = BallId.CUE) = Ball(id, Vec2(x,y), Vec2(vx,vy))
    private fun step(ball: Ball, dt: Double = .05) = BilliardsPhysics.advance(TableState(listOf(ball)), dt).table.balls.single()
    @Test fun `rack has ten balls including cue and unique one through nine`() {
        val table = BilliardsPhysics.rack()
        assertEquals(10, table.balls.size)
        assertEquals((0..9).toList(), table.balls.map { it.id.number })
        assertEquals(10, table.balls.map { it.id }.toSet().size)
        assertEquals("white", BallId.CUE.visualIdentity)
    }
    @Test fun `six distinct pockets`() {
        assertEquals(6, BilliardsPhysics.pockets.size)
        assertEquals(6, BilliardsPhysics.pockets.map { it.position }.toSet().size)
        assertEquals((0..5).toList(), BilliardsPhysics.pockets.map { it.id })
    }
    @Test fun `initial positions inside bounds`() {
        BilliardsPhysics.rack().balls.forEach { assertTrue(it.position.x in 24.0..576.0); assertTrue(it.position.y in 24.0..976.0) }
    }
    @Test fun `rack never overlaps`() {
        val balls = BilliardsPhysics.rack().balls
        for (a in balls) for (b in balls) if (a.id != b.id) assertTrue((a.position-b.position).length() >= a.radius+b.radius)
    }
    @Test fun `stationary ball stays still`() { val b = ball(); assertEquals(b, step(b)) }
    @Test fun `velocity rolls position`() { assertTrue(step(ball(vx=100.0)).position.x > 300.0) }
    @Test fun `friction reduces speed`() { assertTrue(step(ball(vx=100.0)).velocity.length() < 100.0) }
    @Test fun `friction never reverses direction`() {
        var b = ball(vx=10.0,vy=-10.0)
        repeat(100) { b=step(b); assertTrue(b.velocity.x >= 0); assertTrue(b.velocity.y <= 0) }
        assertEquals(Vec2.ZERO,b.velocity)
    }
    @Test fun `tiny speed stops`() { assertEquals(Vec2.ZERO,step(ball(vx=1.0)).velocity) }
    @Test fun `large delta is clamped deterministically`() {
        val table=TableState(listOf(ball(vx=600.0)))
        assertEquals(BilliardsPhysics.advance(table,.1),BilliardsPhysics.advance(table,100.0))
    }
    @Test fun `invalid elapsed safely ignored`() {
        val table=BilliardsPhysics.rack()
        listOf(Double.NaN,Double.POSITIVE_INFINITY,-1.0,0.0).forEach { assertSame(table,BilliardsPhysics.advance(table,it).table) }
    }
    @Test fun `left rail reverses approaching x`() { assertTrue(step(ball(x=25.0,y=300.0,vx=-100.0)).velocity.x > 0) }
    @Test fun `right rail reverses approaching x`() { assertTrue(step(ball(x=575.0,y=300.0,vx=100.0)).velocity.x < 0) }
    @Test fun `top rail reverses approaching y`() { assertTrue(step(ball(x=300.0,y=25.0,vy=-100.0)).velocity.y > 0) }
    @Test fun `bottom rail reverses approaching y`() { assertTrue(step(ball(x=300.0,y=975.0,vy=100.0)).velocity.y < 0) }
    @Test fun `rail does not reverse an already departing ball`() { assertTrue(step(ball(x=24.0,y=300.0,vx=100.0)).velocity.x > 0) }
    @Test fun `repeated rail bounces remain stable and bounded`() {
        var b=ball(x=300.0,y=300.0,vx=650.0)
        repeat(2000) { b=step(b); if (!b.pocketed) { assertTrue(b.position.x in 24.0..576.0); assertTrue(b.position.y in 24.0..976.0) } }
        assertEquals(Vec2.ZERO,b.velocity)
    }
    @Test fun `head on collision transfers momentum`() {
        val a=ball(x=200.0,vx=100.0)
        val b=ball(x=248.0,id=BallId.ONE)
        val result=BilliardsPhysics.collide(a,b)
        assertTrue(result.first.velocity.x < 5)
        assertTrue(result.second.velocity.x > 95)
    }
    @Test fun `head on collision approximately conserves total momentum`() {
        val result=BilliardsPhysics.collide(ball(x=200.0,vx=100.0),ball(x=248.0,id=BallId.ONE))
        assertEquals(100.0,result.first.velocity.x+result.second.velocity.x,1e-9)
    }
    @Test fun `glancing collision changes both directions`() {
        val result=BilliardsPhysics.collide(ball(x=200.0,y=200.0,vx=150.0),ball(x=235.0,y=230.0,id=BallId.ONE))
        assertTrue(abs(result.first.velocity.y)>1)
        assertTrue(result.second.velocity.x>1)
        assertTrue(result.second.velocity.y>1)
    }
    @Test fun `overlap corrected`() {
        val result=BilliardsPhysics.collide(ball(x=200.0),ball(x=220.0,id=BallId.ONE))
        assertTrue((result.first.position-result.second.position).length()>=48)
    }
    @Test fun `coincident centers resolve without NaN`() {
        val result=BilliardsPhysics.collide(ball(),ball(id=BallId.ONE))
        assertTrue((result.first.position-result.second.position).length()>=48)
        assertTrue(result.first.position.x.isFinite())
    }
    @Test fun `separating overlap does not create an extra impulse`() {
        val a=ball(x=200.0,vx=-100.0)
        val b=ball(x=230.0,vx=100.0,id=BallId.ONE)
        val result=BilliardsPhysics.collide(a,b)
        assertEquals(a.velocity,result.first.velocity)
        assertEquals(b.velocity,result.second.velocity)
    }
    @Test fun `non overlapping balls unaffected`() {
        val a=ball(x=100.0,vx=100.0); val b=ball(x=300.0,id=BallId.ONE)
        assertEquals(a to b,BilliardsPhysics.collide(a,b))
    }
    @Test fun `normal maximum velocity does not tunnel through stationary ball`() {
        var table=TableState(listOf(ball(x=120.0,y=300.0,vx=650.0),ball(x=200.0,y=300.0,id=BallId.ONE)))
        table=BilliardsPhysics.advance(table,.1).table
        assertTrue(table.balls[1].velocity.x>500)
        assertTrue(table.balls[0].position.x<table.balls[1].position.x)
    }
    @Test fun `all pocket capture zones work`() {
        BilliardsPhysics.pockets.forEach { pocket ->
            val position=Vec2(pocket.position.x.coerceIn(24.0,576.0),pocket.position.y.coerceIn(24.0,976.0))
            val result=BilliardsPhysics.advance(TableState(listOf(Ball(BallId.ONE,position))),.01)
            assertEquals(pocket.id,result.table.balls.single().pocketId)
            assertEquals(listOf(PocketEvent(BallId.ONE,pocket.id)),result.pocketEvents)
        }
    }
    @Test fun `pocketed ball excluded from collisions`() {
        val a=ball().copy(pocketId=4); val b=ball(id=BallId.ONE)
        assertEquals(a to b,BilliardsPhysics.collide(a,b))
    }
    @Test fun `pocket capture only reported once`() {
        val initial=TableState(listOf(ball(x=24.0)))
        val first=BilliardsPhysics.advance(initial,.01)
        assertEquals(1,first.pocketEvents.size)
        assertTrue(BilliardsPhysics.advance(first.table,.01).pocketEvents.isEmpty())
    }
    @Test fun `cue capture detected without special foul behavior`() {
        assertEquals(4,step(ball(x=24.0)).pocketId)
    }
    @Test fun `cue restore avoids occupied starting point`() {
        val rack=BilliardsPhysics.rack()
        val table=rack.copy(balls=rack.balls.map {
            when(it.id) { BallId.CUE -> it.copy(pocketId=4); BallId.ONE -> it.copy(position=Vec2(300.0,780.0)); else -> it }
        })
        val restored=BilliardsPhysics.restore(table,BallId.CUE)
        val cue=restored.balls.first { it.id==BallId.CUE }
        assertFalse(cue.pocketed)
        assertEquals(Vec2.ZERO,cue.velocity)
        restored.balls.filter { it.id!=BallId.CUE }.forEach { assertTrue((it.position-cue.position).length()>=48) }
    }
    @Test fun `restore active ball is a no op`() { val table=BilliardsPhysics.rack(); assertSame(table,BilliardsPhysics.restore(table,BallId.ONE)) }
    @Test fun `drag controls direction and distance controls power`() {
        val small=BilliardsPhysics.shotFromDrag(BallId.ONE,Vec2(30.0,40.0))!!
        val large=BilliardsPhysics.shotFromDrag(BallId.ONE,Vec2(60.0,80.0))!!
        assertEquals(Vec2(.6,.8),small.direction)
        assertTrue(large.power>small.power)
    }
    @Test fun `shot power clamps minimum and maximum`() {
        assertEquals(BilliardsPhysics.MIN_SPEED,BilliardsPhysics.shotFromDrag(BallId.ONE,Vec2(5.0,0.0))!!.power,0.0)
        assertEquals(BilliardsPhysics.MAX_SPEED,BilliardsPhysics.shotFromDrag(BallId.ONE,Vec2(10000.0,0.0))!!.power,0.0)
    }
    @Test fun `near zero shot is safe`() { assertNull(BilliardsPhysics.shotFromDrag(BallId.CUE,Vec2(1.0,1.0))) }
    @Test fun `cannot launch while moving`() {
        val shot=BallShot(BallId.CUE,Vec2(0.0,-1.0),300.0)
        val moving=BilliardsPhysics.launch(BilliardsPhysics.rack(),shot)
        assertSame(moving,BilliardsPhysics.launch(moving,shot))
    }
    @Test fun `can launch stopped ball`() { assertTrue(BilliardsPhysics.launch(BilliardsPhysics.rack(),BallShot(BallId.CUE,Vec2(1.0,0.0),300.0)).moving) }
    @Test fun `invalid shot leaves table alone`() {
        val table=BilliardsPhysics.rack()
        assertSame(table,BilliardsPhysics.launch(table,BallShot(BallId.ONE,Vec2.ZERO,100.0)))
        assertSame(table,BilliardsPhysics.launch(table,BallShot(BallId.ONE,Vec2(1.0,0.0),Double.NaN)))
    }
    @Test fun `pocketed ball cannot launch`() {
        val table=TableState(listOf(ball().copy(pocketId=4)))
        assertSame(table,BilliardsPhysics.launch(table,BallShot(BallId.CUE,Vec2(1.0,0.0),200.0)))
    }
    @Test fun `fixed substeps deterministic across standard frame sizes`() {
        val table=BilliardsPhysics.launch(BilliardsPhysics.rack(),BallShot(BallId.CUE,Vec2(0.0,-1.0),300.0))
        val one=BilliardsPhysics.advance(table,1.0/30).table
        val two=BilliardsPhysics.advance(BilliardsPhysics.advance(table,1.0/60).table,1.0/60).table
        assertEquals(one,two)
    }
}
