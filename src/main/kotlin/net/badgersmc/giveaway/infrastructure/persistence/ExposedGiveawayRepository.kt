package net.badgersmc.giveaway.infrastructure.persistence

import net.badgersmc.giveaway.domain.Giveaway
import net.badgersmc.giveaway.domain.GiveawayId
import net.badgersmc.giveaway.domain.GiveawayState
import net.badgersmc.giveaway.domain.ports.GiveawayRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.util.UUID

class ExposedGiveawayRepository : GiveawayRepository {

    override fun save(giveaway: Giveaway): Unit = transaction {
        GiveawaysTable.upsert {
            it[id] = giveaway.id.value.toString()
            it[title] = giveaway.title
            it[description] = giveaway.description.takeIf { it.isNotEmpty() }
            it[command] = giveaway.command
            it[scheduledAt] = giveaway.scheduledAt
            it[endsAt] = giveaway.endsAt
            it[maxWinners] = giveaway.maxWinners
            it[state] = giveaway.state.name
            it[createdBy] = giveaway.createdBy.toString()
        }
        Unit
    }

    override fun findById(id: GiveawayId): Giveaway? = transaction {
        GiveawaysTable
            .selectAll()
            .where { GiveawaysTable.id eq id.value.toString() }
            .singleOrNull()
            ?.toGiveaway()
    }

    override fun listByState(state: GiveawayState): List<Giveaway> = transaction {
        GiveawaysTable
            .selectAll()
            .where { GiveawaysTable.state eq state.name }
            .map { it.toGiveaway() }
    }

    private fun ResultRow.toGiveaway(): Giveaway = Giveaway(
        id = GiveawayId(UUID.fromString(this[GiveawaysTable.id])),
        title = this[GiveawaysTable.title],
        description = this[GiveawaysTable.description] ?: "",
        command = this[GiveawaysTable.command],
        scheduledAt = this[GiveawaysTable.scheduledAt],
        endsAt = this[GiveawaysTable.endsAt],
        maxWinners = this[GiveawaysTable.maxWinners],
        state = GiveawayState.valueOf(this[GiveawaysTable.state]),
        createdBy = UUID.fromString(this[GiveawaysTable.createdBy]),
    )
}
