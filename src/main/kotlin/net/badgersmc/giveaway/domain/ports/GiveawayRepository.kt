package net.badgersmc.giveaway.domain.ports

import net.badgersmc.giveaway.domain.Giveaway
import net.badgersmc.giveaway.domain.GiveawayId

import net.badgersmc.giveaway.domain.GiveawayState

interface GiveawayRepository {
    fun findById(id: GiveawayId): Giveaway?
    fun save(giveaway: Giveaway)
    fun listByState(state: GiveawayState): List<Giveaway>
}
