# Tech Stack — EnthusiaGiveaway

**Date:** 2026-05-25
**Status:** Bootstrap (emitted by `/spear:init`; revise as the project evolves)
**Owner:** BadgersMC

## 1. What this project is

EnthusiaGiveaway is a Paper 1.21 plugin that lets admins schedule timed giveaways with a configurable winning console command, and lets players enter via a GUI menu. When a giveaway expires, the plugin draws N random winners, runs the configured command (PlaceholderAPI-expanded) per winner, and broadcasts a celebratory announcement that includes the winner's pixel-art skin head rendered into chat. Deploys as a single shaded jar to a BadgersMC Paper server alongside the Nexus runtime plugin.

## 2. Runtimes & languages

| Layer | Language / Tool | Min version | Reason |
|---|---|---|---|
| Plugin code | Kotlin (JVM) | 2.1.0 | Matches LumaSG / LumaGuilds across BadgersMC stack |
| Build tool | Gradle (Kotlin DSL) | 8.10+ | Shadow 8.3.5 requires Gradle 8+ |
| Test framework | JUnit Jupiter | 5.10.0 | Standard JVM test runner |
| JVM | JDK 21 | — | Paper 1.21.x requires Java 21 |
| CI runner | GitHub Actions | — | Matches existing BadgersMC repos |

Detected by `/spear:init` from `build.gradle.kts` (Gradle Kotlin DSL).

## 3. Runtime dependencies

| Package | Version | Why |
|---|---|---|
| io.papermc.paper:paper-api | 1.21.11-R0.1-SNAPSHOT | Server platform |
| net.badgersmc:nexus-core | 1.5.3 | DI + service registry |
| net.badgersmc:nexus-paper | 1.5.3 | Bukkit dispatcher + coroutines bridge |
| com.github.stefvanschie.inventoryframework:IF | 0.11.6 | GUI menus (player + admin wizard) |
| org.jetbrains.exposed:exposed-* | 0.55.0 | SQL DSL over SQLite (compileOnly; downloaded by Paper loader at startup) |
| com.zaxxer:HikariCP | 5.1.0 | Connection pool (compileOnly; downloaded at startup) |
| org.xerial:sqlite-jdbc | 3.45.1.0 | Embedded DB driver (compileOnly; downloaded at startup) |
| org.jetbrains.kotlinx:kotlinx-coroutines-core | 1.8.0 | Async scheduler ticks + celebration sequence |
| me.clip:placeholderapi | 2.11.6 | Optional hook for command placeholder expansion |
| net.kyori:adventure-text-minimessage | 4.17.0 | MiniMessage templates for celebration broadcast (bundled with Paper) |

## 4. Pinned external schemas

| Schema | Source of truth | Snapshot location |
|---|---|---|
| Starlight Skins API (face render) | https://starlightskins.lunareclipse.studio | `docs/refs/starlight-skins.md` (to add when first consumed) |
| PlaceholderAPI placeholder protocol | https://wiki.placeholderapi.com | — (read-only consumer) |

## 5. AI / agent rules

1. **Verify, don't guess.** Before writing code, confirm library APIs via context7 MCP, library source on disk (sibling Lumalyte plugins under `D:\BadgersMC-Dev\`), or `Read`/`Grep`. Record consulted sources in the task's `Evidence:` block.
2. **Use context7 MCP** for up-to-date library docs; prefer it over re-reading large source trees.
3. **Reference LumaSG** (`D:\BadgersMC-Dev\LumaSG\src\main\kotlin\net\lumalyte\lumasg\game\Celebration.kt`) for the winner-celebration pixel-art renderer pattern.
4. **Reference LumaGuilds** for the IF (InventoryFramework) menu pattern, ACF-less command wiring, and Koin-free Nexus DI examples.
5. **Briefing contract.** Any subagent dispatch carries: file paths, pre-verified signatures, the failing test (for TDD tasks), acceptance criteria, forbidden actions, and the task's `Evidence:` block.
6. **Task sizing.** If a worker briefing exceeds ~1500 tokens, `/spear:spec` decomposes the task further before dispatch.

## 6. Versioning

Semantic versioning. Project starts at `0.1.0`; bump major on breaking schema or command-API change.

## 7. CI

GitHub Actions — single workflow at `.github/workflows/build.yml` (to add in INFRA-02).

1. Build / compile (`./gradlew build`)
2. Unit tests (`./gradlew test`)
3. Architecture tests (Konsist — runs as part of `test`)
4. Shadow jar (`./gradlew shadowJar`)

## 8. Out of stack

Explicit non-goals for the toolchain — frameworks, languages, or infrastructure this project will NOT adopt without a spec change.

- MariaDB / MySQL (SQLite is sufficient for the expected load; revisit only if multi-shard deployment is needed)
- Vault / EnthusiaCurrency entry cost (free entry only this iteration)
- InvUI (project uses IF — InventoryFramework — to match LumaGuilds convention)
- Discord webhook announcements (out of scope for v0.1; consider for later versions)
