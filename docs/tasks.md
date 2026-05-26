# Tasks — EnthusiaGiveaway

**Date:** 2026-05-25
**Status:** Bootstrap (emitted by `/spear:init`; extend via `/spear:spec`)

Tags: `TDD` (failing test before code), `DOC` (markdown / template authoring), `INFRA` (manifests, CI, repo plumbing).
State legend: `[ ]` not started, `[~]` in progress, `[x]` done, `[!]` blocked.

Each task carries `References:` (REQ-IDs + spec sections consulted) and `Evidence:` (sources consulted as work proceeds — an empty block blocks advancement past `spec-done` per REQ-030).

Tasks are ordered to honour state-machine and architectural dependencies. Independent tasks within a milestone may be parallelised.

---

## Milestone M0 — Bootstrap & skeleton

### INFRA tasks

- [x] **INFRA-01** — Initialise SPEAR docs + Gradle scaffold
  References: REQ-022, REQ-023, REQ-100, REQ-101
  Tag: INFRA
  Description: Create build.gradle.kts, settings.gradle.kts, paper-plugin.yml, config.yml, .gitignore, README.md, and the four SPEAR docs. Konsist `LayerRulesTest` template substituted with `net.badgersmc.giveaway`. Initial commit lands.
  Evidence: `D:/BadgersMC-Dev/LumaSG/build.gradle.kts:1-100; D:/BadgersMC-Dev/LumaGuilds/build.gradle.kts:1-60; C:/Users/Noah/.claude/plugins/cache/BadgersMC-spear-plugin/spear/0.1.0/templates/LayerRulesTest.kt`

- [x] **INFRA-02** — Gradle wrapper + GitHub Actions build workflow
  References: REQ-022
  Tag: INFRA
  Description: Run `gradle wrapper --gradle-version 8.10`. Add `.github/workflows/build.yml` running `./gradlew build` on push + PR. Verify `./gradlew test shadowJar` succeeds locally.
  Evidence: `gradle/wrapper/gradle-wrapper.properties (8.5→8.10); .github/workflows/build.yml (push main + PR, JDK 21 Temurin, Gradle cache, test --no-daemon; shadowJar skipped in CI — nexus-core/nexus-paper only in mavenLocal); ./gradlew test shadowJar --no-daemon BUILD SUCCESSFUL in 29s locally`

- [x] **INFRA-03** — SQLite schema migration runner
  References: REQ-001, REQ-022
  Tag: INFRA
  Description: Create `infrastructure/persistence/Migrations.kt` that opens the SQLite file under the plugin data folder via Hikari and runs Exposed `SchemaUtils.createMissingTablesAndColumns(Giveaways, Entries, Winners)`. Wire into `onEnable`.
  Evidence: `src/main/kotlin/.../infrastructure/persistence/{Tables,DatabaseFactory,Migrations}.kt; EnthusiaGiveawayPlugin.onEnable wires it; MigrationsIntegrationTest passes against on-disk SQLite in @TempDir; runtime classpath (Hikari/Exposed/sqlite-jdbc) needs PaperLoader (out of scope this task — TBD M1)`

### TDD tasks — domain

- [x] **TDD-10** — RED: Giveaway state transitions
  References: REQ-007, REQ-013, implementation.md §3.1
  Tag: TDD
  Description: Write `src/test/kotlin/.../domain/GiveawayTest.kt` asserting legal transitions (SCHEDULED→ACTIVE→DRAWING→COMPLETED; *→CANCELLED) and illegal transitions throw `IllegalStateException`. Run, confirm red.
  Evidence: `docs/requirements.md#REQ-007; docs/requirements.md#REQ-013; docs/implementation.md#3.1; D:/BadgersMC-Dev/LumaSG/src/main/kotlin/net/lumalyte/lumasg/domain/GamePhase.kt:1-13; docs/tech-stack.md#3 (org.junit.jupiter:junit-jupiter:5.10.0 baseline)`

