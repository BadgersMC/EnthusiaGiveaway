package net.badgersmc.giveaway.infrastructure.bukkit

import net.badgersmc.giveaway.application.StartFromTemplateResult
import net.badgersmc.giveaway.application.StartGiveawayFromTemplate
import net.badgersmc.giveaway.infrastructure.menus.AdminGiveawayMenu
import net.badgersmc.giveaway.infrastructure.menus.PlayerGiveawayMenu
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class GiveawayCommand(
    private val playerMenu: PlayerGiveawayMenu,
    private val adminMenu: AdminGiveawayMenu,
    private val startFromTemplate: StartGiveawayFromTemplate,
) : TabExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        val sub = args.firstOrNull()?.lowercase()

        // Console template command (automation, works from console or player)
        if (sub == "template") {
            val templateName = args.getOrNull(1)?.lowercase()
            if (templateName == null) {
                sender.sendMessage("§cUsage: /giveaway template <name>")
                return true
            }
            when (val result = startFromTemplate.invoke(templateName)) {
                is StartFromTemplateResult.Created ->
                    sender.sendMessage("§aStarted template giveaway: ${result.giveaway.title}")
                StartFromTemplateResult.NotFound ->
                    sender.sendMessage("§cTemplate not found: $templateName")
                is StartFromTemplateResult.Invalid ->
                    sender.sendMessage("§cInvalid template: $templateName — ${result.reason}")
            }
            return true
        }

        // Player-only paths below
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can run this command.")
            return true
        }

        when (sub) {
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
            else -> sender.sendMessage("§cUsage: /giveaway [admin|template <name>]")
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
            listOf("admin", "template").filter { it.startsWith(args[0], ignoreCase = true) }
        args.size == 2 && args[0].equals("template", ignoreCase = true) ->
            startFromTemplate.templates.map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
        else -> emptyList()
    }

    /** Exposed for template tab-completion. */
    val templates get() = startFromTemplate.templates
}
