package net.badgersmc.giveaway.infrastructure.menus

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import net.badgersmc.giveaway.application.CancelGiveaway
import net.badgersmc.giveaway.application.CancelResult
import net.badgersmc.giveaway.domain.Giveaway
import net.badgersmc.giveaway.domain.GiveawayState
import net.badgersmc.giveaway.domain.ports.GiveawayRepository
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class AdminGiveawayMenu(
    private val giveaways: GiveawayRepository,
    private val cancelGiveaway: CancelGiveaway,
    private val scheduleWizard: ScheduleWizard,
) {
    fun open(player: Player) {
        val gui = ChestGui(6, "Giveaway Admin")
        gui.setOnTopClick { it.isCancelled = true }
        val pane = StaticPane(0, 0, 9, 6)

        val rows = listOf(
            giveaways.listByState(GiveawayState.SCHEDULED),
            giveaways.listByState(GiveawayState.ACTIVE),
        ).flatten()

        if (rows.isEmpty()) {
            val empty = ItemStack(Material.BARRIER)
            empty.editMeta {
                it.displayName(Component.text("No scheduled or active giveaways").color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false))
            }
            pane.addItem(GuiItem(empty) { it.isCancelled = true }, 4, 2)
        } else {
            rows.take(45).forEachIndexed { index, g ->
                pane.addItem(buildRow(g, player), index % 9, index / 9)
            }
        }

        // Schedule-new button bottom-right
        val schedule = ItemStack(Material.NETHER_STAR)
        schedule.editMeta {
            it.displayName(Component.text("Schedule new").color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
        }
        pane.addItem(GuiItem(schedule) { event ->
            event.isCancelled = true
            player.closeInventory()
            scheduleWizard.open(player)
        }, 8, 5)

        gui.addPane(pane)
        gui.show(player)
    }

    private fun buildRow(g: Giveaway, player: Player): GuiItem {
        val mat = if (g.state == GiveawayState.ACTIVE) Material.YELLOW_DYE else Material.GRAY_DYE
        val item = ItemStack(mat)
        item.editMeta { meta ->
            meta.displayName(Component.text(g.title).color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
            meta.lore(listOf(
                Component.text("State: ${g.state}").color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("Winners: ${g.maxWinners}").color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("Command: ${g.command}").color(NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Shift-click to cancel").color(NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false),
            ))
        }
        return GuiItem(item) { event ->
            event.isCancelled = true
            if (!event.isShiftClick) return@GuiItem
            when (cancelGiveaway.invoke(g.id)) {
                CancelResult.Cancelled -> {
                    player.sendActionBar(Component.text("Cancelled: ${g.title}").color(NamedTextColor.RED))
                    open(player) // refresh
                }
                CancelResult.NotFound -> player.sendActionBar(Component.text("Already gone").color(NamedTextColor.GRAY))
                CancelResult.AlreadyFinal -> player.sendActionBar(Component.text("Already finalized").color(NamedTextColor.GRAY))
            }
        }
    }
}
