package com.nahtygal.olivialooi.games.shapes

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class ShapesEngineTest {
    private val modes = listOf(ShapesMode.FIND_SHAPE, ShapesMode.FIND_COLOR_SHAPE)
    private fun fresh(mode: ShapesMode = ShapesMode.FIND_SHAPE) = ShapesEngine.newGame(mode, Random(42))
    private fun wrong(s: ShapesState) = s.answerChoices.first { !ShapesEngine.isCorrect(s, it) }.stableId
    private fun correct(s: ShapesState) = ShapesEngine.selectChoice(s, s.currentTarget.stableId)
    private fun finish(s: ShapesState) = ShapesEngine.finishEvaluation(s, s.attemptIdentity)
    private fun completed(mode: ShapesMode): ShapesState {
        var s = fresh(mode)
        repeat(5) {
            s = finish(correct(s))
            if (it < 4) s = ShapesEngine.nextRound(s, Random(it))
        }
        return s
    }
    private fun valid(s: ShapesState) {
        assertEquals(3, s.answerChoices.size)
        assertEquals(3, s.answerChoices.toSet().size)
        assertEquals(1, s.answerChoices.count { ShapesEngine.isCorrect(s, it) })
        if (s.selectedMode == ShapesMode.FIND_SHAPE) {
            assertEquals(setOf(s.currentTarget.color), s.answerChoices.map { it.color }.toSet())
            assertEquals(3, s.answerChoices.map { it.shape }.toSet().size)
        } else {
            assertEquals(1, s.answerChoices.count { it.color == s.currentTarget.color && it.shape != s.currentTarget.shape })
            assertEquals(1, s.answerChoices.count { it.shape == s.currentTarget.shape && it.color != s.currentTarget.color })
        }
    }

    @Test fun `six exact shapes`() { assertEquals(listOf("circle", "square", "triangle", "star", "heart", "rectangle"), ShapeCatalog.entries.map { it.stableId }) }
    @Test fun `shape IDs unique`() { assertEquals(6, ShapeCatalog.entries.map { it.stableId }.toSet().size) }
    @Test fun `shape names populated`() { ShapeCatalog.entries.forEach { assertTrue(listOf(it.displayName, it.spokenName, it.accessibilityName).all(String::isNotBlank)) } }
    @Test fun `six exact colors`() { assertEquals(listOf("red", "blue", "green", "yellow", "purple", "pink"), ColorCatalog.entries.map { it.stableId }) }
    @Test fun `color IDs unique`() { assertEquals(6, ColorCatalog.entries.map { it.stableId }.toSet().size) }
    @Test fun `color names populated`() { ColorCatalog.entries.forEach { assertTrue(listOf(it.displayName, it.spokenName, it.accessibilityName).all(String::isNotBlank)) } }
    @Test fun `catalog lookup`() { ShapeCatalog.entries.forEach { assertEquals(it, ShapeCatalog.findById(it.stableId)) }; ColorCatalog.entries.forEach { assertEquals(it, ColorCatalog.findById(it.stableId)) } }
    @Test fun `unknown lookup safe`() { listOf("", "unknown", "CIRCLE").forEach { assertNull(ShapeCatalog.findById(it)); assertNull(ColorCatalog.findById(it)) } }
    @Test fun `fresh learn valid`() { val s = fresh(ShapesMode.LEARN); assertEquals(0, s.learnIndex); assertEquals(ShapeChoice(ShapeId.CIRCLE, ColorId.RED), s.currentTarget); assertTrue(s.answerChoices.isEmpty()); assertFalse(s.usesStars) }
    @Test fun `learn covers shapes`() { assertEquals(ShapeId.entries.toSet(), ShapesEngine.learnSequence.map { it.shape }.toSet()) }
    @Test fun `learn covers colors`() { assertEquals(ColorId.entries.toSet(), ShapesEngine.learnSequence.map { it.color }.toSet()) }
    @Test fun `learn next ordered`() { val s = ShapesEngine.nextShape(fresh(ShapesMode.LEARN)); assertEquals(1, s.learnIndex); assertEquals(ShapesEngine.learnSequence[1], s.currentTarget) }
    @Test fun `learn previous ordered`() { val s = fresh(ShapesMode.LEARN); assertEquals(s, ShapesEngine.previousShape(ShapesEngine.nextShape(s))) }
    @Test fun `learn wraps both ends`() { val s = fresh(ShapesMode.LEARN); val last = ShapesEngine.previousShape(s); assertEquals(11, last.learnIndex); assertEquals(s, ShapesEngine.nextShape(last)) }
    @Test fun `learn cannot award stars`() { var s = fresh(ShapesMode.LEARN); repeat(40) { s = ShapesEngine.selectChoice(s, s.currentTarget.stableId); s = ShapesEngine.nextShape(s); assertEquals(0, s.sessionStars) } }
    @Test fun `learn never evaluates`() { val s = fresh(ShapesMode.LEARN); assertSame(s, ShapesEngine.selectChoice(s, s.currentTarget.stableId)); assertSame(s, finish(s)); assertSame(s, ShapesEngine.nextRound(s)); assertSame(s, ShapesEngine.requestHelp(s)); assertEquals(ShapesRoundState.READY, s.roundState) }
    @Test fun `shape has three choices`() { assertEquals(3, fresh().answerChoices.size) }
    @Test fun `shape exactly one target`() { val s = fresh(); assertEquals(1, s.answerChoices.count { it.shape == s.currentTarget.shape }) }
    @Test fun `shape same presentation color`() { val s = fresh(); assertTrue(s.answerChoices.all { it.color == s.currentTarget.color }) }
    @Test fun `shape unique distractors`() { assertEquals(3, fresh().answerChoices.map { it.shape }.toSet().size) }
    @Test fun `positions deterministic and varied`() { modes.forEach { mode ->
        assertEquals(ShapesEngine.newGame(mode, Random(9)), ShapesEngine.newGame(mode, Random(9)))
        assertEquals(setOf(0, 1, 2), (0..100).map { val s = ShapesEngine.newGame(mode, Random(it)); s.answerChoices.indexOf(s.currentTarget) }.toSet())
    } }
    @Test fun `shape correct two stars`() { assertEquals(2, correct(fresh()).sessionStars) }
    @Test fun `shape incorrect one star`() { val s = fresh(); assertEquals(1, ShapesEngine.selectChoice(s, wrong(s)).sessionStars) }
    @Test fun `incorrect no progress`() { modes.forEach { val s = fresh(it); assertEquals(0, ShapesEngine.selectChoice(s, wrong(s)).completedRoundCount) } }
    @Test fun `retry retains target and choices`() { modes.forEach { val s = fresh(it); val r = finish(ShapesEngine.selectChoice(s, wrong(s))); assertEquals(s.currentTarget, r.currentTarget); assertEquals(s.answerChoices, r.answerChoices) } }
    @Test fun `shape correct completes round`() { assertEquals(ShapesRoundState.COMPLETED, finish(correct(fresh())).roundState) }
    @Test fun `duplicate evaluation no stars`() { modes.forEach { val s = finish(correct(fresh(it))); assertSame(s, finish(s)); assertEquals(2, s.sessionStars) } }
    @Test fun `rapid taps ignored`() { modes.forEach { var s = correct(fresh(it)); repeat(100) { s = ShapesEngine.selectChoice(s, s.currentTarget.stableId) }; assertEquals(2, s.sessionStars); assertEquals(1, s.completedRoundCount) } }
    @Test fun `legitimate retry earns star`() { modes.forEach { val s = fresh(it); val r = finish(ShapesEngine.selectChoice(s, wrong(s))); val again = ShapesEngine.selectChoice(r, wrong(r)); assertEquals(2, again.sessionStars); assertEquals(2L, again.attemptIdentity) } }
    @Test fun `completed progress only once`() { modes.forEach { val s = finish(correct(fresh(it))); assertSame(s, ShapesEngine.selectChoice(s, s.currentTarget.stableId)); assertEquals(1, finish(s).completedRoundCount) } }
    @Test fun `shape avoids repeat`() { var s = fresh(); repeat(200) { val previous = s.currentTarget; s = if (s.sessionComplete) ShapesEngine.newSession(s, Random(it)) else ShapesEngine.nextRound(finish(correct(s)), Random(it)); if (!s.sessionComplete) assertNotEquals(previous.shape, s.currentTarget.shape) } }
    @Test fun `combined three choices`() { assertEquals(3, fresh(ShapesMode.FIND_COLOR_SHAPE).answerChoices.size) }
    @Test fun `combined one semantic answer`() { val s = fresh(ShapesMode.FIND_COLOR_SHAPE); assertEquals(1, s.answerChoices.count { ShapesEngine.isCorrect(s, it) }) }
    @Test fun `combined unique choices`() { assertEquals(3, fresh(ShapesMode.FIND_COLOR_SHAPE).answerChoices.toSet().size) }
    @Test fun `combined color only distractor`() { val s = fresh(ShapesMode.FIND_COLOR_SHAPE); assertEquals(1, s.answerChoices.count { it.color == s.currentTarget.color && it.shape != s.currentTarget.shape }) }
    @Test fun `combined shape only distractor`() { val s = fresh(ShapesMode.FIND_COLOR_SHAPE); assertEquals(1, s.answerChoices.count { it.shape == s.currentTarget.shape && it.color != s.currentTarget.color }) }
    @Test fun `combined correct two stars`() { assertEquals(2, correct(fresh(ShapesMode.FIND_COLOR_SHAPE)).sessionStars) }
    @Test fun `combined incorrect one star`() { val s = fresh(ShapesMode.FIND_COLOR_SHAPE); assertEquals(1, ShapesEngine.selectChoice(s, wrong(s)).sessionStars) }
    @Test fun `combined retry unlocks`() { val s = fresh(ShapesMode.FIND_COLOR_SHAPE); assertFalse(finish(ShapesEngine.selectChoice(s, wrong(s))).inputLocked) }
    @Test fun `wrong selection does not reveal answer`() { modes.forEach { val s = fresh(it); val r = ShapesEngine.selectChoice(s, wrong(s)); assertEquals(wrong(s), r.selectedChoiceId); assertFalse(r.roundComplete); assertEquals(s.currentTarget, r.currentTarget); assertEquals(s.answerChoices, r.answerChoices) } }
    @Test fun `combined completes round`() { assertEquals(1, finish(correct(fresh(ShapesMode.FIND_COLOR_SHAPE))).completedRoundCount) }
    @Test fun `combined duplicate evaluation ignored`() { val s = correct(fresh(ShapesMode.FIND_COLOR_SHAPE)); assertEquals(finish(s), finish(finish(s))) }
    @Test fun `combined avoids exact repeats`() { var s = fresh(ShapesMode.FIND_COLOR_SHAPE); repeat(200) { val previous = s.currentTarget; s = if (s.sessionComplete) ShapesEngine.newSession(s, Random(it)) else ShapesEngine.nextRound(finish(correct(s)), Random(it)); if (!s.sessionComplete) assertNotEquals(previous, s.currentTarget) } }
    @Test fun `help never changes stars`() { modes.forEach { val s = fresh(it); assertEquals(0, ShapesEngine.requestHelp(s).sessionStars) } }
    @Test fun `help never selects`() { modes.forEach { assertNull(ShapesEngine.requestHelp(fresh(it)).selectedChoiceId) } }
    @Test fun `help never completes`() { modes.forEach { assertEquals(0, ShapesEngine.requestHelp(fresh(it)).completedRoundCount) } }
    @Test fun `help target stable`() { modes.forEach { val s = fresh(it); assertEquals(s.currentTarget, ShapesEngine.requestHelp(s).currentTarget) } }
    @Test fun `help choices stable`() { modes.forEach { val s = fresh(it); assertEquals(s.answerChoices, ShapesEngine.requestHelp(s).answerChoices) } }
    @Test fun `help resets new round`() { modes.forEach { val s = ShapesEngine.requestHelp(fresh(it)); assertEquals(0, ShapesEngine.nextRound(finish(correct(s))).helpCount) } }
    @Test fun `session exactly five correct rounds`() { modes.forEach { mode -> var s = fresh(mode); repeat(5) { i -> s = finish(correct(s)); assertEquals(i + 1, s.completedRoundCount); assertEquals(i == 4, s.sessionComplete); s = ShapesEngine.nextRound(s) }; assertEquals(5, s.completedRoundCount) } }
    @Test fun `many incorrect attempts no progress`() { modes.forEach { var s = fresh(it); repeat(25) { s = finish(ShapesEngine.selectChoice(s, wrong(s))) }; assertEquals(0, s.completedRoundCount); assertFalse(s.sessionComplete); assertEquals(25, s.sessionStars) } }
    @Test fun `stars accumulate five rounds with retries`() { modes.forEach { mode -> var s = fresh(mode); repeat(5) { s = finish(ShapesEngine.selectChoice(s, wrong(s))); s = finish(correct(s)); s = ShapesEngine.nextRound(s) }; assertEquals(15, s.sessionStars) } }
    @Test fun `play again resets stars`() { modes.forEach { assertEquals(0, ShapesEngine.newSession(completed(it)).sessionStars) } }
    @Test fun `play again resets progress and feedback`() { modes.forEach { val s = ShapesEngine.newSession(completed(it)); assertEquals(0, s.completedRoundCount); assertEquals(0, s.helpCount); assertNull(s.selectedChoiceId); assertNull(s.awardedAttemptIdentity); assertEquals(ShapesRoundState.READY, s.roundState); assertFalse(s.sessionComplete) } }
    @Test fun `play again preserves mode`() { modes.forEach { assertEquals(it, ShapesEngine.newSession(completed(it)).selectedMode) } }
    @Test fun `stars never negative under randomized play`() { modes.forEach { mode -> val random = Random(101); var s = fresh(mode); repeat(1000) { s = when { s.sessionComplete -> ShapesEngine.newSession(finish(s), random); s.roundState == ShapesRoundState.COMPLETED -> ShapesEngine.nextRound(s, random); s.inputLocked -> finish(s); else -> ShapesEngine.selectChoice(s, s.answerChoices.random(random).stableId) }; assertTrue(s.sessionStars >= 0) } } }
    @Test fun `randomized shape invariant`() { repeat(1000) { valid(ShapesEngine.newGame(ShapesMode.FIND_SHAPE, Random(it))) } }
    @Test fun `randomized combined invariant`() { repeat(1000) { valid(ShapesEngine.newGame(ShapesMode.FIND_COLOR_SHAPE, Random(it))) } }
    @Test fun `correctness independent of position`() { modes.forEach { val s = fresh(it); s.answerChoices.shuffled(Random(99)).forEach { c -> val r = ShapesEngine.selectChoice(s.copy(answerChoices = s.answerChoices.reversed()), c.stableId); assertEquals(if (ShapesEngine.isCorrect(s, c)) 2 else 1, r.sessionStars) } } }
    @Test fun `pure source has no platform or network imports`() {
        val root = java.io.File("src/main/java/com/nahtygal/olivialooi/games/shapes").let { if (it.exists()) it else java.io.File("app", it.path) }
        assertTrue(root.isDirectory)
        root.listFiles()!!.filter { it.extension == "kt" }.forEach { file ->
            file.readLines().filter { it.startsWith("import ") }.forEach { assertTrue(it, it.startsWith("import kotlin.")) }
        }
    }
    @Test fun `stale tap after retry rejected`() { modes.forEach { val s = fresh(it); val r = finish(ShapesEngine.selectChoice(s, wrong(s))); assertSame(r, ShapesEngine.selectChoice(r, wrong(r), s.attemptIdentity)) } }
    @Test fun `stale evaluation cannot end new attempt`() { modes.forEach { val s = ShapesEngine.selectChoice(fresh(it), wrong(fresh(it))); val r = correct(finish(s)); assertSame(r, ShapesEngine.finishEvaluation(r, s.attemptIdentity)) } }
    @Test fun `unknown choice ignored`() { modes.forEach { val s = fresh(it); assertSame(s, ShapesEngine.selectChoice(s, "unknown")) } }
    @Test fun `next round cannot skip feedback or ready round`() { modes.forEach { val s = fresh(it); assertSame(s, ShapesEngine.nextRound(s)); val c = correct(s); assertSame(c, ShapesEngine.nextRound(c)) } }
    @Test fun `help ignored during evaluation`() { modes.forEach { val s = correct(fresh(it)); assertSame(s, ShapesEngine.requestHelp(s)) } }
    @Test fun `help speech progression`() { modes.forEach { val first = ShapesEngine.requestHelp(fresh(it)); assertEquals(ShapesSpeech.prompt(first), ShapesSpeech.help(first)); val second = ShapesEngine.requestHelp(first); val expected = if (it == ShapesMode.FIND_SHAPE) "Look carefully at the shape." else "Look for the color and the shape."; assertEquals(expected, ShapesSpeech.help(second)); assertEquals(expected, ShapesSpeech.help(ShapesEngine.requestHelp(second))) } }
    @Test fun `required speech phrases`() { val learn = fresh(ShapesMode.LEARN); assertEquals("Red circle!", ShapesSpeech.prompt(learn)); val s = fresh().copy(currentTarget = ShapeChoice(ShapeId.TRIANGLE, ColorId.BLUE)); assertEquals("Can you find the triangle?", ShapesSpeech.prompt(s)); assertEquals("You found the triangle!", ShapesSpeech.correct(s)); val c = s.copy(selectedMode = ShapesMode.FIND_COLOR_SHAPE, currentTarget = ShapeChoice(ShapeId.STAR, ColorId.PURPLE)); assertEquals("Can you find the purple star?", ShapesSpeech.prompt(c)); assertEquals("You found the purple star!", ShapesSpeech.correct(c)); assertEquals("Almost! Try again.", ShapesSpeech.INCORRECT); assertEquals("Great job, Olivia!", ShapesSpeech.SESSION_COMPLETE) }
    @Test fun `learn combinations varied`() { assertEquals(12, ShapesEngine.learnSequence.toSet().size); ShapesEngine.learnSequence.zipWithNext().forEach { assertNotEquals(it.first, it.second) } }
    @Test fun `save restore every learn position`() { var s = fresh(ShapesMode.LEARN); repeat(12) { assertEquals(s, ShapesStateCodec.decode(ShapesStateCodec.encode(s))); s = ShapesEngine.nextShape(s) } }
    @Test fun `save restore quiz evaluation retry and completion`() { modes.forEach { mode -> val ready = ShapesEngine.requestHelp(fresh(mode)); val wrong = ShapesEngine.selectChoice(ready, wrong(ready)); val retry = finish(wrong); val correct = correct(retry); listOf(ready, wrong, retry, correct, finish(correct), ShapesEngine.nextRound(finish(correct)), completed(mode)).forEach { assertEquals(it, ShapesStateCodec.decode(ShapesStateCodec.encode(it))) } } }
    @Test fun `restored evaluation cannot award twice`() { modes.forEach { val s = correct(fresh(it)); val restored = ShapesStateCodec.decode(ShapesStateCodec.encode(s))!!; assertSame(restored, ShapesEngine.selectChoice(restored, restored.currentTarget.stableId)); assertEquals(2, finish(restored).sessionStars); assertEquals(1, finish(finish(restored)).completedRoundCount) } }
    @Test fun `invalid saved payload fails safely`() { assertNull(ShapesStateCodec.decode("")); assertNull(ShapesStateCodec.decode("broken|data")) }
}
