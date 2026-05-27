package net.badgersmc.giveaway.infrastructure.schedule

import net.badgersmc.giveaway.application.DrawWinners
import net.badgersmc.giveaway.domain.GiveawayState
import net.badgersmc.giveaway.domain.ports.Clock
import net.badgersmc.giveaway.domain.ports.GiveawayRepository
import net.badgersmc.giveaway.domain.ports.Logger
import net.badgersmc.nexus.scheduler.NexusScheduler

/**
 * Periodic expiry sweep (REQ-007, REQ-040).
 *
 * Runs off the main thread so the SQLite query never blocks the tick loop.
 * `DrawWinners` orchestrates the state transitions and side effects; any work
 * that must run on the main thread (command dispatch, celebration broadcast)
 * routes itself there via the relevant adapter.
 *
 * Scheduling is delegated to `nexus-scheduler`, which gives us automatic
 * task cancellation on plugin disable.
 */
class BukkitTickScheduler(
    private val scheduler: NexusScheduler,
    private val giveaways: GiveawayRepository,
    private val drawWinners: DrawWinners,
    private val clock: Clock,
    private val logger: Logger,
    private val pollIntervalSeconds: Long,
) {
    private var handle: AutoCloseable? = null

    fun start() {
        val ticks = (pollIntervalSeconds * 20L).coerceAtLeast(20L)
        handle = scheduler.runRepeatingAsync(ticks, ticks) { tick() }
        logger.info("Giveaway scheduler started — polling every ${pollIntervalSeconds}s")
    }

    fun stop() {
        handle?.close()
        handle = null
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
