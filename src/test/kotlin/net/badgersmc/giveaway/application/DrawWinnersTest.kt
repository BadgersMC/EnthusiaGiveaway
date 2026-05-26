package net.badgersmc.giveaway.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import net.badgersmc.giveaway.domain.Giveaway
import net.badgersmc.giveaway.domain.GiveawayId
import net.badgersmc.giveaway.domain.GiveawayState
import net.badgersmc.giveaway.domain.Winner
import net.badgersmc.giveaway.domain.WinnerHandle
import net.badgersmc.giveaway.domain.ports.CelebrationBroadcaster
import net.badgersmc.giveaway.domain.ports.Clock
import net.badgersmc.giveaway.domain.ports.CommandExecutor
import net.badgersmc.giveaway.domain.ports.EntryRepository
import net.badgersmc.giveaway.domain.ports.GiveawayRepository
import net.badgersmc.giveaway.domain.ports.Logger
import net.badgersmc.giveaway.domain.ports.PlaceholderExpander
import net.badgersmc.giveaway.domain.ports.PlayerNameLookup
import net.badgersmc.giveaway.domain.ports.RandomDraw
import net.badgersmc.giveaway.domain.ports.WinnerRepository
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * TDD-22 — RED test for DrawWinners use case
 * (REQ-007 draw on expiry, REQ-008 command per winner, REQ-009 celebration,
 *  REQ-010 empty-command guard, REQ-012 fewer entries than max winners).
 */
class DrawWinnersTest {

    private val giveawayId = GiveawayId(UUID.randomUUID())
    private val now = Instant.parse("2026-05-25T12:00:00Z")
    private val alice = UUID.randomUUID()
    private val bob = UUID.randomUUID()
    private val carol = UUID.randomUUID()

    private fun activeGiveaway(command: String = "give <player> diamond"): Giveaway = Giveaway(
        id = giveawayId,
        title = "Diamond drop",
        command = command,
        scheduledAt = now.minusSeconds(3600),
        endsAt = now.minusSeconds(1),
        maxWinners = 2,
        state = GiveawayState.ACTIVE,
        createdBy = UUID.randomUUID(),
    )

    @Test
    fun `happy path orchestrates draw, command, celebration and final state`() {
        val giveaway = activeGiveaway()
        val drawing = giveaway.copy(state = GiveawayState.DRAWING)
        val completed = drawing.copy(state = GiveawayState.COMPLETED)

        val giveaways = mockk<GiveawayRepository>(relaxed = true) {
            every { findById(giveawayId) } returns giveaway
        }
        val entries = mockk<EntryRepository>(relaxed = true) {
            every { playerUuidsFor(giveawayId) } returns listOf(alice, bob, carol)
        }
        val winners = mockk<WinnerRepository>(relaxed = true)
        val draw = mockk<RandomDraw> {
            every { pick(2, listOf(alice, bob, carol)) } returns listOf(alice, bob)
        }
        val names = mockk<PlayerNameLookup> {
            every { nameOf(alice) } returns "Alice"
            every { nameOf(bob) } returns "Bob"
        }
        val placeholders = mockk<PlaceholderExpander> {
            every { expand("give <player> diamond", alice, "Alice") } returns "give Alice diamond"
            every { expand("give <player> diamond", bob, "Bob") } returns "give Bob diamond"
        }
        val commands = mockk<CommandExecutor>(relaxed = true)
        val celebration = mockk<CelebrationBroadcaster>(relaxed = true)
        val logger = mockk<Logger>(relaxed = true)
        val clock = mockk<Clock> { every { now() } returns now }

        val result = DrawWinners(
            giveaways, entries, winners, draw, names, placeholders, commands, celebration, logger, clock
        ).invoke(giveawayId)

        assertIs<DrawResult.Drawn>(result)
        assertEquals(2, result.winnerCount)

        verifyOrder {
            giveaways.findById(giveawayId)
            giveaways.save(match { it.state == GiveawayState.DRAWING })
            entries.playerUuidsFor(giveawayId)
            draw.pick(2, listOf(alice, bob, carol))
            winners.insert(Winner(giveawayId, alice, now))
            winners.insert(Winner(giveawayId, bob, now))
            commands.dispatch("give Alice diamond")
            commands.dispatch("give Bob diamond")
            celebration.announce(
                drawing,
                listOf(WinnerHandle(alice, "Alice"), WinnerHandle(bob, "Bob"))
            )
            giveaways.save(match { it.state == GiveawayState.COMPLETED })
        }
    }

    @Test
    fun `empty command skips dispatch, logs warning, still celebrates`() {
        val giveaway = activeGiveaway(command = "   ")
        val giveaways = mockk<GiveawayRepository>(relaxed = true) {
            every { findById(giveawayId) } returns giveaway
        }
        val entries = mockk<EntryRepository>(relaxed = true) {
            every { playerUuidsFor(giveawayId) } returns listOf(alice)
        }
        val winners = mockk<WinnerRepository>(relaxed = true)
        val draw = mockk<RandomDraw> { every { pick(2, listOf(alice)) } returns listOf(alice) }
        val names = mockk<PlayerNameLookup> { every { nameOf(alice) } returns "Alice" }
        val placeholders = mockk<PlaceholderExpander>(relaxed = true)
        val commands = mockk<CommandExecutor>(relaxed = true)
        val celebration = mockk<CelebrationBroadcaster>(relaxed = true)
        val logger = mockk<Logger>(relaxed = true)
        val clock = mockk<Clock> { every { now() } returns now }

        DrawWinners(
            giveaways, entries, winners, draw, names, placeholders, commands, celebration, logger, clock
        ).invoke(giveawayId)

        verify(exactly = 0) { commands.dispatch(any()) }
        verify(exactly = 0) { placeholders.expand(any(), any(), any()) }
        verify(atLeast = 1) { logger.warn(any()) }
        verify(exactly = 1) { celebration.announce(any(), any()) }
    }
}
