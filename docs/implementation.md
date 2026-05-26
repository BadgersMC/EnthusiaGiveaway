# Implementation — EnthusiaGiveaway

**Date:** 2026-05-25
**Status:** Bootstrap (emitted by `/spear:init`; extend as components land)
**Owner:** BadgersMC

## 1. Repo layout (canonical)

```
EnthusiaGiveaway/
├── src/main/kotlin/net/badgersmc/giveaway/
│   ├── domain/             # rules of the game — zero framework imports
│   ├── application/        # use cases — imports domain only
│   └── infrastructure/     # adapters — imports anything
├── src/test/kotlin/net/badgersmc/giveaway/
│   └── architecture/       # Konsist layer-rule tests
├── src/main/resources/
│   ├── paper-plugin.yml
│   └── config.yml
├── docs/
│   ├── tech-stack.md
│   ├── requirements.md
│   ├── implementation.md
│   └── tasks.md
└── build.gradle.kts
```

## 2. Layer Dependency Rules

The three-layer discipline SPEAR enforces. `/spear:arch` reads this exact section and blocks on violations.

| Layer | Concrete files | May depend on |
|---|---|---|
| `domain/` (rules-of-the-game) | `src/main/kotlin/net/badgersmc/giveaway/domain/**` | nothing outside `domain/` + Kotlin stdlib |
| `application/` (use cases / workflow) | `src/main/kotlin/net/badgersmc/giveaway/application/**` | `domain/` only |
| `infrastructure/` (adapters, frameworks, I/O) | `src/main/kotlin/net/badgersmc/giveaway/infrastructure/**` | anything |

Violations are reported as `file:line:symbol`. Suggested fixes: move the offending type, introduce a port interface in `domain/`, or relocate framework wiring to `infrastructure/`.

## Forbidden Domain Annotations

Framework annotations and packages that must NOT appear on any type under `domain/**`. `/spear:arch` scans for these.

```yaml
forbidden:
  - org.bukkit
  - io.papermc
  - org.jetbrains.exposed
  - com.zaxxer.hikari
  - com.github.stefvanschie.inventoryframework
  - net.badgersmc.nexus
  - me.clip.placeholderapi
```

## 3. Component design

### 3.1 Domain model (`domain/`)

Pure Kotlin value types and aggregate roots.

- Layer: `domain/`
- Types:
  - `GiveawayId` (value class wrapping `UUID`)
  - `Giveaway` (data class: id, title, command, scheduledAt, endsAt, maxWinners, state, createdBy)
  - `GiveawayState` (enum: `SCHEDULED`, `ACTIVE`, `DRAWING`, `COMPLETED`, `CANCELLED`)
  - `Entry` (giveawayId, playerUuid, enteredAt)
  - `Winner` (giveawayId, playerUuid, drawnAt)
- Ports / interfaces (declared in `domain/ports/`):
  - `GiveawayRepository`, `EntryRepository`, `WinnerRepository`
  - `Clock` (returns `Instant`)
  - `RandomDraw` (picks `k` distinct from a list)
  - `CommandExecutor` (dispatches a console command string)
  - `PlaceholderExpander` (expands `%placeholder%` against a player UUID + display name)
  - `CelebrationBroadcaster` (announces winners; takes scope, winners, giveaway)
  - `Logger` (port — keeps domain free of `org.slf4j`)
- Adapters: none — domain owns no I/O
- Evidence sources consulted: [Celebration.kt](../../LumaSG/src/main/kotlin/net/lumalyte/lumasg/game/Celebration.kt) for celebration shape; LumaGuilds `docs/domain.md` for SPEAR domain layout

### 3.2 Application use cases (`application/`)

Coordinator functions/classes for each user-visible action.

- Layer: `application/`
- Use cases:
  - `ScheduleGiveaway` — validates inputs, persists new `Giveaway`
  - `EnterGiveaway` — checks active + not-already-entered, persists `Entry`
  - `ListActiveGiveaways` — returns active giveaways with entry counts for the requesting player
  - `CancelGiveaway` — flips state to `CANCELLED`
  - `DrawWinners` — orchestrates state transition, random draw, command execution, broadcast
  - `ResumeGiveawaysOnStartup` — loads SCHEDULED / ACTIVE / DRAWING giveaways at boot
- Depends only on domain ports
- Evidence sources consulted: LumaGuilds [docs/application.md](../../LumaGuilds/docs/application.md)

### 3.3 Persistence adapter (`infrastructure/persistence/`)

- Layer: `infrastructure/`
- Adapter: `ExposedGiveawayRepository`, `ExposedEntryRepository`, `ExposedWinnerRepository` using Exposed DSL over a `HikariDataSource` pointing at `plugins/EnthusiaGiveaway/giveaways.db`
- Schema migrations: `Migrations.kt` runs `SchemaUtils.createMissingTablesAndColumns` on startup
- Tables: `Giveaways`, `Entries` (composite PK on giveawayId + playerUuid → enforces REQ-004), `Winners`

### 3.4 GUI menus (`infrastructure/menus/`)

- Layer: `infrastructure/`
- Library: InventoryFramework 0.11.6
- Menus:
  - `PlayerGiveawayMenu` — paginated active list, click to enter
  - `AdminGiveawayMenu` — admin list with schedule + cancel controls
  - `ScheduleWizard` — sequence of anvil text inputs (title → duration → command → winners), final confirm screen
