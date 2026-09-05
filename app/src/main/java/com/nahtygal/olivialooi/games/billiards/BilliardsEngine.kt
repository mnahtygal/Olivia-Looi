package com.nahtygal.olivialooi.games.billiards

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class BilliardsMode { FREE_PLAY, VERSUS, TRY_POCKET }
enum class TurnOwner { OLIVIA, LOOLOO }
enum class ShotOutcome { NONE, NICE_ROLL, POCKET, WRONG_POCKET, TARGET_SUCCESS }
enum class MatchResult { OLIVIA_WINS, LOOLOO_WINS, TIE }
data class PocketTarget(val ballId: BallId, val pocketId: Int, val start: Vec2)
data class LooLooShot(val ballId: BallId, val pocketId: Int, val direction: Vec2, val power: Double) {
    fun ballShot() = BallShot(ballId, direction, power)
}

data class BilliardsState(
    val mode: BilliardsMode,
    val table: TableState,
    val turn: TurnOwner = TurnOwner.OLIVIA,
    val oliviaBalls: Set<BallId> = emptySet(),
    val looLooBalls: Set<BallId> = emptySet(),
    val shotActive: Boolean = false,
    val turnIdentity: Long = 0,
    val pendingAi: LooLooShot? = null,
    val selectedBall: BallId? = null,
    val target: PocketTarget? = null,
    val completedRounds: Int = 0,
    val roundComplete: Boolean = false,
    val sessionComplete: Boolean = false,
    val targetSucceeded: Boolean = false,
    val outcome: ShotOutcome = ShotOutcome.NONE,
    val aimGuide: Boolean = true,
) {
    val round: Int get() = (completedRounds + if (roundComplete) 0 else 1).coerceIn(1, 5)
    val childCanShoot: Boolean get() = !shotActive && !table.moving && !roundComplete && !sessionComplete &&
        (mode != BilliardsMode.VERSUS || turn == TurnOwner.OLIVIA)
}

object BilliardsEngine {
    fun newGame(mode: BilliardsMode, random: Random = Random.Default, identity: Long = 0): BilliardsState {
        val state = BilliardsState(mode, BilliardsPhysics.rack(), turnIdentity = identity)
        return if (mode == BilliardsMode.TRY_POCKET) chooseTarget(state, random) else state
    }
    fun rack(state: BilliardsState, random: Random = Random.Default): BilliardsState =
        newGame(state.mode, random, state.turnIdentity + 1).copy(aimGuide = state.aimGuide)

    fun selectBall(state: BilliardsState, id: BallId): BilliardsState =
        if (state.childCanShoot && state.table.balls.any { it.id == id && !it.pocketed } &&
            (state.mode != BilliardsMode.TRY_POCKET || state.target?.ballId == id)) state.copy(selectedBall = id) else state

    fun shoot(state: BilliardsState, shot: BallShot): BilliardsState {
        if (!state.childCanShoot || (state.mode == BilliardsMode.TRY_POCKET && shot.ballId != state.target?.ballId)) return state
        return launch(state, shot)
    }
    private fun launch(state: BilliardsState, shot: BallShot): BilliardsState {
        val table = BilliardsPhysics.launch(state.table, shot)
        if (table === state.table) return state
        return state.copy(table = table, shotActive = true, pendingAi = null, selectedBall = null, outcome = ShotOutcome.NONE, targetSucceeded = false)
    }
    fun prepareAi(state: BilliardsState, identity: Long, random: Random = Random.Default): BilliardsState {
        if (state.mode != BilliardsMode.VERSUS || state.turn != TurnOwner.LOOLOO || state.shotActive || state.table.moving ||
            state.sessionComplete || identity != state.turnIdentity || state.pendingAi != null) return state
        return state.copy(pendingAi = LooLooOpponent.plan(state.table, random))
    }
    fun fireAi(state: BilliardsState, identity: Long): BilliardsState {
        if (state.mode != BilliardsMode.VERSUS || state.turn != TurnOwner.LOOLOO || state.shotActive || state.sessionComplete || identity != state.turnIdentity) return state
        return state.pendingAi?.let { launch(state, it.ballShot()) } ?: state
    }

    /** Physics reports captures; ownership is credited only during the active shot, once per ID. */
    fun tick(state: BilliardsState, elapsed: Double): BilliardsState {
        if (!state.shotActive || state.sessionComplete) return state
        val physics = BilliardsPhysics.advance(state.table, elapsed)
        var updated = state.copy(table = physics.table)
        for (event in physics.pocketEvents) {
            if (event.ballId != BallId.CUE && state.mode == BilliardsMode.VERSUS &&
                event.ballId !in updated.oliviaBalls && event.ballId !in updated.looLooBalls) {
                updated = if (state.turn == TurnOwner.OLIVIA) updated.copy(oliviaBalls = updated.oliviaBalls + event.ballId)
                    else updated.copy(looLooBalls = updated.looLooBalls + event.ballId)
            }
            updated = updated.copy(outcome = ShotOutcome.POCKET)
            if (state.mode == BilliardsMode.TRY_POCKET && event.ballId == state.target?.ballId) {
                updated = if (event.pocketId == state.target.pocketId) updated.copy(targetSucceeded = true, outcome = ShotOutcome.TARGET_SUCCESS)
                    else updated.copy(outcome = ShotOutcome.WRONG_POCKET)
            }
        }
        return if (!updated.table.moving) settle(updated) else updated
    }

