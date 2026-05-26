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

- [ ] **INFRA-02** — Gradle wrapper + GitHub Actions build workflow
  References: REQ-022
  Tag: INFRA
  Description: Run `gradle wrapper --gradle-version 8.10`. Add `.github/workflows/build.yml` running `./gradlew build` on push + PR. Verify `./gradlew test shadowJar` succeeds locally.
  Evidence: ` `

- [ ] **INFRA-03** — SQLite schema migration runner
  References: REQ-001, REQ-022
  Tag: INFRA
  Description: Create `infrastructure/persistence/Migrations.kt` that opens the SQLite file under the plugin data folder via Hikari and runs Exposed `SchemaUtils.createMissingTablesAndColumns(Giveaways, Entries, Winners)`. Wire into `onEnable`.
  Evidence: ` `

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

- [ ] **TDD-12** — RED: `RandomDraw` picks k distinct, returns all if fewer
  References: REQ-007, REQ-012, REQ-102
  Tag: TDD
  Description: Write `domain/RandomDrawTest.kt` asserting: picking k=3 from 100 returns 3 distinct elements; picking k=5 from 2 returns those 2. Use a seeded `kotlin.random.Random` port for determinism.
  Evidence: ` `

- [ ] **TDD-13** — GREEN: `SeededRandomDraw` implementation
  References: REQ-007, REQ-012
  Tag: TDD
  Description: Implement `domain/RandomDraw.kt` port + `domain/SeededRandomDraw.kt`. Flip TDD-12 to green.
  Evidence: ` `

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

- [ ] **TDD-22** — RED: `DrawWinners` orchestrates draw → command → broadcast
  References: REQ-007, REQ-008, REQ-009, REQ-010, REQ-012
  Tag: TDD
  Description: Write `application/DrawWinnersTest.kt` with mocks for all ports. Assert order: state→DRAWING, RandomDraw called, winners persisted, CommandExecutor called once per winner with expanded placeholders, CelebrationBroadcaster called, state→COMPLETED. Separate test for empty command (REQ-010).
  Evidence: ` `

- [ ] **TDD-23** — GREEN: `DrawWinners` use case
  References: REQ-007, REQ-008, REQ-009, REQ-010, REQ-012
  Tag: TDD
  Description: Implement `application/DrawWinners.kt`. Flip TDD-22 to green.
  Evidence: ` `

### TDD tasks — architecture

- [ ] **TDD-30** — Konsist layer test passes against initial domain types
  References: REQ-100, REQ-101, implementation.md §2
  Tag: TDD
  Description: After TDD-11/13/21/23 land, confirm `LayerRulesTest` passes. If it fails, the violation indicates a misplaced import — fix by moving the type or introducing a port.
  Evidence: ` `

### DOC tasks

- [ ] **DOC-40** — Pin Starlight Skins API contract snapshot
  References: REQ-009, tech-stack.md §4
  Tag: DOC
  Description: Create `docs/refs/starlight-skins.md` with the URL pattern, response format (PNG), and known size/face-render parameters. Note the 5s timeout discipline from LumaSG.
  Evidence: ` `

---

## Milestone M1 — Player path end-to-end

(Empty — populate after M0 completes via `/spear:spec`.)

---

## Milestone M2 — Admin path end-to-end

(Empty — populate after M1 completes.)

---

## Milestone M3 — Celebration broadcaster

(Empty — populate after M2 completes. Will heavily reference `D:/BadgersMC-Dev/LumaSG/src/main/kotlin/net/lumalyte/lumasg/game/Celebration.kt`.)

---

## Task authoring rules

1. Every task has exactly ONE tag (`TDD`, `DOC`, or `INFRA`).
2. `References:` cites at least one REQ-ID from `requirements.md`. If the REQ doesn't exist, run `/spear:spec` first.
3. `Evidence:` starts empty (`\ \``). It must be filled before any skill past `spec-done` will run (REQ-030). Each line is a verified source (e.g. `context7:exposed@0.55.0/dsl`, `src/domain/Giveaway.kt:42`, `docs/implementation.md#3.1`).
4. Task size ceiling: ~1500 tokens of full briefing. If larger, split.
5. A task MUST be achievable by a single SPEAR cycle (`spec → prove → engine → arch → refine` for TDD; `spec → arch → refine` for DOC/INFRA).
6. Mark state as work proceeds: `[~]` when entering `spec`; `[x]` only when `/spear:refine` has cleared state to `idle`.
