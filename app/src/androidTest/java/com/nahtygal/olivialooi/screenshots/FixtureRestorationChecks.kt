package com.nahtygal.olivialooi.screenshots

/** Host-safe regression checks for restoration contracts; UI restoration is exercised instrumented. */
object FixtureRestorationChecks {
    @JvmStatic fun run() {
        check(FixtureEquivalence.matches("1|moonlight_snowman|TO_ME|0|false|4", "1|moonlight_snowman|TO_ME|0|false|5"))
        check(!FixtureEquivalence.matches("1|moonlight_snowman|TO_ME|0|false|4", "1|moonlight_snowman|TO_ME|1|false|5"))
        check(FixtureEquivalence.matches(0, 0))
        check(!FixtureEquivalence.matches(0, 1))
    }
}
