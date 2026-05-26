package net.badgersmc.giveaway.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.badgersmc.giveaway.domain.Giveaway
import net.badgersmc.giveaway.domain.GiveawayId
import net.badgersmc.giveaway.domain.GiveawayState
import net.badgersmc.giveaway.domain.ports.Clock
import net.badgersmc.giveaway.domain.ports.EntryRepository
import net.badgersmc.giveaway.domain.ports.GiveawayRepository
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * TDD-20 — RED test for EnterGiveaway use case (REQ-003, REQ-004).
 *
 * Constraints:
 * - First entry against an ACTIVE giveaway succeeds and persists exactly one Entry.
 * - Repeated entry by the same player returns AlreadyEntered and never re-inserts.
 * - Entering a giveaway that is not ACTIVE returns NotActive.
 * - Entering a non-existent giveaway returns GiveawayNotFound.
 */
class EnterGiveawayTest {

    private val giveawayId = GiveawayId(UUID.randomUUID())
    private val playerId = UUID.randomUUID()
    private val now = Instant.parse("2026-05-25T12:00:00Z")

    private fun active(): Giveaway = Giveaway(
        id = giveawayId,
        title = "x",
        command = "say hi",
        scheduledAt = now,
        endsAt = now.plusSeconds(3600),
        maxWinners = 1,
        state = GiveawayState.ACTIVE,
        createdBy = UUID.randomUUID(),
    )

    private fun wire(giveaway: Giveaway?, alreadyEntered: Boolean): EnterGiveaway {
        val giveaways = mockk<GiveawayRepository>()
        val entries = mockk<EntryRepository>(relaxed = true)
        val clock = mockk<Clock>()
        every { giveaways.findById(giveawayId) } returns giveaway
        every { entries.hasEntered(giveawayId, playerId) } returns alreadyEntered
        every { clock.now() } returns now
        return EnterGiveaway(giveaways, entries, clock)
    }

    @Test
    fun `first entry against an active giveaway succeeds`() {
        val giveaway = active()
        val entries = mockk<EntryRepository>(relaxed = true)
        val giveaways = mockk<GiveawayRepository> { every { findById(giveawayId) } returns giveaway }
        val clock = mockk<Clock> { every { now() } returns now }
        every { entries.hasEntered(giveawayId, playerId) } returns false

        val result = EnterGiveaway(giveaways, entries, clock).invoke(giveawayId, playerId)

        assertIs<EnterResult.Success>(result)
        verify(exactly = 1) { entries.insert(match { it.giveawayId == giveawayId && it.playerUuid == playerId && it.enteredAt == now }) }
    }

    @Test
    fun `second entry by same player returns AlreadyEntered and does not re-insert`() {
        val giveaway = active()
        val entries = mockk<EntryRepository>(relaxed = true)
        val giveaways = mockk<GiveawayRepository> { every { findById(giveawayId) } returns giveaway }
        val clock = mockk<Clock> { every { now() } returns now }
        every { entries.hasEntered(giveawayId, playerId) } returns true

        val result = EnterGiveaway(giveaways, entries, clock).invoke(giveawayId, playerId)

        assertEquals(EnterResult.AlreadyEntered, result)
        verify(exactly = 0) { entries.insert(any()) }
    }

    @Test
    fun `unknown giveaway returns GiveawayNotFound`() {
        val useCase = wire(giveaway = null, alreadyEntered = false)

        val result = useCase.invoke(giveawayId, playerId)

        assertEquals(EnterResult.GiveawayNotFound, result)
    }

    @Test
    fun `non-active giveaway returns NotActive`() {
        val scheduled = active().copy(state = GiveawayState.SCHEDULED)
        val useCase = wire(giveaway = scheduled, alreadyEntered = false)

        val result = useCase.invoke(giveawayId, playerId)

        assertEquals(EnterResult.NotActive, result)
    }
}
