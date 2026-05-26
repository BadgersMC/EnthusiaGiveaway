package net.badgersmc.giveaway.application.util

/**
 * Parses human-friendly duration strings like "1h30m", "45m", "2d", "10s".
 * Returns seconds, or null if the input is empty or contains unrecognised
 * tokens.
 *
 * Domain-pure (no framework imports) so the schedule wizard adapter can call
 * it without breaking layer rules.
 */
object DurationParser {

    private val unitSeconds = mapOf(
        's' to 1L,
        'm' to 60L,
        'h' to 3600L,
        'd' to 86400L,
    )

    fun parse(input: String): Long? {
        if (input.isBlank()) return null
        var total = 0L
        var current = 0L
        var sawUnit = false
        for (ch in input.trim().lowercase()) {
            when {
                ch.isDigit() -> current = current * 10 + (ch - '0')
                ch in unitSeconds -> {
                    if (current == 0L && !sawUnit) return null
                    total += current * unitSeconds.getValue(ch)
                    current = 0L
                    sawUnit = true
                }
                ch.isWhitespace() -> Unit
                else -> return null
            }
        }
        if (current != 0L) return null // trailing number without unit
        return if (total > 0L) total else null
    }
}
