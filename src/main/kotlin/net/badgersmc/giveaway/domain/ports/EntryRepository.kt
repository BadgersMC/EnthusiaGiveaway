package net.badgersmc.giveaway.domain.ports

import net.badgersmc.giveaway.domain.Entry
import net.badgersmc.giveaway.domain.GiveawayId
import java.util.UUID

interface EntryRepository {
    fun hasEntered(giveawayId: GiveawayId, playerUuid: UUID): Boolean
    fun insert(entry: Entry)
    fun playerUuidsFor(giveawayId: GiveawayId): List<UUID>
}
