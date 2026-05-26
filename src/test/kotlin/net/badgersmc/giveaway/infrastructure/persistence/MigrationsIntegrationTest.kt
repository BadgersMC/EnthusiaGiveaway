package net.badgersmc.giveaway.infrastructure.persistence

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration check for INFRA-03 — exercises Hikari + Exposed against a
 * real on-disk SQLite file in a JUnit-managed temp directory.
 */
class MigrationsIntegrationTest {

    @Test
    fun `creates schema in a fresh data folder and is idempotent`(@TempDir tempDir: Path) {
        val dataFolder = File(tempDir.toFile(), "EnthusiaGiveaway")
        val factory = DatabaseFactory(dataFolder, "giveaways.db")
        try {
            Migrations.run()
            Migrations.run() // second call must be a no-op

            assertTrue(File(dataFolder, "giveaways.db").exists(), "DB file should be created")

            // After migration, no further DDL should be needed.
            transaction {
                val pending = SchemaUtils.statementsRequiredToActualizeScheme(
                    GiveawaysTable, EntriesTable, WinnersTable
                )
                assertEquals(
                    emptyList(), pending,
                    "schema should be in sync after Migrations.run() — pending: $pending"
                )
            }
        } finally {
            factory.close()
        }
    }
}
