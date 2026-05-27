package net.badgersmc.giveaway.infrastructure.persistence

import com.zaxxer.hikari.HikariDataSource
import net.badgersmc.nexus.persistence.DatabaseFactory as NexusDatabaseFactory
import net.badgersmc.nexus.persistence.DatabaseSpec
import org.jetbrains.exposed.sql.Database
import java.io.File

/**
 * Opens (or creates) the SQLite database file under the plugin data folder
 * and wires Exposed to it via a Hikari connection pool.
 *
 * The Hikari setup is delegated to `nexus-persistence`'s [NexusDatabaseFactory]
 * so pool sizing rules (`maximumPoolSize = 1` for SQLite to avoid
 * `SQLITE_BUSY`) and driver class wiring stay in one place. This class
 * keeps the Exposed-specific bit — `Database.connect(dataSource)`.
 */
class DatabaseFactory(dataFolder: File, dbFileName: String) {

    val dataSource: HikariDataSource

    init {
        if (!dataFolder.exists()) dataFolder.mkdirs()
        val dbFile = File(dataFolder, dbFileName)
        dataSource = NexusDatabaseFactory.open(DatabaseSpec.Sqlite(dbFile))
        Database.connect(dataSource)
    }

    fun close() {
        dataSource.close()
    }
}
