package net.badgersmc.giveaway.application

import net.badgersmc.giveaway.domain.GiveawayId
import net.badgersmc.giveaway.domain.GiveawayState
import net.badgersmc.giveaway.domain.ports.Clock
import net.badgersmc.giveaway.domain.ports.GiveawayRepository

/**
 * Reconciles the in-flight giveaway state after a server restart (REQ-011).
 *
 * - DRAWING rows: the previous boot crashed mid-draw — re-invoke the draw.
 *   Winner persistence is idempotent on the composite PK so re-inserts are safe.
 * - ACTIVE rows past endsAt: missed their scheduler tick — draw immediately.
 * - Anything still in the future is left for the regular scheduler.
 *
 * Takes the draw dispatcher as a function so the test can mock it without
 * having to mock the concrete `DrawWinners` class.
 */
class ResumeGiveawaysOnStartup(
    private val giveaways: GiveawayRepository,
    private val drawGiveaway: (GiveawayId) -> Unit,
    private val clock: Clock,
) {
    operator fun invoke() {
        val now = clock.now()
        giveaways.listByState(GiveawayState.DRAWING).forEach { drawGiveaway(it.id) }
        giveaways.listByState(GiveawayState.ACTIVE)
            .filter { !it.endsAt.isAfter(now) }
            .forEach { drawGiveaway(it.id) }
    }
}
