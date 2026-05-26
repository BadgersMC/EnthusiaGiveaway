package net.badgersmc.giveaway.infrastructure.persistence

import net.badgersmc.giveaway.domain.Entry
import net.badgersmc.giveaway.domain.GiveawayId
import net.badgersmc.giveaway.domain.GiveawayState
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD-42 — RED test for ExposedEntryRepository (REQ-003, REQ-004).
 *
 * The composite PK on (giveaway_id, player_uuid) is what enforces
 * "one entry per player per giveaway" at the storage level.
 */
class ExposedEntryRepositoryTest {

    private val now = Instant.parse("2026-05-25T12:00:00Z")

    private fun seedGiveaway(): GiveawayId {
        val giveaways = ExposedGiveawayRepository()
        val id = GiveawayId(UUID.randomUUID())
        giveaways.save(
            net.badgersmc.giveaway.domain.Giveaway(
                id = id,
                title = "t",
                command = "c",
                scheduledAt = now,
                endsAt = now.plusSeconds(60),
                maxWinners = 1,
                state = GiveawayState.ACTIVE,
                createdBy = UUID.randomUUID(),
            )
        )
        return id
    }

    private fun withRepo(tempDir: Path, block: (ExposedEntryRepository, GiveawayId) -> Unit) {
        val factory = DatabaseFactory(File(tempDir.toFile(), "data"), "g.db")
        try {
            Migrations.run()
            val gid = seedGiveaway()
            block(ExposedEntryRepository(), gid)
        } finally {
            factory.close()
        }
    }

    @Test
    fun `insert then hasEntered returns true`(@TempDir tempDir: Path) {
        withRepo(tempDir) { repo, gid ->
            val player = UUID.randomUUID()
            assertFalse(repo.hasEntered(gid, player))
            repo.insert(Entry(gid, player, now))
            assertTrue(repo.hasEntered(gid, player))
        }
    }

    @Test
    fun `duplicate insert throws on composite PK collision`(@TempDir tempDir: Path) {
        withRepo(tempDir) { repo, gid ->
            val player = UUID.randomUUID()
            repo.insert(Entry(gid, player, now))
            assertThrows<ExposedSQLException> {
                repo.insert(Entry(gid, player, now))
            }
        }
    }

    @Test
    fun `playerUuidsFor returns inserted players for that giveaway`(@TempDir tempDir: Path) {
        withRepo(tempDir) { repo, gid ->
            val a = UUID.randomUUID()
            val b = UUID.randomUUID()
            repo.insert(Entry(gid, a, now))
            repo.insert(Entry(gid, b, now))
            assertEquals(setOf(a, b), repo.playerUuidsFor(gid).toSet())
        }
    }
}
