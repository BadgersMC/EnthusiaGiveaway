package net.badgersmc.giveaway.infrastructure.bukkit

import org.bukkit.command.Command
import org.bukkit.command.CommandSender

/**
 * Thin Bukkit [Command] adapter that delegates to [GiveawayCommand] (a [TabExecutor]).
 *
 * Paper plugins cannot call [org.bukkit.plugin.java.JavaPlugin.getCommand]
 * when commands are declared in `paper-plugin.yml`.  We bypass that by
 * creating our own [Command] and registering it on the server's [CommandMap]
 * directly.  Permissions are still declared in `paper-plugin.yml` and
 * honoured by Paper's permission system.
 */
class GiveawayBukkitCommand(
    private val executor: GiveawayCommand,
) : Command(
    "giveaway",
    "Open the giveaway menu (or admin menu with /giveaway admin)",
    "/giveaway [admin]",
    emptyList(),
) {

    override fun execute(
        sender: CommandSender,
        label: String,
        args: Array<out String>,
    ): Boolean = executor.onCommand(sender, this, label, args)

    override fun tabComplete(
        sender: CommandSender,
        alias: String,
        args: Array<out String>,
    ): List<String> = executor.onTabComplete(sender, this, alias, args)
}
