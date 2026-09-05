package com.nahtygal.olivialooi.games.drums

import kotlin.random.Random

enum class DrumMode { FREE_PLAY, COPY_BEAT }
enum class BeatPhase { REPLAY, YOUR_TURN, COMPLETE }

data class DrumState(
    val mode: DrumMode = DrumMode.FREE_PLAY,
    val pattern: List<DrumSound> = emptyList(),
    val completedRounds: Int = 0,
    val position: Int = 0,
    val phase: BeatPhase = BeatPhase.YOUR_TURN,
    val replayIdentity: Long = 0,
) {
    val round: Int get() = (completedRounds + if (phase == BeatPhase.COMPLETE) 0 else 1).coerceIn(1, 5)
    val sessionComplete: Boolean get() = completedRounds == 5
    val inputLocked: Boolean get() = mode == DrumMode.COPY_BEAT && phase != BeatPhase.YOUR_TURN
    val expected: DrumSound? get() = if (mode == DrumMode.COPY_BEAT) pattern.getOrNull(position) else null
}

object DrumEngine {
    fun copyBeat(state: DrumState, random: Random = Random.Default): DrumState =
        if (state.mode == DrumMode.COPY_BEAT && !state.sessionComplete) state
        else newRound(0, state.replayIdentity + 1, random)
    fun freePlay(state: DrumState): DrumState = DrumState(replayIdentity = state.replayIdentity + 1)

    fun hit(state: DrumState, sound: DrumSound): DrumState {
        if (state.mode != DrumMode.COPY_BEAT || state.inputLocked || state.sessionComplete) return state
        if (sound != state.expected) return state.copy(position = 0)
        val next = state.position + 1
        return if (next == state.pattern.size) state.copy(
            position = next, completedRounds = state.completedRounds + 1, phase = BeatPhase.COMPLETE,
        ) else state.copy(position = next)
    }
    /** Replay preserves the child's partial attempt as well as completed-round progress. */
    fun replay(state: DrumState): DrumState =
        if (state.mode != DrumMode.COPY_BEAT || state.phase != BeatPhase.YOUR_TURN || state.sessionComplete) state
        else state.copy(phase = BeatPhase.REPLAY, replayIdentity = state.replayIdentity + 1)
    fun interruptReplay(state: DrumState): DrumState =
        if (state.mode == DrumMode.COPY_BEAT && state.phase == BeatPhase.REPLAY)
            state.copy(replayIdentity = state.replayIdentity + 1) else state
    fun finishReplay(state: DrumState, identity: Long): DrumState =
        if (state.mode == DrumMode.COPY_BEAT && state.phase == BeatPhase.REPLAY && identity == state.replayIdentity)
            state.copy(phase = BeatPhase.YOUR_TURN) else state
    fun nextBeat(state: DrumState, random: Random = Random.Default): DrumState =
        if (state.mode != DrumMode.COPY_BEAT || state.phase != BeatPhase.COMPLETE || state.sessionComplete) state
        else newRound(state.completedRounds, state.replayIdentity + 1, random)
    private fun newRound(completed: Int, identity: Long, random: Random): DrumState {
        val pattern = MutableList(completed + 2) { DrumSound.entries.random(random) }
        if (pattern.distinct().size == 1) {
            pattern[pattern.lastIndex] = DrumSound.entries.filter { it != pattern.first() }.random(random)
        }
        return DrumState(DrumMode.COPY_BEAT, pattern.toList(), completed, 0, BeatPhase.REPLAY, identity)
    }
}

object DrumStateCodec {
    fun encode(state: DrumState): String = listOf(
        "1", state.mode.name, state.pattern.joinToString(",") { it.stableId }, state.completedRounds,
        state.position, state.phase.name, state.replayIdentity,
    ).joinToString("|")
    fun decode(payload: String): DrumState? = runCatching {
        val f = payload.split('|')
        require(f.size == 7 && f[0] == "1")
        val pattern = if (f[2].isEmpty()) emptyList() else f[2].split(',').map { requireNotNull(DrumCatalog.findById(it)) }
        val s = DrumState(DrumMode.valueOf(f[1]), pattern, f[3].toInt(), f[4].toInt(), BeatPhase.valueOf(f[5]), f[6].toLong())
        require(s.completedRounds in 0..5 && s.replayIdentity >= 0)
        if (s.mode == DrumMode.FREE_PLAY) {
            require(pattern.isEmpty() && s.completedRounds == 0 && s.position == 0 && s.phase == BeatPhase.YOUR_TURN)
        } else {
            require(pattern.size == s.round + 1 && pattern.distinct().size >= 2)
            if (s.phase == BeatPhase.COMPLETE) require(s.completedRounds >= 1 && s.position == pattern.size)
            else require(s.completedRounds < 5 && s.position in pattern.indices)
        }
        s
    }.getOrNull()
}
