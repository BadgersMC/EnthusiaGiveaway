package net.badgersmc.giveaway.application

sealed class CancelResult {
    data object Cancelled : CancelResult()
    data object NotFound : CancelResult()
    data object AlreadyFinal : CancelResult()
}
