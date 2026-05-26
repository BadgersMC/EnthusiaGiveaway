package net.badgersmc.giveaway.domain.ports

import java.util.UUID

fun interface PlaceholderExpander {
    fun expand(template: String, playerUuid: UUID, playerName: String): String
}