- Duration parser: accepts `1h`, `30m`, `1h30m`, `2d`, etc. (impl. detail of wizard infra)

### 3.5 Commands (`infrastructure/commands/`)

- Layer: `infrastructure/`
- `GiveawayCommand` — Bukkit `CommandExecutor` + tab-completer. Routes:
  - `/giveaway` → `PlayerGiveawayMenu.open(player)`
  - `/giveaway admin` → permission check → `AdminGiveawayMenu.open(player)`

### 3.6 Scheduler (`infrastructure/schedule/`)

- Layer: `infrastructure/`
- `BukkitTickScheduler` — Bukkit task running every `poll-interval-seconds`, calls `DrawWinners.tick()` on the Nexus `BukkitDispatcher` coroutine scope

### 3.7 PlaceholderAPI bridge (`infrastructure/papi/`)

- Layer: `infrastructure/`
- `PlaceholderApiExpander` — calls `PlaceholderAPI.setPlaceholders(offlinePlayer, command)` if PAPI is loaded; no-op pass-through otherwise

### 3.8 Celebration broadcaster (`infrastructure/celebrate/`)

- Layer: `infrastructure/`
- `BukkitCelebrationBroadcaster` — port of LumaSG's [Celebration.kt](../../LumaSG/src/main/kotlin/net/lumalyte/lumasg/game/Celebration.kt). Fetches each winner's face render from Starlight Skins API, renders as colored pixel characters, sends with MiniMessage title + optional fireworks. Adapts the LumaSG single-winner shape to support N winners (one head block per winner).

### 3.9 Nexus wiring (`infrastructure/nexus/`)

- Layer: `infrastructure/`
- `ServiceModule` — registers all adapters as Nexus services; `EnthusiaGiveawayPlugin.onEnable` creates the `NexusContext`, scans for `@Service` annotations, wires use cases.

## 4. Data flows

### 4.1 Enter flow

1. Player runs `/giveaway`.
2. `GiveawayCommand` opens `PlayerGiveawayMenu`.
3. Menu calls `ListActiveGiveaways(playerId)` → returns list of `GiveawaySummary(id, title, secondsRemaining, entryCount, alreadyEntered)`.
4. Player clicks an unentered row → menu calls `EnterGiveaway(playerId, giveawayId)`.
5. `EnterGiveaway` checks state == ACTIVE, calls `EntryRepository.insertIfAbsent(...)`. PK collision → returns `AlreadyEntered`.
6. Menu refreshes; clicked row now shows "Entered ✓".

### 4.2 Schedule flow

1. Admin runs `/giveaway admin`.
2. Admin clicks "Schedule" → `ScheduleWizard` opens.
3. Anvil prompts collect title → duration → command → winners.
4. Final confirm → `ScheduleGiveaway(title, durationSec, command, winnerCount, adminId)`.
5. Use case persists `Giveaway(state=ACTIVE, endsAt=Clock.now()+duration)`.
6. If `broadcast-new-giveaway: true`, broadcast new giveaway notice via `CelebrationBroadcaster.notifyNew(...)`.

### 4.3 Draw flow

1. `BukkitTickScheduler` fires every `poll-interval-seconds`.
2. Calls `DrawWinners.tick()` which loads all `ACTIVE` giveaways with `endsAt <= Clock.now()`.
3. For each, flips state to `DRAWING` (atomic update), loads entries.
4. `RandomDraw.pick(maxWinners, entries)` returns winners (or all entries if fewer — REQ-012).
5. Persists `Winner` rows.
6. For each winner: `PlaceholderExpander.expand(command, winner) → CommandExecutor.dispatch(expanded)`.
7. `CelebrationBroadcaster.announce(giveaway, winners, scope)` → MiniMessage title + pixel-art heads + fireworks.
8. Flips state to `COMPLETED`.

### 4.4 Resume on startup

1. `EnthusiaGiveawayPlugin.onEnable` wires Nexus, runs migrations.
2. Calls `ResumeGiveawaysOnStartup()`:
   - `DRAWING` rows → re-run draw step (idempotent because `Winner` PK is `(giveawayId, playerUuid)`).
   - `SCHEDULED` / `ACTIVE` rows → loaded into in-memory cache; scheduler picks them up on next tick.

## 5. Briefing contract for subagent dispatch

Every worker dispatch (`Agent` tool call) for implementation work carries:

- Exact file paths to create / modify.
- Pre-verified signatures (from context7, library source on disk under `D:\BadgersMC-Dev\`, or `Grep`).
- The failing test (path + test name) for TDD tasks.
- Acceptance criteria — which test goes green; which files MUST NOT change.
- Forbidden actions — scope fences (e.g. "do not touch `domain/`").
- The task's `Evidence:` block verbatim.

Tasks whose full briefing exceeds ~1500 tokens are decomposed further by `/spear:spec` before dispatch.

## 6. Versioning

Semantic versioning. Start at `0.1.0`. Bump major on breaking public-API or DB schema change.

## 7. Out of scope (this doc)

- Per-component code-level docs — owned by each component's own KDoc.
- CI workflow YAML — owned by `tech-stack.md` §CI and the workflow file itself.
- Webhook / Discord announcement — explicit non-goal in `tech-stack.md` §8.
