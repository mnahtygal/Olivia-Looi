package com.nahtygal.olivialooi.games.billiards

import org.junit.Assert.*
import org.junit.Test

class BilliardsRackStyleTest {
    private val table = BilliardsPhysics.rack()
    private val rows = table.balls.filter { it.id != BallId.CUE }.groupBy { it.position.y }.toSortedMap().values.toList()

    @Test fun `apex faces cue and wide row is farthest away`() {
        val cue = table.balls.first { it.id == BallId.CUE }
        assertEquals(Vec2(300.0, 780.0), cue.position)
        val apex = rows.last().single()
        assertEquals(BallId.ONE, apex.id)
        assertEquals(5, rows.first().size)
        table.balls.filter { it.id != BallId.CUE && it.id != apex.id }.forEach {
            assertTrue((apex.position - cue.position).length() < (it.position - cue.position).length())
        }
        assertTrue(rows.first().all { it.position.y < apex.position.y })
    }
    @Test fun `rack ordering is mixed and repeatable`() {
        assertEquals(listOf(listOf(2,9,4,12,7),listOf(10,5,13,6),listOf(3,8,14),listOf(11,15),listOf(1)),
            rows.map { row -> row.sortedBy { it.position.x }.map { it.id.number } })
        assertEquals(table, BilliardsPhysics.rack())
        listOf(BilliardsMode.FREE_PLAY, BilliardsMode.VERSUS).forEach {
            assertEquals(table, BilliardsEngine.rack(BilliardsEngine.newGame(it)).table)
        }
    }
    @Test fun `eight occupies interior center of middle row`() {
        val middle = rows[2].sortedBy { it.position.x }
        assertEquals(BallId.EIGHT, middle[1].id)
        assertEquals(Vec2(300.0,324.0), middle[1].position)
        assertTrue(middle.first().position.x < middle[1].position.x)
        assertTrue(middle.last().position.x > middle[1].position.x)
        assertTrue(rows.first().first().position.y < middle[1].position.y)
        assertTrue(rows.last().first().position.y > middle[1].position.y)
    }
    @Test fun `one through seven are solids and eight is black`() {
        BallId.entries.filter { it.number in 1..7 }.forEach { assertEquals(BallStyle.SOLID,it.style) }
        assertEquals(BallStyle.EIGHT,BallId.EIGHT.style)
        assertEquals("black",BallId.EIGHT.visualIdentity)
        assertEquals(BallStyle.CUE,BallId.CUE.style)
    }
    @Test fun `nine through fifteen are stripes with matching color families`() {
        BallId.entries.filter { it.number in 9..15 }.forEach {
            assertEquals(BallStyle.STRIPE,it.style)
            assertEquals(BallId.entries.first { solid -> solid.number == it.number - 8 }.visualIdentity,it.visualIdentity)
        }
    }
    @Test fun `style and readable number metadata are deterministic`() {
        val styles = BallId.entries.map { it.style }
        repeat(20) { assertEquals(styles,BallId.entries.map { it.style }) }
        assertEquals((0..15).toList(),BallId.entries.map { it.number })
        BallId.entries.filter { it != BallId.CUE }.forEach { assertEquals("${it.number} ball",it.description) }
    }
}
