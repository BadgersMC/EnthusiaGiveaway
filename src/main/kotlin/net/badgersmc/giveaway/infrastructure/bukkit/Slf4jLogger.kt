package net.badgersmc.giveaway.infrastructure.bukkit

import net.badgersmc.giveaway.domain.ports.Logger

class PluginLoggerAdapter(private val delegate: java.util.logging.Logger) : Logger {
    override fun info(message: String) = delegate.info(message)
    override fun warn(message: String) = delegate.warning(message)
}
