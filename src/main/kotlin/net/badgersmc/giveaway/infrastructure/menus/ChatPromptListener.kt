package net.badgersmc.giveaway.infrastructure.menus

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Captures a single chat input from a player, then unregisters. */
class ChatPromptListener(
    private val plugin: JavaPlugin,
) : Listener {

    private val callbacks = ConcurrentHashMap<UUID, (String) -> Unit>()

    fun await(player: Player, callback: (String) -> Unit) {
        callbacks[player.uniqueId] = callback
    }

    fun cancel(player: Player) {
        callbacks.remove(player.uniqueId)
    }

    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        val callback = callbacks.remove(event.player.uniqueId) ?: return
        event.isCancelled = true
        val message = PlainTextComponentSerializer.plainText().serialize(event.message())
        // AsyncChatEvent fires off the main thread — schedule the callback
        // synchronously so InventoryOpenEvent (triggered by ChestGui.show)
        // runs on the server thread.
        plugin.server.scheduler.runTask(plugin, Runnable { callback(message) })
    }
}
