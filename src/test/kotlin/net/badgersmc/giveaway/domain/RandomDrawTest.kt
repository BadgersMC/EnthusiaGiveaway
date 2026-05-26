package net.badgersmc.giveaway.domain

import net.badgersmc.giveaway.domain.ports.RandomDraw
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD-12 — RED test for RandomDraw (REQ-007, REQ-012, REQ-102).
 *
 * Behaviours pinned:
 * - Picking k from N where N >= k returns exactly k distinct elements drawn from the source.
 * - Picking k from N where N < k returns all N elements (REQ-012 "fewer entries than max winners").
 * - Picking from an empty source returns an empty list.
 * - Same seed yields the same picks (determinism for tests and resume-on-restart).
 */
class RandomDrawTest {

    @Test
    fun `pick three from one hundred returns three distinct entries from source`() {
        val source = (1..100).toList()
        val draw: RandomDraw = SeededRandomDraw(seed = 42L)

        val picked = draw.pick(k = 3, from = source)

        assertEquals(3, picked.size)
        assertEquals(3, picked.toSet().size, "winners must be distinct")
        assertTrue(picked.all { it in source }, "winners must come from the source")
    }

    @Test
    fun `pick five from two returns all two`() {
        val source = listOf("a", "b")
        val draw: RandomDraw = SeededRandomDraw(seed = 1L)

        val picked = draw.pick(k = 5, from = source)

        assertEquals(setOf("a", "b"), picked.toSet())
        assertEquals(2, picked.size)
    }

    @Test
    fun `pick from empty source returns empty`() {
        val draw: RandomDraw = SeededRandomDraw(seed = 1L)

        assertEquals(emptyList(), draw.pick(k = 3, from = emptyList<String>()))
    }

    @Test
    fun `same seed yields same picks`() {
        val source = (1..50).toList()
        val a = SeededRandomDraw(seed = 7L).pick(3, source)
        val b = SeededRandomDraw(seed = 7L).pick(3, source)

        assertEquals(a, b)
    }
}
