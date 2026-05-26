package net.badgersmc.giveaway.infrastructure.celebrate

import net.badgersmc.giveaway.domain.Giveaway
import net.badgersmc.giveaway.domain.WinnerHandle
import net.badgersmc.giveaway.domain.ports.CelebrationBroadcaster
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.FireworkEffect
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.entity.Firework
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.plugin.java.JavaPlugin
import java.awt.image.BufferedImage
import java.net.URI
import java.time.Duration
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.random.Random

/**
 * Adventure-based celebration broadcaster (REQ-009). Ported from LumaSG's
 * Celebration.kt; adapted to handle N winners and the EnthusiaGiveaway config
 * layout.
 *
 * Side effects per `announce` call:
 *   1. Show MiniMessage gradient title to the configured audience.
 *   2. Broadcast a chat line per winner.
 *   3. Optionally spawn fireworks at each online winner.
 *   4. Optionally fetch each winner's face from Starlight Skins and render
 *      it as colored pixel characters in chat (silent on failure).
 *
 * `notifyCancellation` and `notifyNew` (INFRA-17) emit lightweight chat
 * broadcasts using the configured MiniMessage templates.
 */
class BukkitCelebrationBroadcaster(
    private val plugin: JavaPlugin,
) : CelebrationBroadcaster {

    private val mm = MiniMessage.miniMessage()
    private val pixelChar = config().getString("celebration.pixel-art.character", "⬛")!!.ifEmpty { "⬛" }

    override fun announce(giveaway: Giveaway, winners: List<WinnerHandle>) {
        if (winners.isEmpty()) return
        val cfg = config()
        val audience = audienceFor(cfg.getString("celebration.broadcast-scope", "server")!!, winners.map { it.uuid })

        runOnMain {
            val titleTpl = cfg.getString("celebration.title", "<gold><bold>GIVEAWAY!</bold></gold>")!!
            val subtitleTpl = cfg.getString("celebration.subtitle", "<white><player> won!</white>")!!
            val title = Title.title(
                mm.deserialize(titleTpl),
                mm.deserialize(subtitleTpl.replace("<player>", winners.first().name)),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3_000), Duration.ofMillis(1_000)),
            )
            audience.showTitle(title)

            val messageTpl = cfg.getString("celebration.message", "<gold><bold><player></bold> won <title>!</gold>")!!
            for (w in winners) {
                val msg = mm.deserialize(
                    messageTpl.replace("<player>", w.name).replace("<title>", giveaway.title)
                )
                audience.sendMessage(msg)
                audience.playSound(
                    Sound.sound(Key.key("entity.player.levelup"), Sound.Source.MASTER, 1f, 1f)
                )
            }
        }

        if (cfg.getBoolean("celebration.fireworks.enabled", true)) {
            spawnFireworks(winners, cfg.getInt("celebration.fireworks.count", 8))
        }

        if (cfg.getBoolean("celebration.pixel-art.enabled", true)) {
            val apiUrl = cfg.getString("celebration.pixel-art.api-url")!!
            val size = cfg.getInt("celebration.pixel-art.size", 8)
            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                for (w in winners) {
                    runCatching { renderPixelArt(w, apiUrl, size, audience) }
                        .onFailure { plugin.logger.warning("Pixel art render skipped for ${w.name}: ${it.message}") }
                }
            })
        }
    }

    override fun notifyCancellation(giveaway: Giveaway) {
        val tpl = config().getString("celebration.templates.cancelled",
            "<red>Giveaway cancelled: <title></red>")!!
        val msg = mm.deserialize(tpl.replace("<title>", giveaway.title))
        runOnMain { audienceFor("server", emptyList()).sendMessage(msg) }
    }

    override fun notifyNew(giveaway: Giveaway) {
        if (!config().getBoolean("broadcast-new-giveaway", true)) return
        val tpl = config().getString("celebration.templates.new",
            "<aqua>New giveaway: <title>!</aqua>")!!
        val msg = mm.deserialize(tpl.replace("<title>", giveaway.title))
        runOnMain { audienceFor("server", emptyList()).sendMessage(msg) }
    }

    // --- internals -----------------------------------------------------------

    private fun config() = plugin.config

    private fun audienceFor(scope: String, participantUuids: Collection<UUID>): Audience =
        when (scope.lowercase()) {
            "participants" -> Audience.audience(participantUuids.mapNotNull { Bukkit.getPlayer(it) })
            else -> Bukkit.getServer()
        }

    private fun runOnMain(block: () -> Unit) {
        if (Bukkit.isPrimaryThread()) block()
        else Bukkit.getScheduler().runTask(plugin, Runnable { block() })
    }

    private fun spawnFireworks(winners: List<WinnerHandle>, count: Int) {
        Bukkit.getScheduler().runTaskTimer(plugin, object : Runnable {
            var ticks = 0
            override fun run() {
                if (ticks >= count) return
                for (w in winners) {
                    val player = Bukkit.getPlayer(w.uuid) ?: continue
                    val loc = player.location.clone().add(
                        Random.nextDouble(-2.0, 2.0),
                        Random.nextDouble(1.5, 3.0),
                        Random.nextDouble(-2.0, 2.0),
                    )
                    val firework: Firework = loc.world.spawn(loc, Firework::class.java)
                    firework.fireworkMeta = firework.fireworkMeta.also { meta ->
                        meta.addEffect(
                            FireworkEffect.builder()
                                .withColor(FIREWORK_COLORS.random())
                                .withFade(FIREWORK_COLORS.random())
                                .with(FIREWORK_TYPES.random())
                                .trail(Random.nextBoolean())
                                .flicker(Random.nextBoolean())
                                .build()
                        )
                        meta.power = Random.nextInt(1, 3)
                    }
                    firework.setMetadata("celebration_firework", FixedMetadataValue(plugin, true))
                    firework.setPersistent(false)
                }
                ticks++
            }
        }, 0L, 5L) // 5 ticks = 250 ms
    }

    private fun renderPixelArt(w: WinnerHandle, apiUrl: String, size: Int, audience: Audience) {
        val url = apiUrl.replace("<uuid>", w.uuid.toString()).replace("<name>", w.name)
        val conn = URI(url).toURL().openConnection().apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("User-Agent", "EnthusiaGiveaway-Plugin")
        }
        val raw: BufferedImage = ImageIO.read(conn.getInputStream()) ?: return
        val img = if (raw.width == size && raw.height == size) raw
        else BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB).also { scaled ->
            val g = scaled.createGraphics()
            g.drawImage(raw, 0, 0, size, size, null)
            g.dispose()
        }

        val rows = (0 until img.height).map { y ->
            var row = Component.empty()
            for (x in 0 until img.width) {
                val rgb = img.getRGB(x, y)
                val alpha = (rgb shr 24) and 0xFF
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF
                val color = if (alpha < 128) NamedTextColor.BLACK else TextColor.color(r, g, b)
                row = row.append(Component.text(pixelChar).color(color))
            }
            row
        }

        runOnMain {
            audience.sendMessage(Component.empty())
            val winnerLine = Component.text(" 👑 ").color(NamedTextColor.GOLD)
                .append(Component.text(w.name).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
            for ((y, line) in rows.withIndex()) {
                val composed = if (y == 2) line.append(winnerLine) else line
                audience.sendMessage(composed)
            }
            audience.sendMessage(Component.empty())
        }
    }

    private companion object {
        val FIREWORK_COLORS = listOf(
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.PURPLE,
            Color.ORANGE, Color.AQUA, Color.FUCHSIA, Color.LIME,
        )
        val FIREWORK_TYPES = listOf(
            FireworkEffect.Type.BALL, FireworkEffect.Type.BALL_LARGE,
            FireworkEffect.Type.STAR, FireworkEffect.Type.BURST, FireworkEffect.Type.CREEPER,
        )
    }
}
