package net.badgersmc.giveaway.infrastructure.bukkit

import net.badgersmc.giveaway.domain.ports.Clock
import java.time.Instant

class BukkitClock : Clock {
    override fun now(): Instant = Instant.now()
}
