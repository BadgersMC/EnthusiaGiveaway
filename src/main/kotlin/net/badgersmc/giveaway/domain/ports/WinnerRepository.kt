package net.badgersmc.giveaway.domain.ports

import net.badgersmc.giveaway.domain.Winner

interface WinnerRepository {
    fun insert(winner: Winner)
}
