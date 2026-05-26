package net.badgersmc.giveaway.infrastructure.bukkit

import net.badgersmc.giveaway.domain.ports.PlayerNameLookup
import org.bukkit.Bukkit
import java.util.UUID

class BukkitNameLookup : PlayerNameLookup {
    @Suppress("DEPRECATION") // OfflinePlayer#getName may return null for never-seen UUIDs
    override fun nameOf(uuid: UUID): String =
        Bukkit.getOfflinePlayer(uuid).name ?: "Unknown"
}
