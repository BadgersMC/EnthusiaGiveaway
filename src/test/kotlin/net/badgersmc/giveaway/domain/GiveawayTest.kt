package net.badgersmc.giveaway.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

/**
 * TDD-10 — RED test for Giveaway state transitions (REQ-007, REQ-013).
 *
 * Legal forward path:  SCHEDULED → ACTIVE → DRAWING → COMPLETED
 * Legal cancel path:   SCHEDULED, ACTIVE → CANCELLED
 * Illegal everything else throws IllegalStateException.
 *
 * Terminal states (COMPLETED, CANCELLED) reject every transition.
 */
class GiveawayTest {

    private fun fixture(state: GiveawayState): Giveaway = Giveaway(
        id = GiveawayId(UUID.randomUUID()),
        title = "Test giveaway",
        command = "say <player> won",
        scheduledAt = Instant.parse("2026-05-25T00:00:00Z"),
        endsAt = Instant.parse("2026-05-25T01:00:00Z"),
        maxWinners = 1,
        state = state,
        createdBy = UUID.randomUUID(),
    )

    @Test
    fun `scheduled transitions to active`() {
        val g = fixture(GiveawayState.SCHEDULED).transition(GiveawayState.ACTIVE)
        assertEquals(GiveawayState.ACTIVE, g.state)
    }

    @Test
    fun `active transitions to drawing`() {
        val g = fixture(GiveawayState.ACTIVE).transition(GiveawayState.DRAWING)
        assertEquals(GiveawayState.DRAWING, g.state)
    }

    @Test
    fun `drawing transitions to completed`() {
        val g = fixture(GiveawayState.DRAWING).transition(GiveawayState.COMPLETED)
        assertEquals(GiveawayState.COMPLETED, g.state)
    }

    @Test
    fun `scheduled can be cancelled`() {
        val g = fixture(GiveawayState.SCHEDULED).transition(GiveawayState.CANCELLED)
        assertEquals(GiveawayState.CANCELLED, g.state)
    }

    @Test
    fun `active can be cancelled`() {
        val g = fixture(GiveawayState.ACTIVE).transition(GiveawayState.CANCELLED)
        assertEquals(GiveawayState.CANCELLED, g.state)
    }

    @Test
    fun `scheduled cannot jump straight to drawing`() {
        assertThrows<IllegalStateException> {
            fixture(GiveawayState.SCHEDULED).transition(GiveawayState.DRAWING)
        }
    }

    @Test
    fun `active cannot jump straight to completed`() {
        assertThrows<IllegalStateException> {
            fixture(GiveawayState.ACTIVE).transition(GiveawayState.COMPLETED)
        }
    }

    @Test
    fun `drawing cannot be cancelled`() {
        assertThrows<IllegalStateException> {
            fixture(GiveawayState.DRAWING).transition(GiveawayState.CANCELLED)
        }
    }

    @Test
    fun `completed is terminal`() {
        assertThrows<IllegalStateException> {
            fixture(GiveawayState.COMPLETED).transition(GiveawayState.ACTIVE)
        }
    }

    @Test
    fun `cancelled is terminal`() {
        assertThrows<IllegalStateException> {
            fixture(GiveawayState.CANCELLED).transition(GiveawayState.ACTIVE)
        }
    }
}
