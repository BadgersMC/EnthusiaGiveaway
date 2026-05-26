package net.badgersmc.giveaway.infrastructure.schedule

import net.badgersmc.giveaway.application.DrawWinners
import net.badgersmc.giveaway.domain.GiveawayState
import net.badgersmc.giveaway.domain.ports.Clock
import net.badgersmc.giveaway.domain.ports.GiveawayRepository
import net.badgersmc.giveaway.domain.ports.Logger
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

/**
 * Periodic expiry sweep (REQ-007, REQ-040).
 *
 * Runs on Bukkit's async scheduler so the SQLite query never blocks the main
 * thread. `DrawWinners` orchestrates the state transitions and side effects;
 * any work that must run on the main thread (command dispatch, celebration
 * broadcast) routes itself there via the relevant adapter.
 */
class BukkitTickScheduler(
    private val plugin: JavaPlugin,
    private val giveaways: GiveawayRepository,
    private val drawWinners: DrawWinners,
    private val clock: Clock,
    private val logger: Logger,
    private val pollIntervalSeconds: Long,
) {
    private var task: BukkitTask? = null

    fun start() {
        val ticks = (pollIntervalSeconds * 20L).coerceAtLeast(20L)
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable { tick() }, ticks, ticks)
        logger.info("Giveaway scheduler started — polling every ${pollIntervalSeconds}s")
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    private fun tick() {
        try {
            val now = clock.now()
            giveaways.listByState(GiveawayState.ACTIVE)
                .filter { !it.endsAt.isAfter(now) }
                .forEach { drawWinners.invoke(it.id) }
        } catch (t: Throwable) {
            logger.warn("Scheduler tick failed: ${t.message}")
        }
    }
}