    private fun settle(state: BilliardsState): BilliardsState {
        var table = BilliardsPhysics.restore(state.table, BallId.CUE)
        val wrongTarget = state.mode == BilliardsMode.TRY_POCKET && !state.targetSucceeded &&
            table.balls.any { it.id == state.target?.ballId && it.pocketed }
        if (wrongTarget) table = BilliardsPhysics.restore(table, state.target!!.ballId, state.target.start)
        var s = state.copy(table = table, shotActive = false, turnIdentity = state.turnIdentity + 1,
            outcome = if (wrongTarget) ShotOutcome.WRONG_POCKET else if (state.outcome == ShotOutcome.NONE) ShotOutcome.NICE_ROLL else state.outcome)
        when (state.mode) {
            BilliardsMode.FREE_PLAY -> Unit
            BilliardsMode.VERSUS -> {
                val done = table.balls.filter { it.id != BallId.CUE }.all { it.pocketed }
                s = s.copy(sessionComplete = done, turn = if (done) state.turn else if (state.turn == TurnOwner.OLIVIA) TurnOwner.LOOLOO else TurnOwner.OLIVIA)
            }
            BilliardsMode.TRY_POCKET -> if (state.targetSucceeded) {
                val rounds = state.completedRounds + 1
                s = s.copy(completedRounds = rounds, roundComplete = true, sessionComplete = rounds == 5, outcome = ShotOutcome.TARGET_SUCCESS)
            }
        }
        return s
    }

    fun nextTarget(state: BilliardsState, random: Random = Random.Default): BilliardsState =
        if (state.mode != BilliardsMode.TRY_POCKET || !state.roundComplete || state.sessionComplete) state
        else chooseTarget(state.copy(roundComplete = false, targetSucceeded = false, outcome = ShotOutcome.NONE, turnIdentity = state.turnIdentity + 1), random)

    /** Other pocketed balls stay pocketed. Between rounds only, move the target into a
     * clear, short direct-shot position. Re-rack if no active object balls remain.
     */
    private fun chooseTarget(state: BilliardsState, random: Random): BilliardsState {
        val table = if (state.table.balls.none { it.id != BallId.CUE && !it.pocketed }) BilliardsPhysics.rack() else state.table
        val ball = table.balls.filter { it.id != BallId.CUE && !it.pocketed }.random(random)
        val pocket = table.pockets.random(random)
        val start = pocket.position + (Vec2(300.0, 500.0) - pocket.position).normalized() * 190.0
        // Mark just the target for safe placement so the shared open-space check avoids overlaps.
        val marked = table.copy(balls = table.balls.map { if (it.id == ball.id) it.copy(pocketId = pocket.id, velocity = Vec2.ZERO) else it })
        val placed = BilliardsPhysics.restore(marked, ball.id, start)
        val position = placed.balls.first { it.id == ball.id }.position
        return state.copy(table = placed, target = PocketTarget(ball.id, pocket.id, position), selectedBall = null)
    }

    fun matchResult(olivia: Int, looLoo: Int): MatchResult = when {
        olivia > looLoo -> MatchResult.OLIVIA_WINS
        looLoo > olivia -> MatchResult.LOOLOO_WINS
        else -> MatchResult.TIE
    }
}

object LooLooOpponent {
    /** Direct candidates only. No table mutation, teleportation, banks, or pocket credit. */
    fun plan(table: TableState, random: Random = Random.Default): LooLooShot? {
        data class Candidate(val ball: Ball, val pocket: Pocket, val distance: Double, val cost: Double)
        val candidates = table.balls.filter { !it.pocketed && it.id != BallId.CUE }.flatMap { ball ->
            table.pockets.map { pocket ->
                val delta = pocket.position - ball.position
                val distance = delta.length()
                val direction = delta.normalized()
                val blocked = table.balls.any { other ->
                    if (other.id == ball.id || other.pocketed) false else {
                        val relative = other.position - ball.position
                        val along = relative.dot(direction)
                        along > 0 && along < distance && (relative - direction * along).length() < ball.radius + other.radius + 3
                    }
                }
                Candidate(ball, pocket, distance, distance + if (blocked) 1500 else 0)
            }
        }.sortedBy { it.cost }.take(4)
        if (candidates.isEmpty()) return null
        val chosen = candidates.random(random)
        val direct = (chosen.pocket.position - chosen.ball.position).normalized()
        val angle = random.nextDouble(-.18, .18)
        val direction = Vec2(direct.x * cos(angle) - direct.y * sin(angle), direct.x * sin(angle) + direct.y * cos(angle))
        val power = (sqrt(2 * BilliardsPhysics.DECELERATION * (chosen.distance + 50)) * random.nextDouble(.86, 1.06))
            .coerceIn(BilliardsPhysics.MIN_SPEED, BilliardsPhysics.MAX_SPEED)
        return LooLooShot(chosen.ball.id, chosen.pocket.id, direction, power)
    }
}
