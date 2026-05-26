package net.badgersmc.giveaway.infrastructure.papi

import me.clip.placeholderapi.PlaceholderAPI
import net.badgersmc.giveaway.domain.ports.PlaceholderExpander
import org.bukkit.Bukkit
import java.util.UUID

/**
 * Optional PlaceholderAPI hook (REQ-008). Expands `%placeholder%` tokens
 * against the winner's `OfflinePlayer` context. Falls back to literal
 * `<player>` / `<name>` substitution if PAPI is not installed so the
 * command still runs correctly without the hook.
 */
class PlaceholderApiExpander : PlaceholderExpander {

    private val papiPresent: Boolean by lazy {
        Bukkit.getPluginManager().getPlugin("PlaceholderAPI")?.isEnabled == true
    }

    override fun expand(template: String, playerUuid: UUID, playerName: String): String {
        var result = template.replace("<player>", playerName).replace("<name>", playerName)
        if (papiPresent) {
            val player = Bukkit.getOfflinePlayer(playerUuid)
            result = PlaceholderAPI.setPlaceholders(player, result)
        }
        return result
    }
}
