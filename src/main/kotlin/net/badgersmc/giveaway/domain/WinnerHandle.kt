package net.badgersmc.giveaway.domain

import java.util.UUID

/**
 * Resolved winner identity passed to outbound adapters
 * (command execution, placeholder expansion, celebration broadcast).
 */
data class WinnerHandle(val uuid: UUID, val name: String)