- [x] **TDD-11** — GREEN: Implement `Giveaway` + `GiveawayState`
  References: REQ-007, REQ-013, implementation.md §3.1
  Tag: TDD
  Description: Create `domain/Giveaway.kt` + `domain/GiveawayState.kt` with the transition function. Domain-only — no Bukkit imports. Flip TDD-10 to green.
  Evidence: `src/main/kotlin/net/badgersmc/giveaway/domain/Giveaway.kt; src/main/kotlin/net/badgersmc/giveaway/domain/GiveawayId.kt; src/main/kotlin/net/badgersmc/giveaway/domain/GiveawayState.kt; java.time.Instant + java.util.UUID (JDK stdlib)`

- [x] **TDD-12** — RED: `RandomDraw` picks k distinct, returns all if fewer
  References: REQ-007, REQ-012, REQ-102
  Tag: TDD
  Description: Write `domain/RandomDrawTest.kt` asserting: picking k=3 from 100 returns 3 distinct elements; picking k=5 from 2 returns those 2. Use a seeded `kotlin.random.Random` port for determinism.
  Evidence: `docs/requirements.md#REQ-007; docs/requirements.md#REQ-012; docs/requirements.md#REQ-102; docs/implementation.md#3.1 (domain ports); kotlin.random.Random (Kotlin stdlib)`

- [x] **TDD-13** — GREEN: `SeededRandomDraw` implementation
  References: REQ-007, REQ-012
  Tag: TDD
  Description: Implement `domain/RandomDraw.kt` port + `domain/SeededRandomDraw.kt`. Flip TDD-12 to green.
  Evidence: `src/main/kotlin/net/badgersmc/giveaway/domain/ports/RandomDraw.kt; src/main/kotlin/net/badgersmc/giveaway/domain/SeededRandomDraw.kt; kotlin.random.Random (Kotlin stdlib)`

### TDD tasks — application

- [x] **TDD-20** — RED: `EnterGiveaway` rejects duplicate entry
  References: REQ-003, REQ-004
  Tag: TDD
  Description: Write `application/EnterGiveawayTest.kt` using Mockk fakes for `GiveawayRepository` + `EntryRepository`. Assert duplicate entry returns `EnterResult.AlreadyEntered` and does not call `EntryRepository.insert` twice.
  Evidence: `docs/requirements.md#REQ-003; docs/requirements.md#REQ-004; docs/implementation.md#3.1 (domain ports); docs/implementation.md#3.2 (application use cases); build.gradle.kts (io.mockk:mockk:1.13.10 baseline); io.mockk; src/main/kotlin/net/badgersmc/giveaway/domain/Giveaway.kt`

- [x] **TDD-21** — GREEN: `EnterGiveaway` use case
  References: REQ-003, REQ-004
  Tag: TDD
  Description: Implement `application/EnterGiveaway.kt`. Flip TDD-20 to green.
  Evidence: `src/main/kotlin/net/badgersmc/giveaway/application/EnterGiveaway.kt; src/main/kotlin/net/badgersmc/giveaway/application/EnterResult.kt; src/main/kotlin/net/badgersmc/giveaway/domain/Entry.kt; src/main/kotlin/net/badgersmc/giveaway/domain/ports/Clock.kt; src/main/kotlin/net/badgersmc/giveaway/domain/ports/GiveawayRepository.kt; src/main/kotlin/net/badgersmc/giveaway/domain/ports/EntryRepository.kt; Konsist LayerRulesTest re-enabled now that application/ has files`

- [x] **TDD-22** — RED: `DrawWinners` orchestrates draw → command → broadcast
  References: REQ-007, REQ-008, REQ-009, REQ-010, REQ-012
  Tag: TDD
  Description: Write `application/DrawWinnersTest.kt` with mocks for all ports. Assert order: state→DRAWING, RandomDraw called, winners persisted, CommandExecutor called once per winner with expanded placeholders, CelebrationBroadcaster called, state→COMPLETED. Separate test for empty command (REQ-010).
  Evidence: `docs/requirements.md#REQ-007,008,009,010,012; docs/implementation.md#3.1,#3.2,#4.3; src/main/kotlin/net/badgersmc/giveaway/domain/Giveaway.kt; src/main/kotlin/net/badgersmc/giveaway/domain/ports/RandomDraw.kt; io.mockk (build.gradle.kts baseline)`

