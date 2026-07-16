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

        return section.getKeys(false).mapNotNull { key ->
            val cfg = section.getConfigurationSection(key)
            if (cfg == null) {
                pluginDataFolder.let { java.util.logging.Logger.getLogger("EnthusiaGiveaway").warning("Template '$key' is not a section — skipping") }
                return@mapNotNull null
            }
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

    private fun parseDuration(raw: String): Long {
        val result = runCatching { Duration.parse(raw).seconds }
        result.exceptionOrNull()?.let {
            java.util.logging.Logger.getLogger("EnthusiaGiveaway")
                .warning("Invalid duration '$raw' in templates.yml — falling back to 300s")
        }
        return result.getOrDefault(300L)
    }
}
