package net.badgersmc.giveaway.infrastructure.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * SQLite schema for EnthusiaGiveaway (REQ-001, REQ-022).
 *
 * UUIDs stored as 36-char strings (canonical form) — SQLite has no native UUID
 * type and TEXT keeps the database file inspectable by hand.
 *
 * Timestamps use Exposed's `timestamp` (Java `Instant`) which on SQLite
 * serialises as TEXT in ISO-8601.
 */

object GiveawaysTable : Table("giveaway") {
    val id = varchar("id", 36)
    val title = varchar("title", 64)
    val command = text("command")
    val scheduledAt = timestamp("scheduled_at")
    val endsAt = timestamp("ends_at")
    val maxWinners = integer("max_winners")
    val state = varchar("state", 16)
    val createdBy = varchar("created_by", 36)

    override val primaryKey = PrimaryKey(id)
}

object EntriesTable : Table("giveaway_entry") {
    val giveawayId = varchar("giveaway_id", 36) references GiveawaysTable.id
    val playerUuid = varchar("player_uuid", 36)
    val enteredAt = timestamp("entered_at")

    /** Composite PK enforces REQ-004 (no double entries). */
    override val primaryKey = PrimaryKey(giveawayId, playerUuid)
}

object WinnersTable : Table("giveaway_winner") {
    val giveawayId = varchar("giveaway_id", 36) references GiveawaysTable.id
    val playerUuid = varchar("player_uuid", 36)
    val drawnAt = timestamp("drawn_at")

    /** Composite PK makes resume-on-restart winner persistence idempotent. */
    override val primaryKey = PrimaryKey(giveawayId, playerUuid)
}
