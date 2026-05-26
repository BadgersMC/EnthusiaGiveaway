package net.badgersmc.giveaway.application

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

class DrawWinners(
    private val giveaways: GiveawayRepository,
    private val entries: EntryRepository,
    private val winners: WinnerRepository,
    private val draw: RandomDraw,
    private val nameLookup: PlayerNameLookup,
    private val placeholders: PlaceholderExpander,
    private val commands: CommandExecutor,
    private val celebration: CelebrationBroadcaster,
    private val logger: Logger,
    private val clock: Clock,
) {
    operator fun invoke(giveawayId: GiveawayId): DrawResult {
        val giveaway = giveaways.findById(giveawayId) ?: return DrawResult.NotFound
        if (giveaway.state != GiveawayState.ACTIVE) return DrawResult.NotActive

        val drawing = giveaway.transition(GiveawayState.DRAWING)
        giveaways.save(drawing)

        val entryUuids = entries.playerUuidsFor(giveawayId)
        val picked = draw.pick(giveaway.maxWinners, entryUuids)
        val now = clock.now()

        for (uuid in picked) {
            winners.insert(Winner(giveawayId, uuid, now))
        }

        val handles = picked.map { WinnerHandle(it, nameLookup.nameOf(it)) }

        if (giveaway.command.isBlank()) {
            logger.warn("Giveaway ${giveawayId.value} has empty command; skipping dispatch")
        } else {
            for (handle in handles) {
                val expanded = placeholders.expand(giveaway.command, handle.uuid, handle.name)
                commands.dispatch(expanded)
            }
        }

        celebration.announce(drawing, handles)

        giveaways.save(drawing.transition(GiveawayState.COMPLETED))
        logger.info("Drew ${picked.size} winners for giveaway ${giveawayId.value}")

        return DrawResult.Drawn(picked.size)
    }
}
