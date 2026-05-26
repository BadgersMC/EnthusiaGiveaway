package net.badgersmc.giveaway.application

import net.badgersmc.giveaway.domain.Giveaway

sealed class ScheduleResult {
    data class Created(val giveaway: Giveaway) : ScheduleResult()
    data class Invalid(val field: String, val reason: String) : ScheduleResult()
}
