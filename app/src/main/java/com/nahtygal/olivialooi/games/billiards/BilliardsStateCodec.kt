package com.nahtygal.olivialooi.games.billiards

/** Version 2 requires the full 16-ball table; incompatible version 1 sessions fail safely.
 * Live velocities and pending AI plans are preserved. The fixed-step loop resumes only
 * in the foreground, so recreation neither settles a turn early nor replans an AI shot.
 */
object BilliardsStateCodec {
    fun encode(s: BilliardsState): String = listOf(
        "2", s.mode.name, s.turn.name,
        s.oliviaBalls.sortedBy { it.number }.joinToString(",") { it.number.toString() },
        s.looLooBalls.sortedBy { it.number }.joinToString(",") { it.number.toString() },
        s.shotActive, s.turnIdentity, s.selectedBall?.number?.toString().orEmpty(),
        s.pendingAi?.let { "${it.ballId.number},${it.pocketId},${it.direction.x},${it.direction.y},${it.power}" }.orEmpty(),
        s.target?.let { "${it.ballId.number},${it.pocketId},${it.start.x},${it.start.y}" }.orEmpty(),
        s.completedRounds, s.roundComplete, s.sessionComplete, s.targetSucceeded, s.outcome.name, s.aimGuide,
        s.table.balls.joinToString(";") { "${it.id.number},${it.position.x},${it.position.y},${it.velocity.x},${it.velocity.y},${it.radius},${it.pocketId ?: -1}" },
    ).joinToString("|")

    fun decode(payload: String): BilliardsState? = runCatching {
        val f = payload.split('|')
        require(f.size == 17 && f[0] == "2")
        fun id(value: String): BallId = BallId.entries.first { it.number == value.toInt() }
        fun ids(value: String): Set<BallId> = if (value.isEmpty()) emptySet() else value.split(',').map(::id).toSet()
        val balls = f[16].split(';').map { raw ->
            val v = raw.split(','); require(v.size == 7)
            Ball(id(v[0]), Vec2(v[1].toDouble(), v[2].toDouble()), Vec2(v[3].toDouble(), v[4].toDouble()), v[5].toDouble(), v[6].toInt().takeIf { it != -1 })
        }
        val ai = f[8].takeIf { it.isNotEmpty() }?.split(',')?.let { v ->
            require(v.size == 5)
            LooLooShot(id(v[0]), v[1].toInt(), Vec2(v[2].toDouble(), v[3].toDouble()), v[4].toDouble())
        }
        val target = f[9].takeIf { it.isNotEmpty() }?.split(',')?.let { v ->
            require(v.size == 4)
            PocketTarget(id(v[0]), v[1].toInt(), Vec2(v[2].toDouble(), v[3].toDouble()))
        }
        val s = BilliardsState(
            mode = BilliardsMode.valueOf(f[1]), table = TableState(balls), turn = TurnOwner.valueOf(f[2]),
            oliviaBalls = ids(f[3]), looLooBalls = ids(f[4]), shotActive = f[5].toBooleanStrict(), turnIdentity = f[6].toLong(),
            selectedBall = f[7].takeIf { it.isNotEmpty() }?.let(::id), pendingAi = ai, target = target,
            completedRounds = f[10].toInt(), roundComplete = f[11].toBooleanStrict(), sessionComplete = f[12].toBooleanStrict(),
            targetSucceeded = f[13].toBooleanStrict(), outcome = ShotOutcome.valueOf(f[14]), aimGuide = f[15].toBooleanStrict(),
        )
        require(balls.size == 16 && balls.map { it.id }.toSet().size == 16)
        require(s.turnIdentity >= 0 && s.completedRounds in 0..5)
        require(s.oliviaBalls.intersect(s.looLooBalls).isEmpty())
        require(BallId.CUE !in s.oliviaBalls + s.looLooBalls)
        require((s.oliviaBalls + s.looLooBalls).size <= 15)
        require((s.oliviaBalls + s.looLooBalls).all { owner -> balls.any { it.id == owner && it.pocketed } })
        require(balls.all { it.radius == 24.0 && (it.pocketId == null || it.pocketId in 0..5) && it.velocity.length() <= BilliardsPhysics.MAX_SPEED * 1.5 })
        require(balls.all { if (it.pocketed) it.velocity == Vec2.ZERO else it.position.x in 24.0..576.0 && it.position.y in 24.0..976.0 })
        require(!s.table.moving || s.shotActive)
        require(!s.sessionComplete || !s.shotActive)
        require(ai == null || (s.mode == BilliardsMode.VERSUS && s.turn == TurnOwner.LOOLOO && !s.shotActive && !s.sessionComplete &&
            ai.ballId != BallId.CUE && ai.pocketId in 0..5 && ai.power in BilliardsPhysics.MIN_SPEED..BilliardsPhysics.MAX_SPEED && ai.direction.length() > .9))
        if (s.mode == BilliardsMode.TRY_POCKET) {
            require(target != null && target.ballId != BallId.CUE && target.pocketId in 0..5)
            require(s.sessionComplete == (s.completedRounds == 5))
            require(!s.roundComplete || s.completedRounds > 0)
        } else require(target == null && s.completedRounds == 0 && !s.roundComplete)
        if (s.mode != BilliardsMode.VERSUS) require(s.oliviaBalls.isEmpty() && s.looLooBalls.isEmpty() && ai == null)
        s
    }.getOrNull()
}
