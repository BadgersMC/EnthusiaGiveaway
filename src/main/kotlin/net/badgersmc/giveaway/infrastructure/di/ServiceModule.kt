package net.badgersmc.giveaway.infrastructure.di

import net.badgersmc.giveaway.application.DrawWinners
import net.badgersmc.giveaway.application.EnterGiveaway
import net.badgersmc.giveaway.application.ListActiveGiveaways
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
import net.badgersmc.giveaway.infrastructure.menus.PlayerGiveawayMenu
import net.badgersmc.giveaway.infrastructure.persistence.ExposedEntryRepository
import net.badgersmc.giveaway.infrastructure.persistence.ExposedGiveawayRepository
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
class ServiceModule(plugin: JavaPlugin) {

    // Domain ports → infra adapters
    val clock = BukkitClock()
    val nameLookup = BukkitNameLookup()
    val commands = BukkitCommandExecutor()
    val logger = PluginLoggerAdapter(plugin.logger)
    val placeholders: PlaceholderExpander = NoOpPlaceholderExpander()        // INFRA-12 replaces
    val celebration: CelebrationBroadcaster = NoOpCelebrationBroadcaster()   // INFRA-15 replaces
    val winners: WinnerRepository = InMemoryWinnerRepository()               // TDD-56 replaces
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

    // Bukkit-facing
    val playerMenu = PlayerGiveawayMenu(listActive, enterGiveaway)
    val giveawayCommand = GiveawayCommand(playerMenu)
}

