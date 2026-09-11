package com.nahtygal.olivialooi.stories

import java.io.File
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class StoryTimeTest {
    private val id = StoryCatalog.stories.first().id
    private fun state(mode: ReadingMode = ReadingMode.TO_ME) = StoryEngine.chooseMode(StoryEngine.open(id), mode)
    @Test fun `fifteen unique stable stories`() { assertEquals(15,StoryCatalog.stories.size); assertEquals(15,StoryCatalog.stories.map { it.id }.toSet().size); assertEquals(listOf("moonlight_snowman","yellow_duck","rainbow_sisters","sleepy_puppy","little_star"),StoryCatalog.stories.take(5).map { it.id }) }
    @Test fun `six pages each ordered one to six`() { assertEquals(90,StoryCatalog.stories.sumOf { it.pages.size }); StoryCatalog.stories.forEach { assertEquals((1..6).toList(),it.pages.map { p -> p.pageNumber }) } }
    @Test fun `titles descriptions and page text are nonblank`() { StoryCatalog.stories.forEach { s -> assertTrue(s.title.isNotBlank()); assertTrue(s.shortDescription.isNotBlank()); s.pages.forEach { assertTrue(it.text.isNotBlank()) } } }
    @Test fun `covers and all page visuals present`() { assertTrue(StoryCatalog.stories.map { it.coverVisual }.toSet().size >= 4); StoryCatalog.stories.forEach { s -> s.pages.forEach { assertEquals(s.coverVisual,it.visual) } } }
    @Test fun `short spoken pages and stories within length budget`() { StoryCatalog.stories.forEach { s -> var total=0; s.pages.forEach { val words=it.text.split(Regex("\\s+")).size; assertTrue("${s.id}: $words",words in 10..40); total+=words }; assertTrue(total<200) } }
    @Test fun `catalog and engine have no platform speech or network imports`() {
        val root=listOf(File("src/main/java"),File("app/src/main/java")).first { it.exists() }
        val files=File(root,"com/nahtygal/olivialooi/stories").listFiles()!!
        files.forEach { file -> assertTrue(file.readLines().filter { it.startsWith("import ") }.all { it.startsWith("import kotlin.") }) }
    }
    @Test fun `opening is silent on page one`() { val s=StoryEngine.open(id); assertEquals(0,s.page); assertNull(s.mode); assertFalse(s.narrating) }
    @Test fun `choosing narration starts current page`() { assertTrue(state().narrating) }
    @Test fun `choosing myself is silent`() { assertFalse(state(ReadingMode.MYSELF).narrating) }
    @Test fun `next advances and previous returns`() { assertEquals(1,StoryEngine.next(state()).page); assertEquals(0,StoryEngine.previous(StoryEngine.next(state())).page) }
    @Test fun `previous cannot go below page one`() { val s=state(); assertSame(s,StoryEngine.previous(s)) }
    @Test fun `sixth next completes and locks story`() { var s=state(); repeat(6) { s=StoryEngine.next(s) }; assertTrue(s.complete); assertEquals(5,s.page); assertFalse(s.narrating); assertSame(s,StoryEngine.next(s)); assertSame(s,StoryEngine.say(s)) }
    @Test fun `again starts first page in same mode`() { var s=state(ReadingMode.MYSELF); repeat(6) { s=StoryEngine.next(s) }; val again=StoryEngine.again(s); assertEquals(0,again.page); assertFalse(again.complete); assertEquals(ReadingMode.MYSELF,again.mode) }
    @Test fun `switching story resets page and invalidates narration`() { val old=StoryEngine.next(state()); val s=StoryEngine.open(StoryCatalog.stories[1].id,old); assertEquals(0,s.page); assertFalse(s.narrating); assertTrue(s.generation>old.generation); assertSame(s,StoryEngine.finished(s,old.generation,true)) }
    @Test fun `mode switch preserves story and page and rejects old completion`() { val old=StoryEngine.next(state()); val s=StoryEngine.chooseMode(old,ReadingMode.MYSELF); assertEquals(old.storyId,s.storyId); assertEquals(old.page,s.page); assertSame(s,StoryEngine.finished(s,old.generation,true)) }
    @Test fun `no scoring or stars state`() { assertFalse(StoryPlaybackState::class.java.declaredFields.any { it.name.contains("score",true)||it.name.contains("star",true) }) }
    @Test fun `current narration advances`() { val s=state(); val n=StoryEngine.finished(s,s.generation,true); assertEquals(1,n.page); assertTrue(n.narrating) }
    @Test fun `stale narration does not advance`() { val s=state(); assertSame(s,StoryEngine.finished(s,s.generation-1,true)) }
    @Test fun `duplicate callback cannot advance twice`() { val s=state(); val n=StoryEngine.finished(s,s.generation,true); assertSame(n,StoryEngine.finished(n,s.generation,true)) }
    @Test fun `manual next invalidates prior speech`() { val s=state(); val n=StoryEngine.next(s); assertSame(n,StoryEngine.finished(n,s.generation,true)) }
    @Test fun `manual previous invalidates prior speech`() { val s=StoryEngine.next(state()); val n=StoryEngine.previous(s); assertSame(n,StoryEngine.finished(n,s.generation,true)) }
    @Test fun `say again keeps page and replaces identity`() { val s=StoryEngine.next(state()); val n=StoryEngine.say(s); assertEquals(s.page,n.page); assertTrue(n.generation>s.generation); assertSame(n,StoryEngine.finished(n,s.generation,true)) }
    @Test fun `pause and leaving invalidate speech`() { val s=state(); val n=StoryEngine.stop(s); assertFalse(n.narrating); assertSame(n,StoryEngine.finished(n,s.generation,true)) }
    @Test fun `resume speaks same page with fresh identity`() { val s=StoryEngine.next(state()); val resumed=StoryEngine.say(StoryEngine.stop(s)); assertEquals(s.page,resumed.page); assertTrue(resumed.narrating); assertTrue(resumed.generation>s.generation) }
    @Test fun `stop reading returns to silent opening`() { val s=state(); val n=StoryEngine.opening(s); assertNull(n.mode); assertFalse(n.narrating); assertSame(n,StoryEngine.finished(n,s.generation,true)) }
    @Test fun `read myself say it never advances`() { val s=StoryEngine.say(state(ReadingMode.MYSELF)); val n=StoryEngine.finished(s,s.generation,true); assertEquals(s.page,n.page); assertFalse(n.narrating) }
    @Test fun `failed narration pauses rather than skipping page`() { val s=state(); val n=StoryEngine.finished(s,s.generation,false); assertEquals(s.page,n.page); assertFalse(n.narrating); assertTrue(n.speechFailed) }
    @Test fun `final narration completes story`() { var s=state(); repeat(6) { s=StoryEngine.finished(s,s.generation,true) }; assertTrue(s.complete); assertFalse(s.narrating) }
    @Test fun `restore retains story mode and page but never restarts speech`() { val s=StoryEngine.next(state()); val n=StoryEngine.decode(StoryEngine.encode(s))!!; assertEquals(s.storyId,n.storyId); assertEquals(s.mode,n.mode); assertEquals(s.page,n.page); assertFalse(n.narrating); assertTrue(n.generation>s.generation) }
    @Test fun `completion state restores`() { var s=state(); repeat(6) { s=StoryEngine.next(s) }; assertTrue(StoryEngine.decode(StoryEngine.encode(s))!!.complete) }
    @Test fun `invalid saves fail safely`() { listOf("","1|bad||0|false|0","1|$id|TO_ME|6|false|0","1|$id|TO_ME|-1|false|0","1|$id|BAD|0|false|0","1|$id|TO_ME|0|true|0","1|$id|TO_ME|0|false|-1").forEach { assertNull(it,StoryEngine.decode(it)) } }
    @Test fun `random interactions preserve bounds generations and stale safety`() {
        val random=Random(25)
        repeat(50) {
            var s=state()
            repeat(100) {
                val old=s
                s=when(random.nextInt(8)) {
                    0 -> StoryEngine.next(s)
                    1 -> StoryEngine.previous(s)
                    2 -> StoryEngine.say(s)
                    3 -> StoryEngine.stop(s)
                    4 -> StoryEngine.finished(s,s.generation,true)
                    5 -> StoryEngine.again(s)
                    6 -> StoryEngine.chooseMode(s,ReadingMode.entries.random(random))
                    else -> StoryEngine.decode(StoryEngine.encode(s))!!
                }
                assertTrue(s.page in 0..5); assertTrue(s.generation>=old.generation)
                assertFalse(s.complete && s.narrating)
                if(s.generation!=old.generation) assertSame(s,StoryEngine.finished(s,old.generation,true))
            }
        }
    }
}
