package net.badgersmc.giveaway.domain

import net.badgersmc.giveaway.domain.ports.RandomDraw
import java.security.SecureRandom

/**
 * CSPRNG-based random draw for picking giveaway winners.
 * Uses [java.security.SecureRandom] (unpredictable) rather than
 * kotlin.random.Random (statistical only) per security best practice.
 */
class SeededRandomDraw(seed: Long) : RandomDraw {
    private val rng = SecureRandom().apply { setSeed(seed) }

    override fun <T> pick(k: Int, from: List<T>): List<T> {
        if (from.isEmpty() || k <= 0) return emptyList()
        if (k >= from.size) return from.toList()
        return from.shuffled(rng).take(k)
    }
}
