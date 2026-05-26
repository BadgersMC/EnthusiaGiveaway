package net.badgersmc.giveaway.infrastructure.persistence

import net.badgersmc.giveaway.domain.Entry
import net.badgersmc.giveaway.domain.GiveawayId
import net.badgersmc.giveaway.domain.ports.EntryRepository
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ExposedEntryRepository : EntryRepository {

    override fun hasEntered(giveawayId: GiveawayId, playerUuid: UUID): Boolean = transaction {
        EntriesTable
            .selectAll()
            .where {
                (EntriesTable.giveawayId eq giveawayId.value.toString()) and
                    (EntriesTable.playerUuid eq playerUuid.toString())
            }
            .empty().not()
    }

    override fun insert(entry: Entry): Unit = transaction {
        EntriesTable.insert {
            it[giveawayId] = entry.giveawayId.value.toString()
            it[playerUuid] = entry.playerUuid.toString()
            it[enteredAt] = entry.enteredAt
        }
        Unit
    }

    override fun playerUuidsFor(giveawayId: GiveawayId): List<UUID> = transaction {
        EntriesTable
            .selectAll()
            .where { EntriesTable.giveawayId eq giveawayId.value.toString() }
            .map { UUID.fromString(it[EntriesTable.playerUuid]) }
    }
}