- [x] **TDD-23** — GREEN: `DrawWinners` use case
  References: REQ-007, REQ-008, REQ-009, REQ-010, REQ-012
  Tag: TDD
  Description: Implement `application/DrawWinners.kt`. Flip TDD-22 to green.
  Evidence: `src/main/kotlin/net/badgersmc/giveaway/application/DrawWinners.kt; src/main/kotlin/net/badgersmc/giveaway/application/DrawResult.kt; src/main/kotlin/net/badgersmc/giveaway/domain/Winner.kt; src/main/kotlin/net/badgersmc/giveaway/domain/WinnerHandle.kt; new ports WinnerRepository, CommandExecutor, PlaceholderExpander, PlayerNameLookup, CelebrationBroadcaster, Logger; GiveawayRepository.save + EntryRepository.playerUuidsFor added`

### TDD tasks — architecture

- [x] **TDD-30** — Konsist layer test passes against initial domain types
  References: REQ-100, REQ-101, implementation.md §2
  Tag: TDD
  Description: After TDD-11/13/21/23 land, confirm `LayerRulesTest` passes. If it fails, the violation indicates a misplaced import — fix by moving the type or introducing a port.
  Evidence: `src/test/kotlin/net/badgersmc/giveaway/architecture/LayerRulesTest.kt; ./gradlew test passes 1/1 on this class (21/21 total suite). Domain has zero infra/framework imports, application depends only on domain ports.`

### DOC tasks

- [x] **DOC-40** — Pin Starlight Skins API contract snapshot
  References: REQ-009, tech-stack.md §4
  Tag: DOC
  Description: Create `docs/refs/starlight-skins.md` with the URL pattern, response format (PNG), and known size/face-render parameters. Note the 5s timeout discipline from LumaSG.
  Evidence: `docs/refs/starlight-skins.md; D:/BadgersMC-Dev/LumaSG/src/main/kotlin/net/lumalyte/lumasg/game/Celebration.kt:60-180; config.yml celebration.pixel-art block; docs/tech-stack.md §4`

---

## Milestone M1 — Player path end-to-end

Goal: a player can run `/giveaway`, see the menu, click an active giveaway, and enter it — against a real SQLite-backed plugin running on Paper. Closes REQ-002, REQ-003, REQ-020, plus the runtime infrastructure to make any of M0 actually load.

### INFRA tasks

- [ ] **INFRA-04** — Paper plugin loader for runtime libraries
  References: REQ-022, tech-stack.md §3
  Tag: INFRA
  Description: Add `EnthusiaGiveawayLoader` (PluginLoader) that uses Paper's `MavenLibraryResolver` to fetch Hikari, Exposed (core/dao/jdbc/java-time), sqlite-jdbc, and kotlinx-coroutines at startup. Wire `loader:` in `paper-plugin.yml`. Mirror the pattern in `D:/BadgersMC-Dev/LumaSG/src/main/kotlin/.../LumaSGLoader.kt`. Verify `./gradlew shadowJar` produces a jar that boots on a local Paper server.
  Evidence: ` `

- [ ] **INFRA-05** — Nexus DI module wiring
  References: tech-stack.md §3, implementation.md §3.9
  Tag: INFRA
  Description: Create `infrastructure/nexus/ServiceModule.kt` registering every adapter and use case as a `@Service`. Bootstrap `NexusContext` in `EnthusiaGiveawayPlugin.onEnable` after `Migrations.run()`. Resolve `EnterGiveaway` and `DrawWinners` from the context to prove wiring works.
  Evidence: ` `

- [ ] **INFRA-06** — `BukkitClock` + `BukkitNameLookup` adapters
  References: REQ-008, implementation.md §3.1
  Tag: INFRA
  Description: Implement `infrastructure/bukkit/BukkitClock` (`Instant.now()`) and `BukkitNameLookup` (`Bukkit.getOfflinePlayer(uuid).name ?: "Unknown"`). No tests required — adapters are trivial pass-throughs.
  Evidence: ` `

