package net.badgersmc.giveaway.infrastructure.bukkit

import net.badgersmc.giveaway.infrastructure.menus.AdminGiveawayMenu
import net.badgersmc.giveaway.infrastructure.menus.PlayerGiveawayMenu
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class GiveawayCommand(
    private val playerMenu: PlayerGiveawayMenu,
    private val adminMenu: AdminGiveawayMenu,
) : TabExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can run this command.")
            return true
        }

        when (args.firstOrNull()?.lowercase()) {
            null -> {
                if (!sender.hasPermission("enthusiagiveaway.use")) {
                    sender.sendMessage("§cYou don't have permission.")
                    return true
                }
                playerMenu.open(sender)
            }
            "admin" -> {
                if (!sender.hasPermission("enthusiagiveaway.admin")) {
                    sender.sendMessage("§cYou don't have permission.")
                    return true
                }
                adminMenu.open(sender)
            }
            else -> sender.sendMessage("§cUsage: /giveaway [admin]")
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> = when {
        args.size == 1 && sender.hasPermission("enthusiagiveaway.admin") ->
            listOf("admin").filter { it.startsWith(args[0], ignoreCase = true) }
        else -> emptyList()
    }
}
