package net.badgersmc.giveaway.domain.ports

fun interface CommandExecutor {
    fun dispatch(commandLine: String)
}