- [ ] **INFRA-07** — `GiveawayCommand` registration
  References: REQ-002, REQ-020
  Tag: INFRA
  Description: Implement Bukkit `CommandExecutor` + tab-completer for `/giveaway`. Routes bare `/giveaway` → `PlayerGiveawayMenu.open(player)`. Permission `enthusiagiveaway.use`. Register in `onEnable`.
  Evidence: ` `

### TDD tasks — persistence adapters

- [ ] **TDD-40** — RED: `ExposedGiveawayRepository` round-trip
  References: REQ-001, REQ-022, implementation.md §3.3
  Tag: TDD
  Description: Integration test in `infrastructure/persistence` exercising save + findById + listActive against in-memory or @TempDir SQLite. Assert state survives a `transaction { }` boundary.
  Evidence: ` `

- [ ] **TDD-41** — GREEN: `ExposedGiveawayRepository` implementation
  References: REQ-001, REQ-022
  Tag: TDD
  Description: Implement `infrastructure/persistence/ExposedGiveawayRepository` against `GiveawaysTable`. Flip TDD-40 green.
  Evidence: ` `

- [ ] **TDD-42** — RED: `ExposedEntryRepository` enforces composite PK
  References: REQ-003, REQ-004
  Tag: TDD
  Description: Integration test asserting `insert` then `hasEntered` returns true, a second `insert` throws (PK collision), and `playerUuidsFor` returns the inserted UUID.
  Evidence: ` `

- [ ] **TDD-43** — GREEN: `ExposedEntryRepository` implementation
  References: REQ-003, REQ-004
  Tag: TDD
  Description: Implement `infrastructure/persistence/ExposedEntryRepository`. Catch the PK collision in `insert` and rethrow as a typed exception (or document the raw `ExposedSQLException` if cheaper). Flip TDD-42 green.
  Evidence: ` `

### TDD tasks — application

- [ ] **TDD-44** — RED: `ListActiveGiveaways` returns annotated summaries
  References: REQ-002
  Tag: TDD
  Description: Application test with mocked repos. Returns `List<GiveawaySummary(id, title, secondsRemaining, entryCount, alreadyEntered)>` for ACTIVE giveaways only, computed against a fake `Clock`. Each row's `alreadyEntered` flag reflects `EntryRepository.hasEntered(id, viewerUuid)`.
  Evidence: ` `

- [ ] **TDD-45** — GREEN: `ListActiveGiveaways` use case + `GiveawaySummary`
  References: REQ-002
  Tag: TDD
  Description: Implement `application/ListActiveGiveaways` + `application/GiveawaySummary`. Add `GiveawayRepository.listByState(state)` port method. Flip TDD-44 green.
  Evidence: ` `

### INFRA tasks — player GUI

- [ ] **INFRA-08** — `PlayerGiveawayMenu` (InventoryFramework)
  References: REQ-002, REQ-003
  Tag: INFRA
  Description: Paginated chest GUI listing `GiveawaySummary` rows. Each row: title, lore with `secondsRemaining`, entry count, and "Click to enter" or "Entered ✓". Click calls `EnterGiveaway`; on `Success` refresh menu and play sound; on `AlreadyEntered`/`NotActive` show actionbar message. Reference `D:/BadgersMC-Dev/LumaGuilds/src/main/kotlin/.../guildsLib/menu/` for IF 0.11.6 idioms.
  Evidence: ` `

---

## Milestone M2 — Admin path end-to-end

Goal: an admin can schedule a giveaway via GUI wizard, see scheduled/active giveaways in the admin menu, cancel one, and have the scheduler actually fire `DrawWinners` on expiry. Closes REQ-005, REQ-006, REQ-013, REQ-021, REQ-040, REQ-011.

### TDD tasks — application use cases

- [ ] **TDD-50** — RED: `ScheduleGiveaway` validates and persists
  References: REQ-006
  Tag: TDD
  Description: Application test asserting that valid input produces an ACTIVE giveaway with `endsAt = clock.now() + duration`, `maxWinners >= 1`, non-blank title, command persisted verbatim. Invalid inputs (negative duration, zero winners, blank title) return a typed `ScheduleResult.Invalid(field, reason)` and do not persist.
  Evidence: ` `

