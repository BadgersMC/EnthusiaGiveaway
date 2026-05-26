package net.badgersmc.giveaway.infrastructure.bukkit

import net.badgersmc.giveaway.infrastructure.di.ServiceModule
import net.badgersmc.giveaway.infrastructure.persistence.DatabaseFactory
import net.badgersmc.giveaway.infrastructure.persistence.Migrations
import org.bukkit.plugin.java.JavaPlugin

class EnthusiaGiveawayPlugin : JavaPlugin() {

    private var databaseFactory: DatabaseFactory? = null

    lateinit var services: ServiceModule
        private set

    override fun onEnable() {
        saveDefaultConfig()
        val dbFile = config.getString("storage.file") ?: "giveaways.db"

        databaseFactory = DatabaseFactory(dataFolder, dbFile)
        Migrations.run()

        services = ServiceModule(this)

        logger.info("EnthusiaGiveaway enabled. Schema migrated; SQLite file: ${dataFolder}/$dbFile.")
    }

    override fun onDisable() {
        databaseFactory?.close()
        databaseFactory = null
    }
}
