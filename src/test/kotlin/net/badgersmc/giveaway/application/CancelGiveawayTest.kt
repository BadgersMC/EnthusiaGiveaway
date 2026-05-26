package net.badgersmc.giveaway.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.badgersmc.giveaway.domain.Giveaway
import net.badgersmc.giveaway.domain.GiveawayId
import net.badgersmc.giveaway.domain.GiveawayState
import net.badgersmc.giveaway.domain.ports.CelebrationBroadcaster
import net.badgersmc.giveaway.domain.ports.GiveawayRepository
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

/**
 * TDD-52 — RED test for CancelGiveaway (REQ-013).
 */
class CancelGiveawayTest {

    private val giveawayId = GiveawayId(UUID.randomUUID())

    private fun fixture(state: GiveawayState): Giveaway = Giveaway(
        id = giveawayId,
        title = "t",
        command = "c",
        scheduledAt = Instant.parse("2026-05-25T12:00:00Z"),
        endsAt = Instant.parse("2026-05-25T13:00:00Z"),
        maxWinners = 1,
        state = state,
        createdBy = UUID.randomUUID(),
    )

    @Test
    fun `cancelling an ACTIVE giveaway transitions to CANCELLED and notifies`() {
        val g = fixture(GiveawayState.ACTIVE)
        val repo = mockk<GiveawayRepository>(relaxed = true) {
            every { findById(giveawayId) } returns g
        }
        val celebration = mockk<CelebrationBroadcaster>(relaxed = true)

        val result = CancelGiveaway(repo, celebration).invoke(giveawayId)

        assertEquals(CancelResult.Cancelled, result)
        verify(exactly = 1) { repo.save(match { it.state == GiveawayState.CANCELLED }) }
        verify(exactly = 1) { celebration.notifyCancellation(match { it.state == GiveawayState.CANCELLED }) }
    }

    @Test
    fun `cancelling a SCHEDULED giveaway is allowed`() {
        val g = fixture(GiveawayState.SCHEDULED)
        val repo = mockk<GiveawayRepository>(relaxed = true) {
            every { findById(giveawayId) } returns g
        }
        val celebration = mockk<CelebrationBroadcaster>(relaxed = true)

        val result = CancelGiveaway(repo, celebration).invoke(giveawayId)
        assertEquals(CancelResult.Cancelled, result)
    }

    @Test
    fun `cancelling a COMPLETED giveaway returns AlreadyFinal and does not transition`() {
        val g = fixture(GiveawayState.COMPLETED)
        val repo = mockk<GiveawayRepository>(relaxed = true) {
            every { findById(giveawayId) } returns g
        }
        val celebration = mockk<CelebrationBroadcaster>(relaxed = true)

        val result = CancelGiveaway(repo, celebration).invoke(giveawayId)
        assertEquals(CancelResult.AlreadyFinal, result)
        verify(exactly = 0) { repo.save(any<Giveaway>()) }
        verify(exactly = 0) { celebration.notifyCancellation(any()) }
    }

    @Test
    fun `cancelling unknown id returns NotFound`() {
        val repo = mockk<GiveawayRepository>(relaxed = true) {
            every { findById(giveawayId) } returns null
        }
        val celebration = mockk<CelebrationBroadcaster>(relaxed = true)

        assertEquals(CancelResult.NotFound, CancelGiveaway(repo, celebration).invoke(giveawayId))
    }
}
