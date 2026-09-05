package com.nahtygal.olivialooi.games.billiards

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class BilliardsEngineTest {
    private fun fresh(mode: BilliardsMode = BilliardsMode.VERSUS, seed: Int = 24) = BilliardsEngine.newGame(mode,Random(seed))
    private val shot=BallShot(BallId.CUE,Vec2(1.0,0.0),90.0)
    private fun stopped(input: BilliardsState): BilliardsState {
        var state=input
        repeat(2000) { if (state.shotActive) state=BilliardsEngine.tick(state,1.0/60) }
        assertFalse(state.shotActive)
        return state
    }
    /** Capture fixtures still go through the production physics capture/settle path. */
    private fun capture(state: BilliardsState, ids: Set<BallId>, pocketId: Int = 4): BilliardsState {
        val pocket=state.table.pockets.first { it.id==pocketId }
        val p=Vec2(pocket.position.x.coerceIn(24.0,576.0),pocket.position.y.coerceIn(24.0,976.0))
        return BilliardsEngine.tick(state.copy(shotActive=true,table=state.table.copy(balls=state.table.balls.map {
            if (it.id in ids) it.copy(position=p,velocity=Vec2.ZERO,pocketId=null) else it.copy(velocity=Vec2.ZERO)
        })),.01)
    }
    @Test fun `fresh match begins with Olivia`() { assertEquals(TurnOwner.OLIVIA,fresh().turn); assertTrue(fresh().childCanShoot) }
    @Test fun `Olivia shot accepted and locks input`() { val s=BilliardsEngine.shoot(fresh(),shot); assertTrue(s.shotActive); assertFalse(s.childCanShoot) }
    @Test fun `child cannot shoot during LooLoo turn`() { val s=fresh().copy(turn=TurnOwner.LOOLOO); assertSame(s,BilliardsEngine.shoot(s,shot)) }
    @Test fun `moving table cannot accept second shot`() { val s=BilliardsEngine.shoot(fresh(),shot); assertSame(s,BilliardsEngine.shoot(s,shot)) }
    @Test fun `turn switches exactly once at rest`() {
        val s=stopped(BilliardsEngine.shoot(fresh(),shot))
        assertEquals(TurnOwner.LOOLOO,s.turn)
        val id=s.turnIdentity
        assertSame(s,BilliardsEngine.tick(s,.1))
        assertEquals(id,s.turnIdentity)
    }
    @Test fun `object capture credits current player`() { assertEquals(setOf(BallId.ONE),capture(fresh(),setOf(BallId.ONE)).oliviaBalls) }
    @Test fun `collision pockets also credited to LooLoo`() { assertEquals(setOf(BallId.ONE,BallId.TWO),capture(fresh().copy(turn=TurnOwner.LOOLOO),setOf(BallId.ONE,BallId.TWO)).looLooBalls) }
    @Test fun `object ownership credited only once`() {
        val s=capture(fresh(),setOf(BallId.ONE))
        val duplicate=capture(s,setOf(BallId.ONE))
        assertEquals(setOf(BallId.ONE),duplicate.oliviaBalls)
        assertTrue(duplicate.looLooBalls.isEmpty())
    }
    @Test fun `cue does not count and returns safely`() {
        val s=capture(fresh(),setOf(BallId.CUE))
        assertTrue(s.oliviaBalls.isEmpty()); assertTrue(s.looLooBalls.isEmpty())
        assertFalse(s.table.balls.first { it.id==BallId.CUE }.pocketed)
    }
    @Test fun `multiple simultaneous pockets credited correctly`() { assertEquals(3,capture(fresh(),setOf(BallId.ONE,BallId.TWO,BallId.THREE)).oliviaBalls.size) }
    @Test fun `AI plan prepared once and fires once`() {
        val turn=fresh().copy(turn=TurnOwner.LOOLOO)
        val plan=BilliardsEngine.prepareAi(turn,turn.turnIdentity,Random(12))
        assertNotNull(plan.pendingAi)
        assertSame(plan,BilliardsEngine.prepareAi(plan,plan.turnIdentity,Random(99)))
        val moving=BilliardsEngine.fireAi(plan,plan.turnIdentity)
        assertTrue(moving.shotActive)
        assertSame(moving,BilliardsEngine.fireAi(moving,plan.turnIdentity))
    }
    @Test fun `stale AI identity cannot prepare or fire`() {
        val turn=fresh().copy(turn=TurnOwner.LOOLOO,turnIdentity=10)
        assertSame(turn,BilliardsEngine.prepareAi(turn,9))
        val plan=BilliardsEngine.prepareAi(turn,10)
        assertSame(plan,BilliardsEngine.fireAi(plan,9))
        val reset=BilliardsEngine.rack(plan)
        assertSame(reset,BilliardsEngine.fireAi(reset,10))
    }
    @Test fun `AI uses identical normal launch physics`() {
        val s=fresh().copy(turn=TurnOwner.LOOLOO)
        val plan=BilliardsEngine.prepareAi(s,s.turnIdentity,Random(42))
        assertEquals(BilliardsPhysics.launch(s.table,plan.pendingAi!!.ballShot()),BilliardsEngine.fireAi(plan,plan.turnIdentity).table)
    }
    @Test fun `AI turn returns to Olivia after rolling`() {
        val turn=fresh().copy(turn=TurnOwner.LOOLOO)
        val plan=BilliardsEngine.prepareAi(turn,turn.turnIdentity,Random(7))
        val done=stopped(BilliardsEngine.fireAi(plan,plan.turnIdentity))
        assertEquals(TurnOwner.OLIVIA,done.turn)
        assertTrue(done.childCanShoot)
    }
    @Test fun `fifteen objects finish match and prevent further shots`() {
        val done=capture(fresh(),BallId.entries.filter { it!=BallId.CUE }.toSet())
        assertTrue(done.sessionComplete)
        assertEquals(15,done.oliviaBalls.size)
        assertFalse(done.childCanShoot)
        assertSame(done,BilliardsEngine.shoot(done,shot))
        assertSame(done,BilliardsEngine.tick(done,.1))
        assertSame(done,BilliardsEngine.prepareAi(done,done.turnIdentity))
    }
    @Test fun `winner helpers cover both winners and tie`() {
        assertEquals(MatchResult.OLIVIA_WINS,BilliardsEngine.matchResult(5,4))
        assertEquals(MatchResult.LOOLOO_WINS,BilliardsEngine.matchResult(4,5))
        assertEquals(MatchResult.TIE,BilliardsEngine.matchResult(4,4))
    }
    @Test fun `rack resets balls ownership motion and turn`() {
        val reset=BilliardsEngine.rack(capture(fresh(),setOf(BallId.ONE,BallId.CUE)))
        assertEquals(BilliardsPhysics.rack(),reset.table)
        assertEquals(TurnOwner.OLIVIA,reset.turn)
        assertTrue(reset.oliviaBalls.isEmpty()); assertTrue(reset.looLooBalls.isEmpty())
        assertFalse(reset.shotActive); assertNull(reset.pendingAi); assertNull(reset.selectedBall)
    }
    @Test fun `rack preserves aim setting and invalidates old turn`() {
        val initial=fresh().copy(aimGuide=false)
        val reset=BilliardsEngine.rack(initial)
        assertFalse(reset.aimGuide)
        assertTrue(reset.turnIdentity>initial.turnIdentity)
    }
    @Test fun `free play permits direct rolls of all active balls`() {
        BallId.entries.forEach { assertTrue(BilliardsEngine.shoot(fresh(BilliardsMode.FREE_PLAY),shot.copy(ballId=it)).shotActive) }
    }
    @Test fun `free play keeps no ownership or turns`() {
        val s=capture(fresh(BilliardsMode.FREE_PLAY),setOf(BallId.ONE))
        assertTrue(s.oliviaBalls.isEmpty()); assertTrue(s.looLooBalls.isEmpty())
        assertEquals(TurnOwner.OLIVIA,s.turn); assertFalse(s.sessionComplete)
    }
    @Test fun `target ball and pocket valid`() {
        repeat(100) { seed ->
            val s=fresh(BilliardsMode.TRY_POCKET,seed)
            assertNotEquals(BallId.CUE,s.target!!.ballId)
            assertTrue(s.target.pocketId in 0..5)
            assertFalse(s.table.balls.first { it.id==s.target.ballId }.pocketed)
        }
    }
    @Test fun `guided selection only allows target ball`() {
        val s=fresh(BilliardsMode.TRY_POCKET)
        assertSame(s,BilliardsEngine.shoot(s,shot))
        assertSame(s,BilliardsEngine.selectBall(s,BallId.CUE))
        assertEquals(s.target!!.ballId,BilliardsEngine.selectBall(s,s.target.ballId).selectedBall)
    }
    @Test fun `target in requested pocket completes once`() {
        val s=fresh(BilliardsMode.TRY_POCKET)
        val done=capture(s,setOf(s.target!!.ballId),s.target.pocketId)
        assertTrue(done.roundComplete); assertEquals(1,done.completedRounds)
        assertSame(done,BilliardsEngine.tick(done,.1))
        assertSame(done,BilliardsEngine.shoot(done,shot.copy(ballId=done.target!!.ballId)))
    }
    @Test fun `wrong pocket preserves round and restores target`() {
        val s=fresh(BilliardsMode.TRY_POCKET)
        val done=capture(s,setOf(s.target!!.ballId),(s.target.pocketId+1)%6)
        assertFalse(done.roundComplete); assertEquals(0,done.completedRounds)
        assertEquals(s.target,done.target)
        assertFalse(done.table.balls.first { it.id==s.target.ballId }.pocketed)
        assertEquals(ShotOutcome.WRONG_POCKET,done.outcome)
    }
    @Test fun `other pocketed ball remains pocketed without completing target`() {
        val s=fresh(BilliardsMode.TRY_POCKET)
        val other=BallId.entries.first { it!=BallId.CUE && it!=s.target!!.ballId }
        val done=capture(s,setOf(other),s.target!!.pocketId)
        assertFalse(done.roundComplete); assertEquals(0,done.completedRounds)
        assertTrue(done.table.balls.first { it.id==other }.pocketed)
    }
    @Test fun `five successes complete guided session`() {
        var s=fresh(BilliardsMode.TRY_POCKET)
        repeat(5) { i ->
            assertEquals(i+1,s.round)
            s=capture(s,setOf(s.target!!.ballId),s.target!!.pocketId)
            assertEquals(i+1,s.completedRounds)
            assertEquals(i==4,s.sessionComplete)
            if (i<4) s=BilliardsEngine.nextTarget(s,Random(i))
        }
        assertSame(s,BilliardsEngine.nextTarget(s))
        val reset=BilliardsEngine.rack(s)
        assertEquals(0,reset.completedRounds); assertFalse(reset.sessionComplete)
    }
    @Test fun `next target cannot skip incomplete or double advance`() {
        val s=fresh(BilliardsMode.TRY_POCKET)
        assertSame(s,BilliardsEngine.nextTarget(s))
        val done=capture(s,setOf(s.target!!.ballId),s.target.pocketId)
        val next=BilliardsEngine.nextTarget(done)
        assertSame(next,BilliardsEngine.nextTarget(next))
    }
    @Test fun `guided direct shot is achievable through same physics`() {
        repeat(20) { seed ->
            val s=fresh(BilliardsMode.TRY_POCKET,seed)
            val ball=s.table.balls.first { it.id==s.target!!.ballId }
            val pocket=s.table.pockets.first { it.id==s.target!!.pocketId }
            val delta=pocket.position-ball.position
            val power=kotlin.math.sqrt(2*BilliardsPhysics.DECELERATION*(delta.length()+35))
            val done=stopped(BilliardsEngine.shoot(s,BallShot(ball.id,delta.normalized(),power)))
            assertTrue("Seed $seed",done.roundComplete)
        }
    }
    @Test fun `save restore live rolling positions velocities and identity`() {
        val s=BilliardsEngine.tick(BilliardsEngine.shoot(fresh(),shot),.03)
        assertEquals(s,BilliardsStateCodec.decode(BilliardsStateCodec.encode(s)))
    }
    @Test fun `restored pending AI does not replan or duplicate shot`() {
        val s=fresh().copy(turn=TurnOwner.LOOLOO)
        val plan=BilliardsEngine.prepareAi(s,s.turnIdentity,Random(1))
        val restored=BilliardsStateCodec.decode(BilliardsStateCodec.encode(plan))!!
        assertEquals(plan,restored)
        assertSame(restored,BilliardsEngine.prepareAi(restored,restored.turnIdentity,Random(2)))
        val moving=BilliardsEngine.fireAi(restored,restored.turnIdentity)
        assertSame(moving,BilliardsEngine.fireAi(moving,restored.turnIdentity))
    }
    @Test fun `save restore ownership and completion`() {
        val done=capture(fresh(),BallId.entries.filter { it!=BallId.CUE }.toSet())
        assertEquals(done,BilliardsStateCodec.decode(BilliardsStateCodec.encode(done)))
    }
    @Test fun `save restore guided target and progress`() {
        val s=fresh(BilliardsMode.TRY_POCKET)
        val done=capture(s,setOf(s.target!!.ballId),s.target.pocketId)
        assertEquals(done,BilliardsStateCodec.decode(BilliardsStateCodec.encode(done)))
    }
    @Test fun `invalid payload fails safely`() {
        assertNull(BilliardsStateCodec.decode("")); assertNull(BilliardsStateCodec.decode("bad|data"))
        val f=BilliardsStateCodec.encode(fresh()).split('|').toMutableList()
        f[3]="0"
        assertNull(BilliardsStateCodec.decode(f.joinToString("|")))
    }
}
