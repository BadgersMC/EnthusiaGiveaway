package net.badgersmc.giveaway.application

import net.badgersmc.giveaway.domain.GiveawayId
import net.badgersmc.giveaway.domain.GiveawayState
import net.badgersmc.giveaway.domain.ports.CelebrationBroadcaster
import net.badgersmc.giveaway.domain.ports.GiveawayRepository

class CancelGiveaway(
    private val giveaways: GiveawayRepository,
    private val celebration: CelebrationBroadcaster,
) {
    operator fun invoke(id: GiveawayId): CancelResult {
        val g = giveaways.findById(id) ?: return CancelResult.NotFound
        if (g.state != GiveawayState.SCHEDULED && g.state != GiveawayState.ACTIVE) {
            return CancelResult.AlreadyFinal
        }
        val cancelled = g.transition(GiveawayState.CANCELLED)
        giveaways.save(cancelled)
        celebration.notifyCancellation(cancelled)
        return CancelResult.Cancelled
    }
}
