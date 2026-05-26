package net.badgersmc.giveaway.infrastructure.di

import net.badgersmc.giveaway.application.CancelGiveaway
import net.badgersmc.giveaway.application.DrawWinners
import net.badgersmc.giveaway.application.EnterGiveaway
import net.badgersmc.giveaway.application.ListActiveGiveaways
import net.badgersmc.giveaway.application.ResumeGiveawaysOnStartup
import net.badgersmc.giveaway.application.ScheduleGiveaway
import net.badgersmc.giveaway.domain.SeededRandomDraw
import net.badgersmc.giveaway.domain.ports.CelebrationBroadcaster
import net.badgersmc.giveaway.domain.ports.PlaceholderExpander
import net.badgersmc.giveaway.domain.ports.WinnerRepository
import net.badgersmc.giveaway.infrastructure.bukkit.BukkitClock
import net.badgersmc.giveaway.infrastructure.bukkit.BukkitCommandExecutor
import net.badgersmc.giveaway.infrastructure.bukkit.BukkitNameLookup
import net.badgersmc.giveaway.infrastructure.bukkit.InMemoryWinnerRepository
import net.badgersmc.giveaway.infrastructure.bukkit.NoOpCelebrationBroadcaster
import net.badgersmc.giveaway.infrastructure.bukkit.NoOpPlaceholderExpander
import net.badgersmc.giveaway.infrastructure.bukkit.PluginLoggerAdapter
import net.badgersmc.giveaway.infrastructure.bukkit.GiveawayCommand
import net.badgersmc.giveaway.infrastructure.menus.AdminGiveawayMenu
import net.badgersmc.giveaway.infrastructure.menus.PlayerGiveawayMenu
import net.badgersmc.giveaway.infrastructure.menus.ScheduleWizard
import net.badgersmc.giveaway.infrastructure.papi.PlaceholderApiExpander
import net.badgersmc.giveaway.infrastructure.persistence.ExposedEntryRepository
import net.badgersmc.giveaway.infrastructure.persistence.ExposedGiveawayRepository
import net.badgersmc.giveaway.infrastructure.persistence.ExposedWinnerRepository
import net.badgersmc.giveaway.infrastructure.schedule.BukkitTickScheduler
import org.bukkit.plugin.java.JavaPlugin

/**
 * Manual DI wiring. Domain + application are SPEAR-pure (no Nexus imports
 * allowed per implementation.md forbidden list), so use cases are
 * constructor-injected here in infrastructure rather than discovered by a
 * Nexus `@Service` scan.
 *
 * If a future need arises for reflective service discovery on
 * infrastructure-only types (menus, commands, listeners), wire `NexusContext`
 * alongside this module and pass these instances as `externalBeans`.
 */
class ServiceModule(private val plugin: JavaPlugin) {

    // Domain ports → infra adapters
    val clock = BukkitClock()
    val nameLookup = BukkitNameLookup()
    val commands = BukkitCommandExecutor()
    val logger = PluginLoggerAdapter(plugin.logger)
    val placeholders: PlaceholderExpander = PlaceholderApiExpander()
    val celebration: CelebrationBroadcaster = NoOpCelebrationBroadcaster()   // INFRA-15 replaces
    val winners: WinnerRepository = ExposedWinnerRepository()
    val giveaways = ExposedGiveawayRepository()
    val entries = ExposedEntryRepository()
    val draw = SeededRandomDraw(System.nanoTime())

    // Application use cases
    val enterGiveaway = EnterGiveaway(giveaways, entries, clock)
    val listActive = ListActiveGiveaways(giveaways, entries, clock)
    val drawWinners = DrawWinners(
        giveaways, entries, winners, draw,
        nameLookup, placeholders, commands, celebration, logger, clock,
    )
    val scheduleGiveaway = ScheduleGiveaway(giveaways, clock)
    val cancelGiveaway = CancelGiveaway(giveaways, celebration)
    val resumeGiveawaysOnStartup = ResumeGiveawaysOnStartup(
        giveaways, { drawWinners.invoke(it) }, clock,
    )

    // Bukkit-facing
    val playerMenu = PlayerGiveawayMenu(listActive, enterGiveaway)
    val scheduleWizard = ScheduleWizard(scheduleGiveaway)
    val adminMenu = AdminGiveawayMenu(giveaways, cancelGiveaway, scheduleWizard)
    val giveawayCommand = GiveawayCommand(playerMenu, adminMenu)

    val scheduler = BukkitTickScheduler(
        plugin = plugin,
        giveaways = giveaways,
        drawWinners = drawWinners,
        clock = clock,
        logger = logger,
        pollIntervalSeconds = plugin.config.getLong("scheduler.poll-interval-seconds", 1L),
    )
}

