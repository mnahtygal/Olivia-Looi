package com.nahtygal.olivialooi.games.billiards

import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class Vec2(val x: Double, val y: Double) {
    init { require(x.isFinite() && y.isFinite()) }
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scale: Double) = Vec2(x * scale, y * scale)
    fun dot(other: Vec2): Double = x * other.x + y * other.y
    fun length(): Double = hypot(x, y)
    fun normalized(): Vec2 = length().let { if (it < 1e-9) ZERO else Vec2(x / it, y / it) }
    companion object { val ZERO = Vec2(0.0, 0.0) }
}

enum class BallId(val number: Int, val visualIdentity: String) {
    CUE(0, "white"), ONE(1, "yellow"), TWO(2, "blue"), THREE(3, "red"),
    FOUR(4, "purple"), FIVE(5, "orange"), SIX(6, "green"), SEVEN(7, "pink"),
    EIGHT(8, "charcoal"), NINE(9, "cyan");
    val description: String get() = if (this == CUE) "Cue ball" else "$number ball"
}

data class Ball(
    val id: BallId,
    val position: Vec2,
    val velocity: Vec2 = Vec2.ZERO,
    val radius: Double = 24.0,
    val pocketId: Int? = null,
) {
    init { require(radius.isFinite() && radius > 0) }
    val pocketed: Boolean get() = pocketId != null
}

data class Pocket(val id: Int, val position: Vec2, val radius: Double = 41.0)
data class TableBounds(val width: Double = 600.0, val height: Double = 1000.0)
data class TableState(
    val balls: List<Ball>,
    val bounds: TableBounds = TableBounds(),
    val pockets: List<Pocket> = BilliardsPhysics.pockets,
) {
    val moving: Boolean get() = balls.any { !it.pocketed && it.velocity != Vec2.ZERO }
}

data class PocketEvent(val ballId: BallId, val pocketId: Int)
data class PhysicsResult(val table: TableState, val pocketEvents: List<PocketEvent>)
data class BallShot(val ballId: BallId, val direction: Vec2, val power: Double)

object BilliardsPhysics {
    const val MAX_SPEED = 650.0
    const val MIN_SPEED = 70.0
    const val DECELERATION = 90.0
    const val STOP_SPEED = 2.0
    const val MAX_FRAME_SECONDS = .1
    private const val STEP = 1.0 / 240.0
    val pockets = listOf(
        Pocket(0, Vec2(0.0, 0.0)), Pocket(1, Vec2(600.0, 0.0)),
        Pocket(2, Vec2(0.0, 1000.0)), Pocket(3, Vec2(600.0, 1000.0)),
        Pocket(4, Vec2(0.0, 500.0)), Pocket(5, Vec2(600.0, 500.0)),
    )

    fun rack(): TableState {
        val positions = listOf(
            Vec2(300.0, 780.0), Vec2(300.0, 230.0),
            Vec2(274.0, 277.0), Vec2(326.0, 277.0),
            Vec2(248.0, 324.0), Vec2(300.0, 324.0), Vec2(352.0, 324.0),
            Vec2(222.0, 371.0), Vec2(274.0, 371.0), Vec2(326.0, 371.0),
        )
        return TableState(BallId.entries.map { Ball(it, positions[it.number]) })
    }

    /** Drag toward the intended direction. A tap/near-zero drag launches nothing. */
    fun shotFromDrag(ballId: BallId, drag: Vec2): BallShot? {
        val distance = drag.length()
        if (distance < 5.0) return null
        return BallShot(ballId, drag.normalized(), (distance * 3.0).coerceIn(MIN_SPEED, MAX_SPEED))
    }

    fun launch(table: TableState, shot: BallShot): TableState {
        if (table.moving || !shot.power.isFinite() || shot.direction.length() < 1e-9) return table
        if (table.balls.none { it.id == shot.ballId && !it.pocketed }) return table
        val velocity = shot.direction.normalized() * shot.power.coerceIn(MIN_SPEED, MAX_SPEED)
        return table.copy(balls = table.balls.map { if (it.id == shot.ballId) it.copy(velocity = velocity) else it })
    }

