# EnthusiaGiveaway

Paper 1.21 plugin for scheduled giveaways with admin GUI, free entry, and celebratory winner announcements (pixel-art skin head in chat).

Follows the **SPEAR** development methodology — see `docs/`.

## Stack

- Kotlin 2.1 on JDK 21
- Paper 1.21.11 API
- [Nexus](https://github.com/BadgersMC/Nexus) v2.1.1 — DI, coroutines, persistence, scheduler, paper-loader (shaded under `net.badgersmc.giveaway.libs.nexus.*`)
- InventoryFramework 0.11.6 for GUI menus
- SQLite + HikariCP + Exposed for persistence
- PlaceholderAPI (optional) for command placeholder expansion

## Building locally

Nexus v2.1.1 is served via [JitPack](https://jitpack.io) from the public [BadgersMC/Nexus](https://github.com/BadgersMC/Nexus) repo. Plain `./gradlew build` resolves every `com.github.BadgersMC.Nexus:nexus-*:v2.1.1` artifact — no token, no credentials, no `gradle.properties` setup.

To pick up in-progress Nexus changes from your local Maven cache, pass `-PuseMavenLocal=true` after running `./gradlew -PuseMavenLocal=true publishToMavenLocal` in the Nexus repo.

CI needs no extra secrets — JitPack is public.

## Commands

| Command | Permission | Purpose |
|---|---|---|
| `/giveaway` | `enthusiagiveaway.use` | Open the active-giveaways menu and enter |
| `/giveaway admin` | `enthusiagiveaway.admin` | Open the admin menu (schedule, cancel) |

## Docs

- `docs/tech-stack.md` — dependencies, versions, AI rules
- `docs/requirements.md` — EARS-formatted spec
- `docs/implementation.md` — architecture blueprint, layer rules
- `docs/tasks.md` — work breakdown
