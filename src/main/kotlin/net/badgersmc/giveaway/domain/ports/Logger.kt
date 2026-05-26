package net.badgersmc.giveaway.domain.ports

interface Logger {
    fun info(message: String)
    fun warn(message: String)
}