- [ ] **TDD-51** — GREEN: `ScheduleGiveaway` use case + `ScheduleResult`
  References: REQ-006
  Tag: TDD
  Description: Implement `application/ScheduleGiveaway` + sealed `ScheduleResult`. Flip TDD-50 green.
  Evidence: ` `

- [ ] **TDD-52** — RED: `CancelGiveaway` transitions and notifies
  References: REQ-013
  Tag: TDD
  Description: Application test. SCHEDULED/ACTIVE → CANCELLED is allowed and persisted; `CelebrationBroadcaster.notifyCancellation(giveaway)` is called. DRAWING/COMPLETED → returns `CancelResult.AlreadyFinal` and does not transition.
  Evidence: ` `

- [ ] **TDD-53** — GREEN: `CancelGiveaway` use case + add `notifyCancellation` to broadcaster port
  References: REQ-013
  Tag: TDD
  Description: Extend `CelebrationBroadcaster` with `notifyCancellation(Giveaway)`. Implement `application/CancelGiveaway`. Flip TDD-52 green.
  Evidence: ` `

- [ ] **TDD-54** — RED: `ResumeGiveawaysOnStartup` handles DRAWING + ACTIVE rows
  References: REQ-011
  Tag: TDD
  Description: Application test asserting: rows with state DRAWING get `DrawWinners` re-invoked (idempotent via composite PK on Winners); rows with state ACTIVE that are already past `endsAt` are drawn immediately; rows with state ACTIVE in the future are left alone.
  Evidence: ` `

- [ ] **TDD-55** — GREEN: `ResumeGiveawaysOnStartup` use case
  References: REQ-011
  Tag: TDD
  Description: Implement `application/ResumeGiveawaysOnStartup`. Flip TDD-54 green.
  Evidence: ` `

### TDD tasks — persistence

- [ ] **TDD-56** — RED + GREEN: `ExposedWinnerRepository`
  References: REQ-011
  Tag: TDD
  Description: Integration test exercising insert idempotency (re-insert of same `(giveawayId, playerUuid)` is a no-op or typed-skip, not a thrown exception, so resume-on-restart works). Implement to make green.
  Evidence: ` `

### INFRA tasks — admin GUI + scheduling

- [ ] **INFRA-09** — `AdminGiveawayMenu` (InventoryFramework)
  References: REQ-005, REQ-021
  Tag: INFRA
  Description: Chest GUI listing SCHEDULED + ACTIVE giveaways with cancel buttons. A "Schedule new" button opens `ScheduleWizard`. Permission `enthusiagiveaway.admin`.
  Evidence: ` `

- [ ] **INFRA-10** — `ScheduleWizard` (anvil text input chain)
  References: REQ-006
  Tag: INFRA
  Description: Sequential IF anvil prompts: title → duration (parse `1h30m`/`45m`/`2d` via dedicated parser) → command → winner count → confirm screen. On confirm, call `ScheduleGiveaway`. On any invalid input, re-prompt with hint. Reuse the duration parser as a pure function (covered by its own micro-test, TDD-57 if needed).
  Evidence: ` `

- [ ] **INFRA-11** — `BukkitTickScheduler` + `DrawWinners` dispatch
  References: REQ-007, REQ-040
  Tag: INFRA
  Description: Bukkit task scheduled every `scheduler.poll-interval-seconds` (default 1s) on the Nexus `BukkitDispatcher`. Loads `GiveawayRepository.listByState(ACTIVE)` filtered by `endsAt <= clock.now()`, dispatches each through `DrawWinners` in a coroutine. Must never block main thread > 50ms (REQ-040) — move heavy work off-main with `withContext(Dispatchers.IO)`.
  Evidence: ` `

- [ ] **INFRA-12** — `BukkitCommandExecutor` + `PlaceholderApiExpander` adapters
  References: REQ-008
  Tag: INFRA
  Description: `BukkitCommandExecutor` dispatches via `Bukkit.dispatchCommand(consoleSender, line)` on the main thread. `PlaceholderApiExpander` calls `PlaceholderAPI.setPlaceholders(offlinePlayer, template)` if PAPI is loaded, otherwise returns the template unchanged. Also replaces `<player>` / `<name>` literally (matches LumaSG pattern).
  Evidence: ` `

