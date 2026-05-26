package net.badgersmc.giveaway.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.badgersmc.giveaway.domain.Giveaway
import net.badgersmc.giveaway.domain.GiveawayState
import net.badgersmc.giveaway.domain.ports.Clock
import net.badgersmc.giveaway.domain.ports.GiveawayRepository
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * TDD-50 — RED test for ScheduleGiveaway (REQ-006).
 */
class ScheduleGiveawayTest {

    private val now = Instant.parse("2026-05-25T12:00:00Z")
    private val admin = UUID.randomUUID()

    private fun useCase(repo: GiveawayRepository): ScheduleGiveaway =
        ScheduleGiveaway(repo, mockk<Clock> { every { now() } returns now })

    @Test
    fun `valid input persists ACTIVE giveaway with endsAt = now + duration`() {
        val repo = mockk<GiveawayRepository>(relaxed = true)

        val result = useCase(repo).invoke(
            title = "Diamond drop",
            durationSeconds = 3600,
            command = "give <player> diamond",
            maxWinners = 2,
            adminUuid = admin,
        )

        val created = assertIs<ScheduleResult.Created>(result)
        assertEquals(GiveawayState.ACTIVE, created.giveaway.state)
        assertEquals(now, created.giveaway.scheduledAt)
        assertEquals(now.plusSeconds(3600), created.giveaway.endsAt)
        assertEquals("Diamond drop", created.giveaway.title)
        assertEquals("give <player> diamond", created.giveaway.command)
        assertEquals(2, created.giveaway.maxWinners)
        assertEquals(admin, created.giveaway.createdBy)
        verify(exactly = 1) { repo.save(any<Giveaway>()) }
    }

    @Test
    fun `blank title is rejected without persistence`() {
        val repo = mockk<GiveawayRepository>(relaxed = true)
        val result = useCase(repo).invoke("  ", 3600, "x", 1, admin)
        assertEquals(ScheduleResult.Invalid("title", "must not be blank"), result)
        verify(exactly = 0) { repo.save(any()) }
    }

    @Test
    fun `non-positive duration is rejected`() {
        val repo = mockk<GiveawayRepository>(relaxed = true)
        val result = useCase(repo).invoke("t", 0, "x", 1, admin)
        assertEquals(ScheduleResult.Invalid("durationSeconds", "must be > 0"), result)
        verify(exactly = 0) { repo.save(any()) }
    }

    @Test
    fun `zero or negative winners is rejected`() {
        val repo = mockk<GiveawayRepository>(relaxed = true)
        val result = useCase(repo).invoke("t", 60, "x", 0, admin)
        assertEquals(ScheduleResult.Invalid("maxWinners", "must be >= 1"), result)
        verify(exactly = 0) { repo.save(any()) }
    }
}
