package net.badgersmc.giveaway.domain.ports

import net.badgersmc.giveaway.domain.Giveaway
import net.badgersmc.giveaway.domain.WinnerHandle

interface CelebrationBroadcaster {
    fun announce(giveaway: Giveaway, winners: List<WinnerHandle>)
    fun notifyCancellation(giveaway: Giveaway)
    fun notifyNew(giveaway: Giveaway)
}
