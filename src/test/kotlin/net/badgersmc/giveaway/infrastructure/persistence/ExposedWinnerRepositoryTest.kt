package net.badgersmc.giveaway.infrastructure.persistence

import net.badgersmc.giveaway.domain.Giveaway
import net.badgersmc.giveaway.domain.GiveawayId
import net.badgersmc.giveaway.domain.GiveawayState
import net.badgersmc.giveaway.domain.Winner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

/**
 * TDD-56 — Integration test for ExposedWinnerRepository.
 *
 * Re-insert of same (giveawayId, playerUuid) must be idempotent (no thrown
 * exception) so resume-on-startup can safely re-invoke DrawWinners.
 */
class ExposedWinnerRepositoryTest {

    private val now = Instant.parse("2026-05-25T12:00:00Z")

    private fun seedGiveaway(): GiveawayId {
        val repo = ExposedGiveawayRepository()
        val id = GiveawayId(UUID.randomUUID())
        repo.save(
            Giveaway(
                id = id, title = "t", command = "c",
                scheduledAt = now, endsAt = now.plusSeconds(60),
                maxWinners = 1, state = GiveawayState.COMPLETED,
                createdBy = UUID.randomUUID(),
            )
        )
        return id
    }

    private fun withRepo(tempDir: Path, block: (ExposedWinnerRepository, GiveawayId) -> Unit) {
        val factory = DatabaseFactory(File(tempDir.toFile(), "data"), "g.db")
        try {
            Migrations.run()
            val gid = seedGiveaway()
            block(ExposedWinnerRepository(), gid)
        } finally {
            factory.close()
        }
    }

    @Test
    fun `insert then re-insert of same composite key is idempotent`(@TempDir tempDir: Path) {
        withRepo(tempDir) { repo, gid ->
            val player = UUID.randomUUID()
            repo.insert(Winner(gid, player, now))
            repo.insert(Winner(gid, player, now)) // must not throw
            assertEquals(1, repo.countByGiveaway(gid))
        }
    }

    @Test
    fun `multiple distinct winners persist separately`(@TempDir tempDir: Path) {
        withRepo(tempDir) { repo, gid ->
            repo.insert(Winner(gid, UUID.randomUUID(), now))
            repo.insert(Winner(gid, UUID.randomUUID(), now))
            assertEquals(2, repo.countByGiveaway(gid))
        }
    }
}
