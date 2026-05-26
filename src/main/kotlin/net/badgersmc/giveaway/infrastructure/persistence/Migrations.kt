package net.badgersmc.giveaway.infrastructure.persistence

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Idempotent schema migration. Adds any missing tables / columns to the
 * SQLite database. Safe to call on every plugin enable.
 */
object Migrations {
    fun run() {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                GiveawaysTable,
                EntriesTable,
                WinnersTable,
            )
        }
    }
}
