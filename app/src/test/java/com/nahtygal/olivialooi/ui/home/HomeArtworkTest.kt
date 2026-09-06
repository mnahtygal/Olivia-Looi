package com.nahtygal.olivialooi.ui.home

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class HomeArtworkTest {
    @Test fun `portrait fit preserves aspect ratio`() {
        val image = HomeArtwork.fit(360f, 780f)
        assertEquals(HomeArtwork.ASPECT_RATIO, image.width / image.height, .0001f)
        assertEquals(360f, image.width, .001f)
        assertTrue(image.top > 0)
    }
    @Test fun `wide container has centered horizontal letterboxing`() {
        val image = HomeArtwork.fit(800f, 500f)
        assertEquals(500f, image.height, .001f)
        assertTrue(image.left > 0)
        assertEquals(800f, image.left * 2 + image.width, .001f)
    }
    @Test fun `each action center maps to only its own action`() {
        listOf(360f to 780f, 800f to 1280f, 900f to 500f).forEach { (w,h) ->
            val image = HomeArtwork.fit(w,h)
            HomeArtworkAction.entries.forEach { action ->
                val r = HomeArtwork.region(action,image)
                assertEquals(action,HomeArtwork.actionAt(r.left+r.width/2,r.top+r.height/2,image))
            }
        }
    }
    @Test fun `faces margins and letterboxing do not trigger actions`() {
        val image = HomeArtwork.fit(800f,1280f)
        listOf(0f to 0f, 799f to 1279f, 400f to 500f, 400f to 700f).forEach { (x,y) ->
            assertNull(HomeArtwork.actionAt(x,y,image))
        }
    }
    @Test fun `randomized regions stay within actual image without overlapping`() {
        val random = Random(244)
        repeat(500) {
            val image = HomeArtwork.fit(random.nextFloat()*1400+200,random.nextFloat()*1600+200)
            val regions = HomeArtworkAction.entries.map { HomeArtwork.region(it,image) }
            regions.forEach { r ->
                assertTrue(r.left >= image.left && r.top >= image.top)
                assertTrue(r.left+r.width <= image.left+image.width+.001f)
                assertTrue(r.top+r.height <= image.top+image.height+.001f)
            }
            regions.forEachIndexed { i,a -> regions.drop(i+1).forEach { b ->
                assertTrue(a.left+a.width <= b.left || b.left+b.width <= a.left || a.top+a.height <= b.top || b.top+b.height <= a.top)
            } }
        }
    }
    @Test fun `mapping follows image offset rather than screen origin`() {
        val image = ArtworkRect(75f,120f,360f,432f)
        val r = HomeArtwork.region(HomeArtworkAction.GAMES,image)
        assertEquals(75f+360f*.16f,r.left,.001f)
        assertEquals(120f+432f*.86f,r.top,.001f)
    }
    @Test fun `empty image has no interactive regions`() { assertNull(HomeArtwork.actionAt(0f,0f,HomeArtwork.fit(0f,0f))) }
}
