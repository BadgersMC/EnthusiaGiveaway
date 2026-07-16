package net.badgersmc.giveaway.infrastructure.menus

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import net.badgersmc.giveaway.application.CancelGiveaway
import net.badgersmc.giveaway.application.CancelResult
import net.badgersmc.giveaway.application.ScheduleResult
import net.badgersmc.giveaway.application.StartGiveawayFromTemplate
import net.badgersmc.giveaway.application.StartFromTemplateResult
import net.badgersmc.giveaway.domain.Giveaway
import net.badgersmc.giveaway.domain.GiveawayState
import net.badgersmc.giveaway.domain.Template
import net.badgersmc.giveaway.domain.ports.GiveawayRepository
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.time.Duration
import java.time.Instant

class AdminGiveawayMenu(
    private val giveaways: GiveawayRepository,
    private val cancelGiveaway: CancelGiveaway,
    private val scheduleWizard: ScheduleWizard,
    private val templates: List<Template>,
    private val startFromTemplate: StartGiveawayFromTemplate,
) {
    fun open(player: Player) {
        val gui = ChestGui(6, "Giveaway Admin")
        gui.setOnTopClick { it.isCancelled = true }
        val pane = StaticPane(0, 0, 9, 6)
        val now = Instant.now()
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
                pane.addItem(buildRow(g, now, player), index % 9, index / 9)
            }
        }

        // Schedule-new button (bottom-right)
        val schedule = ItemStack(Material.NETHER_STAR)
        schedule.editMeta {
            it.displayName(Component.text("Schedule new").color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
        }
        pane.addItem(GuiItem(schedule) { event ->
            event.isCancelled = true
            player.closeInventory()
            scheduleWizard.open(player)
        }, 7, 5)

        // Templates button
        if (templates.isNotEmpty()) {
            val tpl = ItemStack(Material.BOOK)
            tpl.editMeta {
                it.displayName(Component.text("Start from Template").color(NamedTextColor.AQUA)
                    .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
            }
            pane.addItem(GuiItem(tpl) { event ->
                event.isCancelled = true
                player.closeInventory()
                openTemplates(player)
            }, 8, 5)
        }

        gui.addPane(pane)
        gui.show(player)
    }

    /** Build a giveaway row item with live time-remaining in the lore. */
    private fun buildRow(g: Giveaway, now: Instant, player: Player): GuiItem {
        val mat = if (g.state == GiveawayState.ACTIVE) Material.YELLOW_DYE else Material.GRAY_DYE
        val item = ItemStack(mat)
        item.editMeta { meta ->
            meta.displayName(Component.text(g.title).color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
            val remaining = Duration.between(now, g.endsAt).seconds.coerceAtLeast(0L)
            meta.lore(listOf(
                Component.text("State: ${g.state}").color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("Time left: ${formatDuration(remaining)}").color(NamedTextColor.YELLOW)
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

    /** Template browser sub-menu. */
    private fun openTemplates(player: Player) {
        val gui = ChestGui(3, "Start from Template")
        gui.setOnTopClick { it.isCancelled = true }
        val pane = StaticPane(0, 0, 9, 3)

        templates.take(9).forEachIndexed { index, t ->
            val item = ItemStack(Material.BOOK)
            item.editMeta { meta ->
                meta.displayName(Component.text(t.title).color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                meta.lore(listOf(
                    Component.text(t.description).color(NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Winners: ${t.maxWinners}  ·  Duration: ${formatDuration(t.durationSeconds)}").color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Click to start").color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false),
                ))
            }
            pane.addItem(GuiItem(item) { event ->
                event.isCancelled = true
                val result = startFromTemplate.invoke(t.name)
                when (result) {
                    is StartFromTemplateResult.Created -> {
                        player.closeInventory()
                        player.sendMessage(Component.text("Started: ${t.title}").color(NamedTextColor.GREEN))
                        open(player)
                    }
                    is StartFromTemplateResult.Invalid ->
                        player.sendActionBar(Component.text("Invalid: ${result.reason}").color(NamedTextColor.RED))
                    StartFromTemplateResult.NotFound ->
                        player.sendActionBar(Component.text("Template not found").color(NamedTextColor.RED))
                }
            }, index, 1)
        }

        // Back button
        val back = ItemStack(Material.ARROW)
        back.editMeta {
            it.displayName(Component.text("← Back").color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false))
        }
        pane.addItem(GuiItem(back) { event ->
            event.isCancelled = true
            open(player)
        }, 0, 2)

        gui.addPane(pane)
        gui.show(player)
    }

    private fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "expired"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return buildString {
            if (h > 0) append("${h}h ")
            if (h > 0 || m > 0) append("${m}m ")
            append("${s}s")
        }
    }
}
