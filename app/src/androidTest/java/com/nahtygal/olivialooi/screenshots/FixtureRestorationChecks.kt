package com.nahtygal.olivialooi.screenshots

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import com.nahtygal.olivialooi.games.drums.BeatPhase
import com.nahtygal.olivialooi.games.drums.DrumState
import com.nahtygal.olivialooi.games.drums.DrumStateCodec

/** Host-safe regression checks for restoration contracts; UI restoration is exercised instrumented. */
object FixtureRestorationChecks {
    @JvmStatic fun run() {
        check(FixtureEquivalence.matches("1|moonlight_snowman|TO_ME|0|false|4", "1|moonlight_snowman|TO_ME|0|false|5"))
        check(!FixtureEquivalence.matches("1|moonlight_snowman|TO_ME|0|false|4", "1|moonlight_snowman|TO_ME|1|false|5"))
        check(FixtureEquivalence.matches(0, 0))
        check(!FixtureEquivalence.matches(0, 1))
        val drum = DrumState(replayIdentity = 4)
        val before = DrumStateCodec.encode(drum)
        val after = DrumStateCodec.encode(drum.copy(replayIdentity = 5))
        check(FixtureEquivalence.matches(before, after))
        check(FixtureEquivalence.difference(before, after) == "replayIdentity expected=4 actual=5")
        check(!FixtureEquivalence.matches(before, DrumStateCodec.encode(drum.copy(phase = BeatPhase.REPLAY))))
        check(FixtureMutableState(true).restoredValue() is MutableState<*>)
        check(FixtureMutableIntState(0).restoredValue() is MutableIntState)
        check(FixtureMutableLongState(0).restoredValue() is MutableLongState)
    }
}
