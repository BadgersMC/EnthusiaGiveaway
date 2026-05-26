package net.badgersmc.giveaway.domain.ports

interface RandomDraw {
    fun <T> pick(k: Int, from: List<T>): List<T>
}
