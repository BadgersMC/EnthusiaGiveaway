package net.badgersmc.giveaway.domain

import java.time.Instant
import java.util.UUID

data class Giveaway(
    val id: GiveawayId,
    val title: String,
    val command: String,
    val scheduledAt: Instant,
    val endsAt: Instant,
    val maxWinners: Int,
    val state: GiveawayState,
    val createdBy: UUID,
) {
    fun transition(to: GiveawayState): Giveaway {
        check(state.canTransitionTo(to)) {
            "Illegal Giveaway state transition: $state -> $to"
        }
        return copy(state = to)
    }
}
