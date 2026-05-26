package net.badgersmc.giveaway.infrastructure.persistence

import net.badgersmc.giveaway.domain.Giveaway
import net.badgersmc.giveaway.domain.GiveawayId
import net.badgersmc.giveaway.domain.GiveawayState
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * TDD-40 — RED test for ExposedGiveawayRepository (REQ-001, REQ-022).
 *
 * Behaviours pinned:
 * - `save` then `findById` round-trips every field of `Giveaway`.
 * - `findById` on a missing id returns null.
 * - `save` is upsert: re-saving the same id overwrites state.
 * - `listByState` returns only rows matching that state.
 */
class ExposedGiveawayRepositoryTest {

    private fun fixture(state: GiveawayState = GiveawayState.SCHEDULED): Giveaway = Giveaway(
        id = GiveawayId(UUID.randomUUID()),
        title = "Diamond drop",
        command = "give <player> diamond 1",
        scheduledAt = Instant.parse("2026-05-25T12:00:00Z"),
        endsAt = Instant.parse("2026-05-25T13:00:00Z"),
        maxWinners = 3,
        state = state,
        createdBy = UUID.randomUUID(),
    )

    private fun withRepo(tempDir: Path, block: (ExposedGiveawayRepository) -> Unit) {
        val factory = DatabaseFactory(File(tempDir.toFile(), "data"), "g.db")
        try {
            Migrations.run()
            block(ExposedGiveawayRepository())
        } finally {
            factory.close()
        }
    }

    @Test
    fun `save then findById round-trips all fields`(@TempDir tempDir: Path) {
        withRepo(tempDir) { repo ->
            val g = fixture()
            repo.save(g)
            assertEquals(g, repo.findById(g.id))
        }
    }

    @Test
    fun `findById returns null for unknown id`(@TempDir tempDir: Path) {
        withRepo(tempDir) { repo ->
            assertNull(repo.findById(GiveawayId(UUID.randomUUID())))
        }
    }

    @Test
    fun `save is upsert - overwrites existing row`(@TempDir tempDir: Path) {
        withRepo(tempDir) { repo ->
            val g = fixture(GiveawayState.SCHEDULED)
            repo.save(g)
            repo.save(g.copy(state = GiveawayState.ACTIVE))
            assertEquals(GiveawayState.ACTIVE, repo.findById(g.id)?.state)
        }
    }

    @Test
    fun `listByState returns only matching rows`(@TempDir tempDir: Path) {
        withRepo(tempDir) { repo ->
            val a = fixture(GiveawayState.ACTIVE)
            val b = fixture(GiveawayState.ACTIVE)
            val c = fixture(GiveawayState.SCHEDULED)
            repo.save(a); repo.save(b); repo.save(c)

            val active = repo.listByState(GiveawayState.ACTIVE)
            assertEquals(setOf(a.id, b.id), active.map { it.id }.toSet())
        }
    }
}
