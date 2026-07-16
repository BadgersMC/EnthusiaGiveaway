package net.badgersmc.giveaway.infrastructure.config

import net.badgersmc.giveaway.domain.Template
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.time.Duration

/** Loads giveaway templates from `templates.yml`. */
object TemplateLoader {

    fun load(pluginDataFolder: File, classLoader: ClassLoader): List<Template> {
        val file = File(pluginDataFolder, "templates.yml")
        // Save default from resources if not present
        if (!file.exists()) {
            val defaults = classLoader.getResourceAsStream("templates.yml")
            defaults?.use { src ->
                file.parentFile.mkdirs()
                file.outputStream().use { dst -> src.copyTo(dst) }
            }
        }

        val yaml = YamlConfiguration.loadConfiguration(file)
        val section = yaml.getConfigurationSection("templates") ?: return emptyList()

        return section.getKeys(false).map { key ->
            val cfg = section.getConfigurationSection(key)!!
            Template(
                name = key,
                title = cfg.getString("title") ?: key,
                description = cfg.getString("description") ?: "",
                command = cfg.getString("command") ?: "",
                maxWinners = cfg.getInt("max_winners", 1).coerceAtLeast(1),
                durationSeconds = parseDuration(cfg.getString("duration") ?: "PT5M"),
            )
        }
    }

    private fun parseDuration(raw: String): Long =
        runCatching { Duration.parse(raw).seconds }.getOrDefault(300L)
}
