package net.badgersmc.giveaway.infrastructure.components

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/** Shared MiniMessage styling for chat messages, action bars, and item names. */
internal object Styled {

    private val mm = MiniMessage.miniMessage()

    /** Deserialize a MiniMessage template with positional `<v0>`…`<v9>` placeholders. */
    fun msg(template: String, vararg values: String): Component =
        mm.deserialize(
            template,
            TagResolver.resolver(
                values.mapIndexed { i, v -> Placeholder.component("v${i}", Component.text(v)) }
            )
        ).decoration(TextDecoration.ITALIC, false)

    /** Deserialize with no placeholders. */
    fun msg(template: String): Component = mm.deserialize(template).decoration(TextDecoration.ITALIC, false)

    /**
     * Standard giveaway item name — gradient gold with shadow.
     * Example: `<gradient:#FFD700:#FFA500>Diamond Stack Giveaway</gradient>`
     * falls back to plain gold if gradient parsing fails.
     */
    fun giveawayTitle(title: String): Component = runCatching {
        mm.deserialize("<gradient:#FFD700:#FFA500><b>$title</b></gradient>")
            .decoration(TextDecoration.ITALIC, false)
    }.getOrDefault(
        Component.text(title).color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
            .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false)
    )

    /** Dimmed body text. */
    fun body(text: String): Component =
        Component.text(text, net.kyori.adventure.text.format.NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)

    /** Accent color (aqua/cyan). */
    fun accent(text: String): Component =
        Component.text(text, net.kyori.adventure.text.format.NamedTextColor.AQUA)
            .decoration(TextDecoration.ITALIC, false)

    /** Success green. */
    fun success(text: String): Component =
        Component.text(text, net.kyori.adventure.text.format.NamedTextColor.GREEN)
            .decoration(TextDecoration.ITALIC, false)

    /** Error red. */
    fun error(text: String): Component =
        Component.text(text, net.kyori.adventure.text.format.NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false)

    /** Time remaining in yellow. */
    fun timeLeft(seconds: Long): Component =
        Component.text("Time left: ${formatDuration(seconds)}", net.kyori.adventure.text.format.NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false)

    private fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "expired"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return buildString {
            if (h > 0) append("${h}h ")
            if (h > 0 || m > 0) append("${m}m ")
            append("${s}s")
        }
    }
}
