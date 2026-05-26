package net.badgersmc.giveaway.infrastructure.bukkit

import net.badgersmc.giveaway.infrastructure.persistence.DatabaseFactory
import net.badgersmc.giveaway.infrastructure.persistence.Migrations
import org.bukkit.plugin.java.JavaPlugin

class EnthusiaGiveawayPlugin : JavaPlugin() {

    private var databaseFactory: DatabaseFactory? = null

    override fun onEnable() {
        saveDefaultConfig()
        val dbFile = config.getString("storage.file") ?: "giveaways.db"

        databaseFactory = DatabaseFactory(dataFolder, dbFile)
        Migrations.run()

        logger.info("EnthusiaGiveaway enabled. Schema migrated; SQLite file: ${dataFolder}/$dbFile.")
    }

    override fun onDisable() {
        databaseFactory?.close()
        databaseFactory = null
    }
}