- [ ] **INFRA-13** — `Slf4jLogger` adapter
  References: REQ-041, implementation.md §3.1
  Tag: INFRA
  Description: Bridge `domain.ports.Logger` to the plugin's `java.util.logging.Logger`. Implement `info` + `warn` pass-throughs.
  Evidence: ` `

- [ ] **INFRA-14** — Hook resume on `onEnable`
  References: REQ-011
  Tag: INFRA
  Description: After `Migrations.run()` and Nexus bootstrap, resolve `ResumeGiveawaysOnStartup` and call it once. Log the count of giveaways resumed.
  Evidence: ` `

---

## Milestone M3 — Celebration broadcaster

Goal: when winners are drawn, the configured audience sees a styled chat broadcast with each winner's pixel-art skin head, MiniMessage gradient title, and optional fireworks. Directly ports `D:/BadgersMC-Dev/LumaSG/src/main/kotlin/net/lumalyte/lumasg/game/Celebration.kt`. Closes REQ-009.

### INFRA tasks

- [ ] **INFRA-15** — `BukkitCelebrationBroadcaster` (pixel-art head + title)
  References: REQ-009, implementation.md §3.8, docs/refs/starlight-skins.md
  Tag: INFRA
  Description: Port `renderPixelArtHead` from LumaSG. Fetches each winner's `face` render from Starlight Skins API (5s timeouts, silent skip on fail, `User-Agent: EnthusiaGiveaway-Plugin`). Renders an 8×8 grid of MiniMessage-coloured ⬛ characters with title/winner/border annotation lines beside the art. Broadcast scope (server | participants) from `config.yml`. Use Adventure `TextColor.color(r,g,b)` for per-pixel tint.
  Evidence: ` `

- [ ] **INFRA-16** — Firework feedback (optional)
  References: REQ-009
  Tag: INFRA
  Description: If `celebration.fireworks.enabled`, spawn `celebration.fireworks.count` randomly coloured fireworks at each winner's location, one every 250ms via coroutine. Each firework tagged with a metadata key so it can't be picked up. Skip silently if winner is offline.
  Evidence: ` `

- [ ] **INFRA-17** — `notifyNew` + `notifyCancellation` broadcaster methods
  References: REQ-006, REQ-013
  Tag: INFRA
  Description: Add lightweight broadcast methods to `CelebrationBroadcaster` for new-giveaway announcement (gated by `broadcast-new-giveaway: true`) and cancellation notice (visible to entrants only). MiniMessage templates from `config.yml` (add new keys: `templates.new`, `templates.cancelled`).
  Evidence: ` `

### DOC tasks

- [ ] **DOC-50** — Update `implementation.md` §3 with adapter wiring
  References: implementation.md §3.3–§3.9
  Tag: DOC
  Description: Once M1–M3 adapters land, revise §3.3 through §3.9 to reference the actual class names and explain any deviations from the original blueprint. Note the PaperLoader runtime-dep strategy in §3.9.
  Evidence: ` `

---

## Task authoring rules

1. Every task has exactly ONE tag (`TDD`, `DOC`, or `INFRA`).
2. `References:` cites at least one REQ-ID from `requirements.md`. If the REQ doesn't exist, run `/spear:spec` first.
3. `Evidence:` starts empty (`\ \``). It must be filled before any skill past `spec-done` will run (REQ-030). Each line is a verified source (e.g. `context7:exposed@0.55.0/dsl`, `src/domain/Giveaway.kt:42`, `docs/implementation.md#3.1`).
4. Task size ceiling: ~1500 tokens of full briefing. If larger, split.
5. A task MUST be achievable by a single SPEAR cycle (`spec → prove → engine → arch → refine` for TDD; `spec → arch → refine` for DOC/INFRA).
6. Mark state as work proceeds: `[~]` when entering `spec`; `[x]` only when `/spear:refine` has cleared state to `idle`.
