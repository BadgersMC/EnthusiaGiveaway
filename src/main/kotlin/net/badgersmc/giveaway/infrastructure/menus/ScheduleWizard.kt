package net.badgersmc.giveaway.infrastructure.menus

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import net.badgersmc.giveaway.application.ScheduleGiveaway
import net.badgersmc.giveaway.application.ScheduleResult
import net.badgersmc.giveaway.application.util.DurationParser
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Sequential chat-input wizard that walks the admin through scheduling a
 * giveaway. Steps: title → duration → command → winner count → confirm.
 *
 * Uses chat prompts instead of AnvilGui to avoid NMS compatibility issues
 * on modern Paper (1.21.11).
 */
class ScheduleWizard(
    private val scheduleGiveaway: ScheduleGiveaway,
    private val chatPrompt: ChatPromptListener,
) {

    fun open(player: Player) {
        player.closeInventory()
        promptTitle(player)
    }

    private fun promptTitle(player: Player) {
        player.sendMessage(Component.text("Enter giveaway title:", NamedTextColor.GOLD))
        chatPrompt.await(player) { value ->
            if (value.isBlank()) {
                player.sendMessage(Component.text("Title must not be blank.", NamedTextColor.RED))
                promptTitle(player)
            } else {
                promptDuration(player, value)
            }
        }
    }

    private fun promptDuration(player: Player, title: String) {
        player.sendMessage(Component.text("Enter duration (e.g. 1h30m, 45m, 2d):", NamedTextColor.GOLD))
        chatPrompt.await(player) { value ->
            val seconds = DurationParser.parse(value)
            if (seconds == null) {
                player.sendMessage(Component.text("Invalid duration. Try 1h30m, 45m, 2d.", NamedTextColor.RED))
                promptDuration(player, title)
            } else {
                promptCommand(player, title, seconds)
            }
        }
    }

    private fun promptCommand(player: Player, title: String, durationSeconds: Long) {
        player.sendMessage(
            Component.text("Enter console command (use ", NamedTextColor.GOLD)
                .append(Component.text("<player>", NamedTextColor.AQUA))
                .append(Component.text(" as placeholder):", NamedTextColor.GOLD))
        )
        chatPrompt.await(player) { value ->
            promptWinners(player, title, durationSeconds, value)
        }
    }

    private fun promptWinners(player: Player, title: String, durationSeconds: Long, command: String) {
        player.sendMessage(Component.text("Enter number of winners:", NamedTextColor.GOLD))
        chatPrompt.await(player) { value ->
            val winners = value.toIntOrNull()
            if (winners == null || winners < 1) {
                player.sendMessage(Component.text("Winner count must be a positive integer.", NamedTextColor.RED))
                promptWinners(player, title, durationSeconds, command)
            } else {
                confirm(player, title, durationSeconds, command, winners)
            }
        }
    }

    private fun confirm(
        player: Player, title: String, durationSeconds: Long, command: String, winners: Int,
    ) {
        val gui = ChestGui(3, "Confirm giveaway")
        gui.setOnTopClick { it.isCancelled = true }
        val pane = StaticPane(0, 0, 9, 3)

        val summary = ItemStack(Material.PAPER)
        summary.editMeta { meta ->
            meta.displayName(
                Component.text(title).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false))
            meta.lore(
                listOf(
                    line("Duration", "${durationSeconds}s"),
                    line("Winners", winners.toString()),
                    line("Command", command),
                ))
        }
        pane.addItem(GuiItem(summary) { it.isCancelled = true }, 4, 0)

        val confirmItem = ItemStack(Material.LIME_CONCRETE)
        confirmItem.editMeta {
            it.displayName(
                Component.text("Confirm").color(NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false))
        }
        pane.addItem(GuiItem(confirmItem) { event ->
            event.isCancelled = true
            val result =
                scheduleGiveaway.invoke(title, durationSeconds, command, winners, player.uniqueId)
            when (result) {
                is ScheduleResult.Created -> player.sendMessage(
                    Component.text("Scheduled ").color(NamedTextColor.GREEN)
                        .append(Component.text(title).color(NamedTextColor.GOLD)))
                is ScheduleResult.Invalid -> player.sendMessage(
                    Component.text("Could not schedule: ${result.field} ${result.reason}",
                        NamedTextColor.RED))
            }
            player.closeInventory()
        }, 2, 2)

        val cancelItem = ItemStack(Material.RED_CONCRETE)
        cancelItem.editMeta {
            it.displayName(
                Component.text("Cancel").color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false))
        }
        pane.addItem(GuiItem(cancelItem) { event ->
            event.isCancelled = true
            player.closeInventory()
        }, 6, 2)

        gui.addPane(pane)
        gui.show(player)
    }

    private fun line(label: String, value: String): Component =
        Component.text("$label: ").color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)
            .append(Component.text(value).color(NamedTextColor.WHITE))
}
