package net.badgersmc.giveaway.domain

import java.time.Instant
import java.util.UUID

data class Winner(
    val giveawayId: GiveawayId,
    val playerUuid: UUID,
    val drawnAt: Instant,
)
