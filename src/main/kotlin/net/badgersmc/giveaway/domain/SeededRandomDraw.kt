package net.badgersmc.giveaway.domain

import net.badgersmc.giveaway.domain.ports.RandomDraw
import kotlin.random.Random

class SeededRandomDraw(seed: Long) : RandomDraw {
    private val rng = Random(seed)

    override fun <T> pick(k: Int, from: List<T>): List<T> {
        if (from.isEmpty() || k <= 0) return emptyList()
        if (k >= from.size) return from.toList()
        return from.shuffled(rng).take(k)
    }
}
