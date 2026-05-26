package net.badgersmc.giveaway.infrastructure.persistence

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Idempotent schema creator. `SchemaUtils.create` is `CREATE TABLE IF NOT
 * EXISTS ...` on every supported dialect — safe to call on every enable.
 *
 * We used to call `createMissingTablesAndColumns`, but that emits
 * `ALTER TABLE ... ADD PRIMARY KEY` for tables whose PK Exposed thinks is
 * missing, which SQLite does not support. `create` alone never alters, so
 * the only constraint is that schema changes between releases require an
 * explicit migration script — fine for v0.1 (no schema drift expected).
 */
object Migrations {
    fun run() {
        transaction {
            SchemaUtils.create(
                GiveawaysTable,
                EntriesTable,
                WinnersTable,
            )
        }
    }
}
