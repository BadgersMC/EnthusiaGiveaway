package net.badgersmc.giveaway.application

sealed class DrawResult {
    data class Drawn(val winnerCount: Int) : DrawResult()
    data object NotFound : DrawResult()
    data object NotActive : DrawResult()
}
