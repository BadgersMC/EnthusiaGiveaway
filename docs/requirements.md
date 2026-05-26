# Requirements — EnthusiaGiveaway

**Date:** 2026-05-25
**Status:** Bootstrap (emitted by `/spear:init`; extend via `/spear:spec`)
**EARS subset enforced:** Ubiquitous, Event-driven, State-driven, Unwanted. Optional Feature pattern (`WHERE …`) accepted without validation.

Each requirement carries a stable ID. Tasks reference requirements by ID. New requirements append at the next free integer ID (three-digit padded); IDs are never re-used or renumbered.

---

## Product (what the system is for)

### REQ-001 — Persist and run scheduled giveaways
**Ubiquitous.** THE SYSTEM SHALL persist scheduled, active, and completed giveaways across server restarts in a local SQLite database.

### REQ-002 — Player menu command
**Event-driven.** WHEN a player executes `/giveaway` THE SYSTEM SHALL open an inventory menu listing every active giveaway with title, time remaining, current entry count, and the player's entry status.

### REQ-003 — Enter a giveaway
**Event-driven.** WHEN a player clicks an unentered active giveaway in the menu THE SYSTEM SHALL record exactly one entry for that player against that giveaway and refresh the menu to show entered status.

### REQ-004 — No double entries
**Unwanted.** IF a player attempts to enter a giveaway they have already entered THEN THE SYSTEM SHALL reject the duplicate and leave the existing entry untouched.

### REQ-005 — Admin menu command
**Event-driven.** WHEN a player with the `enthusiagiveaway.admin` permission executes `/giveaway admin` THE SYSTEM SHALL open an admin menu listing scheduled and active giveaways with controls to schedule a new giveaway or cancel an existing one.

### REQ-006 — Schedule a giveaway
**Event-driven.** WHEN an admin completes the schedule wizard with a title, duration, console command, and winner count THE SYSTEM SHALL persist a new giveaway with state ACTIVE and a computed `ends_at` timestamp equal to now plus the supplied duration.

### REQ-007 — Draw winners on expiry
**State-driven.** WHILE a giveaway is in state ACTIVE and the current time is greater than or equal to its `ends_at` timestamp THE SYSTEM SHALL transition the giveaway to state DRAWING and select up to `max_winners` distinct random winners from its entry set.

### REQ-008 — Execute the configured command per winner
**Event-driven.** WHEN winners are selected for a giveaway THE SYSTEM SHALL execute the giveaway's configured console command once per winner with PlaceholderAPI placeholders expanded against the winner's player context.

### REQ-009 — Celebrate the winners in chat
**Event-driven.** WHEN winners are announced THE SYSTEM SHALL broadcast a celebration message containing each winner's display name and pixel-art skin head to the configured audience (server-wide by default).

### REQ-010 — Empty command guard
**Unwanted.** IF a giveaway's configured console command is empty or blank THEN THE SYSTEM SHALL skip command execution, log a warning naming the giveaway, and still perform the winner announcement.

### REQ-011 — Resume in-flight state after restart
**State-driven.** WHILE the plugin is starting up THE SYSTEM SHALL load every giveaway whose state is SCHEDULED, ACTIVE, or DRAWING from the database and resume its lifecycle without losing entries.

### REQ-012 — Fewer entries than max winners
**Unwanted.** IF a giveaway's entry count at draw time is less than its `max_winners` THEN THE SYSTEM SHALL draw all available entries as winners and transition the giveaway to state COMPLETED.

### REQ-013 — Cancel a giveaway
**Event-driven.** WHEN an admin cancels an active or scheduled giveaway from the admin menu THE SYSTEM SHALL transition the giveaway to state CANCELLED, skip winner selection, and broadcast a cancellation notice to participants.

---

## Interfaces & contracts

### REQ-020 — Player command surface
**Ubiquitous.** THE SYSTEM SHALL register the `/giveaway` command guarded by the `enthusiagiveaway.use` permission.

### REQ-021 — Admin subcommand surface
**Ubiquitous.** THE SYSTEM SHALL register the `/giveaway admin` subcommand guarded by the `enthusiagiveaway.admin` permission.

### REQ-022 — Storage engine
**Ubiquitous.** THE SYSTEM SHALL store all giveaway, entry, and winner data in a SQLite database file under the plugin data folder.

### REQ-023 — Configuration surface
**Ubiquitous.** THE SYSTEM SHALL read its celebration templates, broadcast scope, scheduler poll interval, and pixel-art settings from `config.yml` in the plugin data folder.

---

## Non-functional

### REQ-040 — Scheduler responsiveness
**State-driven.** WHILE the server is running THE SYSTEM SHALL poll for expired giveaways at the interval configured in `config.yml` (default one second) and never block the main thread for more than fifty milliseconds per tick.

### REQ-041 — Auditability
**Ubiquitous.** THE SYSTEM SHALL log every schedule, cancel, and draw action with the actor UUID (admin or `CONSOLE`), giveaway ID, and timestamp at INFO level.

### REQ-042 — Database failure isolation
**Unwanted.** IF a database write fails during entry recording THEN THE SYSTEM SHALL inform the player the entry could not be saved and leave the giveaway state unchanged.

---

## Acceptance

### REQ-100 — Domain purity
**Event-driven.** WHEN the test suite runs THE SYSTEM SHALL contain no Bukkit, Paper, Exposed, Hikari, InventoryFramework, or Nexus imports inside the `net.badgersmc.giveaway.domain` package.

### REQ-101 — Application purity
**Event-driven.** WHEN the test suite runs THE SYSTEM SHALL contain only domain imports inside the `net.badgersmc.giveaway.application` package.

### REQ-102 — Draw fairness
**Event-driven.** WHEN ten thousand simulated draws of three winners from one hundred entries are run THE SYSTEM SHALL produce each entry as a winner within five percent of the expected uniform frequency.

---

## Authoring rules

1. Every REQ has a single ID, a heading, and exactly one EARS-formatted sentence under a **pattern label** (Ubiquitous / Event-driven / State-driven / Unwanted / Optional).
2. Use `/spear:spec` to add or revise REQ entries — it runs the EARS validator (`plugins/spear/hooks/lib/ears.mjs`) and assigns the next free ID.
3. Never reuse an ID. When a requirement is obsolete, strike it through and note the deprecation date; do not renumber.
