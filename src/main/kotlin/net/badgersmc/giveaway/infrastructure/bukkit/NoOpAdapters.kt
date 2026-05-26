package net.badgersmc.giveaway.infrastructure.bukkit

import net.badgersmc.giveaway.domain.Giveaway
import net.badgersmc.giveaway.domain.GiveawayId
import net.badgersmc.giveaway.domain.Winner
import net.badgersmc.giveaway.domain.WinnerHandle
import net.badgersmc.giveaway.domain.ports.CelebrationBroadcaster
import net.badgersmc.giveaway.domain.ports.PlaceholderExpander
import net.badgersmc.giveaway.domain.ports.WinnerRepository
import java.util.UUID

/**
 * Temporary no-op adapters for ports not yet implemented. Replaced as their
 * tasks land:
 *   - NoOpPlaceholderExpander → INFRA-12 (PlaceholderApiExpander)
 *   - NoOpCelebrationBroadcaster → INFRA-15 (BukkitCelebrationBroadcaster)
 *   - InMemoryWinnerRepository → TDD-56 (ExposedWinnerRepository)
 *
 * Keeping these here means the DI graph wires cleanly during M1 even though
 * the full feature surface isn't built yet. None of them satisfy the
 * requirements they front — they exist only to let onEnable boot.
 */

class NoOpPlaceholderExpander : PlaceholderExpander {
    override fun expand(template: String, playerUuid: UUID, playerName: String): String =
        template.replace("<player>", playerName).replace("<name>", playerName)
}

class NoOpCelebrationBroadcaster : CelebrationBroadcaster {
    override fun announce(giveaway: Giveaway, winners: List<WinnerHandle>) { /* M3 will implement */ }
}

class InMemoryWinnerRepository : WinnerRepository {
    private val winners = mutableSetOf<Pair<GiveawayId, UUID>>()
    override fun insert(winner: Winner) {
        winners.add(winner.giveawayId to winner.playerUuid)
    }
}
