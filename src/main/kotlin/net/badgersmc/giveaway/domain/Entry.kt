package net.badgersmc.giveaway.domain

import java.time.Instant
import java.util.UUID

data class Entry(
    val giveawayId: GiveawayId,
    val playerUuid: UUID,
    val enteredAt: Instant,
)
