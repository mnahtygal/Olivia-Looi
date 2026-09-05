package com.nahtygal.olivialooi.games.billiards

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class BilliardsPolishTest {
    private val objects = BallId.entries.filter { it != BallId.CUE }

    private fun capture(state: BilliardsState, ids: Set<BallId>): BilliardsState =
        BilliardsEngine.tick(state.copy(shotActive = true, table = state.table.copy(balls = state.table.balls.map {
            if (it.id in ids) it.copy(position = Vec2(24.0, 500.0), velocity = Vec2.ZERO) else it
        })), .01)

    @Test fun `rack has five centered triangular rows and fifteen active objects`() {
        val balls = BilliardsPhysics.rack().balls
        val rows = balls.filter { it.id != BallId.CUE }.groupBy { it.position.y }.toSortedMap()
        assertEquals(listOf(1, 2, 3, 4, 5), rows.values.map { it.size })
        rows.values.forEach { row -> assertEquals(300.0, row.map { it.position.x }.average(), 0.0) }
        assertEquals((1..15).toList(), objects.map { it.number })
        assertEquals(0, BallId.CUE.number)
        assertTrue(balls.all { !it.pocketed && it.velocity == Vec2.ZERO })
    }

    @Test fun `nine pockets do not finish but fifteen do and ownership stays unique`() {
        val start = BilliardsEngine.newGame(BilliardsMode.VERSUS)
        val nine = capture(start, objects.take(9).toSet())
        assertFalse(nine.sessionComplete)
        assertEquals(9, nine.oliviaBalls.size)
        val done = capture(nine, objects.drop(9).toSet())
        assertTrue(done.sessionComplete)
        assertEquals(objects.toSet(), done.oliviaBalls + done.looLooBalls)
        assertTrue(done.oliviaBalls.intersect(done.looLooBalls).isEmpty())
        assertEquals(15, done.oliviaBalls.size + done.looLooBalls.size)
        assertSame(done, BilliardsEngine.tick(done, .1))
        assertEquals(done, BilliardsStateCodec.decode(BilliardsStateCodec.encode(done)))
        val reset = BilliardsEngine.rack(done)
        assertEquals(16, reset.table.balls.size)
        assertTrue(reset.table.balls.none { it.pocketed })
        assertTrue(reset.oliviaBalls.isEmpty() && reset.looLooBalls.isEmpty())
    }

    @Test fun `planner can select each of the fifteen objects without inverting its forward vector`() {
        objects.forEach { id ->
            val table = BilliardsPhysics.rack().let { t -> t.copy(balls = t.balls.map {
                if (it.id != id && it.id != BallId.CUE) it.copy(pocketId = 0) else it
            }) }
            val plan = LooLooOpponent.plan(table, Random(id.number))!!
            assertEquals(id, plan.ballId)
            assertEquals(plan.direction, plan.ballShot().direction)
            val ball = table.balls.first { it.id == id }
            val pocket = table.pockets.first { it.id == plan.pocketId }
            assertTrue(plan.direction.dot((pocket.position - ball.position).normalized()) > .98)
            assertTrue(plan.power in BilliardsPhysics.MIN_SPEED..BilliardsPhysics.MAX_SPEED)
            val launched = BilliardsPhysics.launch(table, plan.ballShot())
            assertEquals(plan.direction.x * plan.power, launched.balls.first { it.id == id }.velocity.x, 1e-9)
            val state = BilliardsState(BilliardsMode.VERSUS, table, turn = TurnOwner.LOOLOO, pendingAi = plan,
                oliviaBalls = objects.filter { it != id }.toSet(), aimGuide = false, turnIdentity = 42)
            assertEquals(state, BilliardsStateCodec.decode(BilliardsStateCodec.encode(state)))
        }
    }

    @Test fun `guided targets above nine round trip and retain correct and wrong pocket behavior`() {
        val states = (0..200).map { BilliardsEngine.newGame(BilliardsMode.TRY_POCKET, Random(it)) }
        val higher = states.filter { it.target!!.ballId.number > 9 }
        assertEquals((10..15).toSet(), higher.map { it.target!!.ballId.number }.toSet())
        higher.take(20).forEach { state ->
            assertEquals(state, BilliardsStateCodec.decode(BilliardsStateCodec.encode(state)))
            val target = state.target!!
            fun into(pocketId: Int): BilliardsState {
                val pocket = state.table.pockets.first { it.id == pocketId }
                return BilliardsEngine.tick(state.copy(shotActive = true, table = state.table.copy(balls = state.table.balls.map {
                    if (it.id == target.ballId) it.copy(position = Vec2(pocket.position.x.coerceIn(24.0, 576.0), pocket.position.y.coerceIn(24.0, 976.0))) else it
                })), .01)
            }
            assertTrue(into(target.pocketId).roundComplete)
            val wrong = into((target.pocketId + 1) % 6)
            assertFalse(wrong.roundComplete)
            assertFalse(wrong.table.balls.first { it.id == target.ballId }.pocketed)
        }
    }

    @Test fun `codec rejects previous version missing balls duplicate IDs and cue ownership`() {
        val fields = BilliardsStateCodec.encode(BilliardsEngine.newGame(BilliardsMode.FREE_PLAY)).split('|')
        assertEquals("2", fields[0])
        fun invalid(change: (MutableList<String>) -> Unit) {
            val f = fields.toMutableList(); change(f)
            assertNull(BilliardsStateCodec.decode(f.joinToString("|")))
        }
        invalid { it[0] = "1" }
        invalid { it[16] = it[16].split(';').take(10).joinToString(";") }
        invalid { val balls = it[16].split(';').toMutableList(); balls[15] = balls[14]; it[16] = balls.joinToString(";") }
        invalid { it[3] = "0" }
    }

    private fun opposite(pull: Vec2, expected: Vec2) {
        val shot = BilliardsPhysics.shotFromDrag(BallId.CUE, pull)!!
        assertEquals(expected.x, shot.direction.x, 1e-12)
        assertEquals(expected.y, shot.direction.y, 1e-12)
    }
    @Test fun `downward pull shoots upward`() = opposite(Vec2(0.0, 100.0), Vec2(0.0, -1.0))
    @Test fun `upward pull shoots downward`() = opposite(Vec2(0.0, -100.0), Vec2(0.0, 1.0))
    @Test fun `left pull shoots right`() = opposite(Vec2(-100.0, 0.0), Vec2(1.0, 0.0))
    @Test fun `right pull shoots left`() = opposite(Vec2(100.0, 0.0), Vec2(-1.0, 0.0))
    @Test fun `diagonal pull shoots exact opposite`() = opposite(Vec2(30.0, 40.0), Vec2(-.6, -.8))
    @Test fun `pull magnitude power threshold and clamps remain unchanged`() {
        assertNull(BilliardsPhysics.shotFromDrag(BallId.CUE, Vec2(0.0, 4.99)))
        listOf(5.0 to 70.0, 50.0 to 150.0, 100.0 to 300.0, 1000.0 to 650.0).forEach { (pull, power) ->
            assertEquals(power, BilliardsPhysics.shotFromDrag(BallId.CUE, Vec2(0.0, pull))!!.power, 0.0)
        }
    }
}
