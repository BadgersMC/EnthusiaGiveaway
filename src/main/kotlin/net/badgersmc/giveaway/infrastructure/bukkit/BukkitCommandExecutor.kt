package net.badgersmc.giveaway.infrastructure.bukkit

import net.badgersmc.giveaway.domain.ports.CommandExecutor
import org.bukkit.Bukkit

class BukkitCommandExecutor : CommandExecutor {
    override fun dispatch(commandLine: String) {
        Bukkit.getScheduler().runTask(
            Bukkit.getPluginManager().getPlugin("EnthusiaGiveaway")!!,
            Runnable { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandLine) }
        )
    }
}
