package net.badgersmc.giveaway.infrastructure.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import java.io.File

/**
 * Opens (or creates) the SQLite database file under the plugin data folder
 * and wires Exposed to it via a Hikari connection pool.
 *
 * SQLite is single-writer so `maximumPoolSize = 1` avoids `SQLITE_BUSY`
 * under contention — concurrency on the use-case side serialises through
 * Exposed transactions.
 */
class DatabaseFactory(dataFolder: File, dbFileName: String) {

    val dataSource: HikariDataSource

    init {
        if (!dataFolder.exists()) dataFolder.mkdirs()
        val dbPath = File(dataFolder, dbFileName).absolutePath
        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:$dbPath"
            driverClassName = "org.sqlite.JDBC"
            maximumPoolSize = 1
            poolName = "EnthusiaGiveaway-SQLite"
        })
        Database.connect(dataSource)
    }

    fun close() {
        dataSource.close()
    }
}
