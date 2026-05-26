package net.badgersmc.giveaway.infrastructure.persistence

import net.badgersmc.giveaway.domain.GiveawayId
import net.badgersmc.giveaway.domain.Winner
import net.badgersmc.giveaway.domain.ports.WinnerRepository
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert

class ExposedWinnerRepository : WinnerRepository {

    /**
     * Upsert keeps re-inserts of the same composite PK idempotent — required
     * for the resume-on-restart flow (REQ-011): if the server crashed after
     * picking winners but before marking the giveaway COMPLETED, re-running
     * the draw on the next boot must not throw.
     */
    override fun insert(winner: Winner): Unit = transaction {
        WinnersTable.upsert {
            it[giveawayId] = winner.giveawayId.value.toString()
            it[playerUuid] = winner.playerUuid.toString()
            it[drawnAt] = winner.drawnAt
        }
        Unit
    }

    fun countByGiveaway(giveawayId: GiveawayId): Int = transaction {
        WinnersTable
            .selectAll()
            .where { WinnersTable.giveawayId eq giveawayId.value.toString() }
            .count()
            .toInt()
    }
}
