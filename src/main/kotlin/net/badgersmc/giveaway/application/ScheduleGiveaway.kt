package net.badgersmc.giveaway.application

import net.badgersmc.giveaway.domain.Giveaway
import net.badgersmc.giveaway.domain.GiveawayId
import net.badgersmc.giveaway.domain.GiveawayState
import net.badgersmc.giveaway.domain.ports.CelebrationBroadcaster
import net.badgersmc.giveaway.domain.ports.Clock
import net.badgersmc.giveaway.domain.ports.GiveawayRepository
import java.util.UUID

class ScheduleGiveaway(
    private val giveaways: GiveawayRepository,
    private val clock: Clock,
    private val celebration: CelebrationBroadcaster,
) {
    operator fun invoke(
        title: String,
        durationSeconds: Long,
        command: String,
        maxWinners: Int,
        adminUuid: UUID,
        description: String = "",
    ): ScheduleResult {
        if (title.isBlank()) return ScheduleResult.Invalid("title", "must not be blank")
        if (durationSeconds <= 0) return ScheduleResult.Invalid("durationSeconds", "must be > 0")
        if (maxWinners < 1) return ScheduleResult.Invalid("maxWinners", "must be >= 1")

        val now = clock.now()
        val g = Giveaway(
            id = GiveawayId(UUID.randomUUID()),
            title = title,
            description = description,
            command = command,
            scheduledAt = now,
            endsAt = now.plusSeconds(durationSeconds),
            maxWinners = maxWinners,
            state = GiveawayState.ACTIVE,
            createdBy = adminUuid,
        )
        giveaways.save(g)
        celebration.notifyNew(g)
        return ScheduleResult.Created(g)
    }
}
