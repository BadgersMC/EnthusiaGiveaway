package net.badgersmc.giveaway.domain

enum class GiveawayState {
    SCHEDULED,
    ACTIVE,
    DRAWING,
    COMPLETED,
    CANCELLED;

    fun canTransitionTo(next: GiveawayState): Boolean = when (this) {
        SCHEDULED -> next == ACTIVE || next == CANCELLED
        ACTIVE -> next == DRAWING || next == CANCELLED
        DRAWING -> next == COMPLETED
        COMPLETED, CANCELLED -> false
    }
}
