package net.badgersmc.giveaway.application

import net.badgersmc.giveaway.domain.Entry
import net.badgersmc.giveaway.domain.GiveawayId
import net.badgersmc.giveaway.domain.GiveawayState
import net.badgersmc.giveaway.domain.ports.Clock
import net.badgersmc.giveaway.domain.ports.EntryRepository
import net.badgersmc.giveaway.domain.ports.GiveawayRepository
import java.util.UUID

class EnterGiveaway(
    private val giveaways: GiveawayRepository,
    private val entries: EntryRepository,
    private val clock: Clock,
) {
    operator fun invoke(giveawayId: GiveawayId, playerUuid: UUID): EnterResult {
        val giveaway = giveaways.findById(giveawayId) ?: return EnterResult.GiveawayNotFound
        if (giveaway.state != GiveawayState.ACTIVE) return EnterResult.NotActive
        if (entries.hasEntered(giveawayId, playerUuid)) return EnterResult.AlreadyEntered
        entries.insert(Entry(giveawayId, playerUuid, clock.now()))
        return EnterResult.Success
    }
}
