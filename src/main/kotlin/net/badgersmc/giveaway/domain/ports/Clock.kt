package net.badgersmc.giveaway.domain.ports

import java.time.Instant

fun interface Clock {
    fun now(): Instant
}