    /** Clamp long frames; fixed maximum substeps travel < 3 units at maximum speed,
     * far below a ball diameter. This prevents normal-speed tunneling without a framework.
     */
    fun advance(table: TableState, elapsed: Double): PhysicsResult {
        if (!elapsed.isFinite() || elapsed <= 0) return PhysicsResult(table, emptyList())
        val duration = min(elapsed, MAX_FRAME_SECONDS)
        val steps = ceil(duration / STEP).toInt().coerceAtLeast(1)
        val dt = duration / steps
        val balls = table.balls.toMutableList()
        val events = mutableListOf<PocketEvent>()
        repeat(steps) {
            for (i in balls.indices) {
                val ball = balls[i]
                if (ball.pocketed) continue
                val speed = ball.velocity.length()
                val velocity = if (speed <= STOP_SPEED) Vec2.ZERO else ball.velocity.normalized() * min(speed, MAX_SPEED)
                val position = ball.position + velocity * dt
                val capture = table.pockets.firstOrNull { (position - it.position).length() <= it.radius }
                if (capture != null) {
                    balls[i] = ball.copy(position = position, velocity = Vec2.ZERO, pocketId = capture.id)
                    events += PocketEvent(ball.id, capture.id)
                } else {
                    val remaining = max(0.0, velocity.length() - DECELERATION * dt)
                    val slowed = if (remaining <= STOP_SPEED) Vec2.ZERO else velocity.normalized() * remaining
                    balls[i] = rail(ball.copy(position = position, velocity = slowed), table.bounds)
                }
            }
            repeat(3) {
                for (i in balls.indices) for (j in i + 1 until balls.size) {
                    if (!balls[i].pocketed && !balls[j].pocketed) {
                        val pair = collide(balls[i], balls[j])
                        balls[i] = pair.first
                        balls[j] = pair.second
                    }
                }
                for (i in balls.indices) if (!balls[i].pocketed) balls[i] = rail(balls[i], table.bounds)
            }
        }
        return PhysicsResult(table.copy(balls = balls.toList()), events.toList())
    }

    fun collide(a: Ball, b: Ball): Pair<Ball, Ball> {
        if (a.pocketed || b.pocketed) return a to b
        val delta = b.position - a.position
        val distance = delta.length()
        val overlap = a.radius + b.radius - distance
        if (overlap < 0) return a to b
        val normal = if (distance < 1e-9) Vec2(1.0, 0.0) else delta.normalized()
        val correction = normal * ((overlap + .001) / 2)
        val relative = (b.velocity - a.velocity).dot(normal)
        val impulse = if (relative < 0) normal * (relative * .98) else Vec2.ZERO
        return a.copy(position = a.position - correction, velocity = a.velocity + impulse) to
            b.copy(position = b.position + correction, velocity = b.velocity - impulse)
    }

    private fun rail(ball: Ball, bounds: TableBounds): Ball {
        val p = ball.position
        var vx = ball.velocity.x
        var vy = ball.velocity.y
        if ((p.x <= ball.radius && vx < 0) || (p.x >= bounds.width - ball.radius && vx > 0)) vx *= -.84
        if ((p.y <= ball.radius && vy < 0) || (p.y >= bounds.height - ball.radius && vy > 0)) vy *= -.84
        val v = Vec2(vx, vy).let { if (it.length() <= STOP_SPEED) Vec2.ZERO else it }
        return ball.copy(position = Vec2(p.x.coerceIn(ball.radius, bounds.width - ball.radius), p.y.coerceIn(ball.radius, bounds.height - ball.radius)), velocity = v)
    }

    /** Deterministic open-position search; never respawn into a pocket or another ball. */
    fun restore(table: TableState, id: BallId, preferred: Vec2 = Vec2(300.0, 780.0)): TableState {
        val ball = table.balls.firstOrNull { it.id == id } ?: return table
        if (!ball.pocketed) return table
        val candidates = buildList {
            add(preferred)
            var y = ball.radius + 25
            while (y < table.bounds.height - ball.radius) {
                var x = ball.radius + 25
                while (x < table.bounds.width - ball.radius) { add(Vec2(x, y)); x += ball.radius * 2 + 4 }
                y += ball.radius * 2 + 4
            }
        }
        val position = candidates.firstOrNull { p ->
            p.x in ball.radius..(table.bounds.width - ball.radius) && p.y in ball.radius..(table.bounds.height - ball.radius) &&
                table.pockets.none { (p - it.position).length() <= it.radius + 2 } &&
                table.balls.none { it.id != id && !it.pocketed && (it.position - p).length() < it.radius + ball.radius + 2 }
        } ?: return table
        return table.copy(balls = table.balls.map { if (it.id == id) it.copy(position = position, velocity = Vec2.ZERO, pocketId = null) else it })
    }
}
