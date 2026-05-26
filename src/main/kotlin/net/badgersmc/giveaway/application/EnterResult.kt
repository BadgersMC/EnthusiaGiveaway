package net.badgersmc.giveaway.application

sealed class EnterResult {
    data object Success : EnterResult()
    data object AlreadyEntered : EnterResult()
    data object GiveawayNotFound : EnterResult()
    data object NotActive : EnterResult()
}
