package net.badgersmc.giveaway.infrastructure.menus

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.AnvilGui
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
 * Sequential anvil-input wizard that walks the admin through scheduling a
 * giveaway. Steps: title → duration → command → winner count → confirm.
 *
 * IF 0.11.6's AnvilGui exposes a `setOnNameInputChanged` callback fired on
 * every keystroke; the confirm button (3rd slot) commits the value and
 * advances to the next step.
 */
class ScheduleWizard(private val scheduleGiveaway: ScheduleGiveaway) {

    fun open(player: Player) = promptTitle(player)

    private fun promptTitle(player: Player) {
        anvilStep(
            player = player,
            prompt = "Enter title",
            initial = "Giveaway",
            onAccept = { value ->
                if (value.isBlank()) {
                    player.sendMessage(Component.text("Title must not be blank.", NamedTextColor.RED))
                    promptTitle(player)
                } else promptDuration(player, value)
            }
        )
    }

    private fun promptDuration(player: Player, title: String) {
        anvilStep(
            player = player,
            prompt = "Duration (e.g. 1h30m)",
            initial = "1h",
            onAccept = { value ->
                val seconds = DurationParser.parse(value)
                if (seconds == null) {
                    player.sendMessage(Component.text("Invalid duration. Try 1h30m, 45m, 2d.", NamedTextColor.RED))
                    promptDuration(player, title)
                } else promptCommand(player, title, seconds)
            }
        )
    }

    private fun promptCommand(player: Player, title: String, durationSeconds: Long) {
        anvilStep(
            player = player,
            prompt = "Console command",
            initial = "give <player> diamond 1",
            onAccept = { value ->
                promptWinners(player, title, durationSeconds, value)
            }
        )
    }

    private fun promptWinners(player: Player, title: String, durationSeconds: Long, command: String) {
        anvilStep(
            player = player,
            prompt = "Winner count",
            initial = "1",
            onAccept = { value ->
                val winners = value.toIntOrNull()
                if (winners == null || winners < 1) {
                    player.sendMessage(Component.text("Winner count must be a positive integer.", NamedTextColor.RED))
                    promptWinners(player, title, durationSeconds, command)
                } else confirm(player, title, durationSeconds, command, winners)
            }
        )
    }

    private fun confirm(player: Player, title: String, durationSeconds: Long, command: String, winners: Int) {
        val gui = ChestGui(3, "Confirm giveaway")
        gui.setOnTopClick { it.isCancelled = true }
        val pane = StaticPane(0, 0, 9, 3)

        val summary = ItemStack(Material.PAPER)
        summary.editMeta { meta ->
            meta.displayName(Component.text(title).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false))
            meta.lore(listOf(
                line("Duration", "${durationSeconds}s"),
                line("Winners", winners.toString()),
                line("Command", command),
            ))
        }
        pane.addItem(GuiItem(summary) { it.isCancelled = true }, 4, 0)

        val confirmItem = ItemStack(Material.LIME_CONCRETE)
        confirmItem.editMeta {
            it.displayName(Component.text("Confirm").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
        }
        pane.addItem(GuiItem(confirmItem) { event ->
            event.isCancelled = true
            val result = scheduleGiveaway.invoke(title, durationSeconds, command, winners, player.uniqueId)
            when (result) {
                is ScheduleResult.Created -> player.sendMessage(
                    Component.text("Scheduled ").color(NamedTextColor.GREEN)
                        .append(Component.text(title).color(NamedTextColor.GOLD))
                )
                is ScheduleResult.Invalid -> player.sendMessage(
                    Component.text("Could not schedule: ${result.field} ${result.reason}", NamedTextColor.RED)
                )
            }
            player.closeInventory()
        }, 2, 2)

        val cancelItem = ItemStack(Material.RED_CONCRETE)
        cancelItem.editMeta {
            it.displayName(Component.text("Cancel").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
        }
        pane.addItem(GuiItem(cancelItem) { event ->
            event.isCancelled = true
            player.closeInventory()
        }, 6, 2)

        gui.addPane(pane)
        gui.show(player)
    }

    private fun anvilStep(player: Player, prompt: String, initial: String, onAccept: (String) -> Unit) {
        val gui = AnvilGui(prompt)
        gui.setOnTopClick { it.isCancelled = true }
        var current = initial

        val firstPane = StaticPane(0, 0, 1, 1)
        val input = ItemStack(Material.PAPER)
        input.editMeta {
            it.displayName(Component.text(initial).decoration(TextDecoration.ITALIC, false))
        }
        firstPane.addItem(GuiItem(input) { it.isCancelled = true }, 0, 0)
        gui.firstItemComponent.addPane(firstPane)

        gui.setOnNameInputChanged { newName -> current = newName }

        val thirdPane = StaticPane(0, 0, 1, 1)
        val confirmItem = ItemStack(Material.LIME_DYE)
        confirmItem.editMeta {
            it.displayName(Component.text("Confirm").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
        }
        thirdPane.addItem(GuiItem(confirmItem) { event ->
            event.isCancelled = true
            val captured = current
            // close before re-opening next step so IF doesn't fight the inventory open
            event.whoClicked.closeInventory()
            onAccept(captured)
        }, 0, 0)
        gui.resultComponent.addPane(thirdPane)

        gui.show(player)
    }

    private fun line(label: String, value: String): Component =
        Component.text("$label: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            .append(Component.text(value).color(NamedTextColor.WHITE))
}
