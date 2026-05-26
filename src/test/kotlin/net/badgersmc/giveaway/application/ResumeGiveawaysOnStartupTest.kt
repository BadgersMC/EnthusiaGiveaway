package net.badgersmc.giveaway.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.badgersmc.giveaway.domain.Giveaway
import net.badgersmc.giveaway.domain.GiveawayId
import net.badgersmc.giveaway.domain.GiveawayState
import net.badgersmc.giveaway.domain.ports.Clock
import net.badgersmc.giveaway.domain.ports.GiveawayRepository
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * TDD-54 — RED test for ResumeGiveawaysOnStartup (REQ-011).
 */
class ResumeGiveawaysOnStartupTest {

    private val now = Instant.parse("2026-05-25T12:00:00Z")

    private fun g(state: GiveawayState, endsAtOffsetSeconds: Long): Giveaway = Giveaway(
        id = GiveawayId(UUID.randomUUID()),
        title = "g",
        command = "x",
        scheduledAt = now.minusSeconds(3600),
        endsAt = now.plusSeconds(endsAtOffsetSeconds),
        maxWinners = 1,
        state = state,
        createdBy = UUID.randomUUID(),
    )

    @Test
    fun `dispatches draw for DRAWING rows and expired ACTIVE rows, leaves future ACTIVE alone`() {
        val drawingExpired = g(GiveawayState.DRAWING, endsAtOffsetSeconds = -100)
        val activeExpired = g(GiveawayState.ACTIVE, endsAtOffsetSeconds = -10)
        val activeFuture = g(GiveawayState.ACTIVE, endsAtOffsetSeconds = +500)
        val scheduledFuture = g(GiveawayState.SCHEDULED, endsAtOffsetSeconds = +1000)

        val repo = mockk<GiveawayRepository> {
            every { listByState(GiveawayState.DRAWING) } returns listOf(drawingExpired)
            every { listByState(GiveawayState.ACTIVE) } returns listOf(activeExpired, activeFuture)
            every { listByState(GiveawayState.SCHEDULED) } returns listOf(scheduledFuture)
        }
        val draws = mutableListOf<GiveawayId>()
        val clock = mockk<Clock> { every { now() } returns now }

        ResumeGiveawaysOnStartup(repo, { draws.add(it) }, clock).invoke()

        assert(drawingExpired.id in draws)
        assert(activeExpired.id in draws)
        assert(activeFuture.id !in draws)
        assert(scheduledFuture.id !in draws)
        verify(exactly = 0) { repo.listByState(GiveawayState.SCHEDULED) }
    }
}
