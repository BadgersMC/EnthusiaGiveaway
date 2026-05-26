package net.badgersmc.giveaway.application

import io.mockk.every
import io.mockk.mockk
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
import kotlin.test.assertTrue

/**
 * TDD-44 — RED test for ListActiveGiveaways (REQ-002).
 */
class ListActiveGiveawaysTest {

    private val now = Instant.parse("2026-05-25T12:00:00Z")
    private val viewer = UUID.randomUUID()

    private fun active(secondsRemaining: Long, id: GiveawayId = GiveawayId(UUID.randomUUID())) =
        Giveaway(
            id = id,
            title = "G-${id.value}",
            command = "say <player>",
            scheduledAt = now,
            endsAt = now.plusSeconds(secondsRemaining),
            maxWinners = 1,
            state = GiveawayState.ACTIVE,
            createdBy = UUID.randomUUID(),
        )

    @Test
    fun `returns summary per active giveaway with seconds remaining, entry count, and entered flag`() {
        val gA = active(secondsRemaining = 120)
        val gB = active(secondsRemaining = 30)

        val giveaways = mockk<GiveawayRepository> {
            every { listByState(GiveawayState.ACTIVE) } returns listOf(gA, gB)
        }
        val entries = mockk<EntryRepository> {
            every { playerUuidsFor(gA.id) } returns listOf(UUID.randomUUID(), UUID.randomUUID())
            every { playerUuidsFor(gB.id) } returns emptyList()
            every { hasEntered(gA.id, viewer) } returns true
            every { hasEntered(gB.id, viewer) } returns false
        }
        val clock = mockk<Clock> { every { now() } returns now }

        val summaries = ListActiveGiveaways(giveaways, entries, clock).invoke(viewer)

        assertEquals(2, summaries.size)
        val a = summaries.single { it.id == gA.id }
        assertEquals(120, a.secondsRemaining)
        assertEquals(2, a.entryCount)
        assertTrue(a.alreadyEntered)

        val b = summaries.single { it.id == gB.id }
        assertEquals(30, b.secondsRemaining)
        assertEquals(0, b.entryCount)
        assertEquals(false, b.alreadyEntered)
    }

    @Test
    fun `clamps secondsRemaining at zero for already-expired giveaways`() {
        val expired = active(secondsRemaining = -5)
        val giveaways = mockk<GiveawayRepository> {
            every { listByState(GiveawayState.ACTIVE) } returns listOf(expired)
        }
        val entries = mockk<EntryRepository> {
            every { playerUuidsFor(expired.id) } returns emptyList()
            every { hasEntered(expired.id, viewer) } returns false
        }
        val clock = mockk<Clock> { every { now() } returns now }

        val summary = ListActiveGiveaways(giveaways, entries, clock).invoke(viewer).single()
        assertEquals(0, summary.secondsRemaining)
    }
}
