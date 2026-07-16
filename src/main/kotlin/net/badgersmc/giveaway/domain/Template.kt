package net.badgersmc.giveaway.domain

/**
 * Premade giveaway preset loaded from templates.yml.  Used by the admin
 * template-browser menu and by the console "/giveaway template <name>" path.
 */
data class Template(
    val name: String,
    val title: String,
    val description: String,
    val command: String,
    val maxWinners: Int,
    val durationSeconds: Long,
)
