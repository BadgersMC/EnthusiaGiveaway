package net.badgersmc.giveaway.domain.ports

import java.util.UUID

fun interface PlayerNameLookup {
    fun nameOf(uuid: UUID): String
}
