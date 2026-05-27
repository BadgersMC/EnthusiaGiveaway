# EnthusiaGiveaway

Paper 1.21 plugin for scheduled giveaways with admin GUI, free entry, and celebratory winner announcements (pixel-art skin head in chat).

Follows the **SPEAR** development methodology — see `docs/`.

## Stack

- Kotlin 2.1 on JDK 21
- Paper 1.21.11 API
- [Nexus](https://github.com/BadgersMC/Nexus) 1.11.0 — DI, coroutines, persistence, scheduler, paper-loader (shaded under `net.badgersmc.giveaway.libs.nexus.*`)
- InventoryFramework 0.11.6 for GUI menus
- SQLite + HikariCP + Exposed for persistence
- PlaceholderAPI (optional) for command placeholder expansion

## Building locally

Nexus 1.11.0 is published to [BadgersMC GitHub Packages](https://github.com/orgs/BadgersMC/packages?repo_name=Nexus). Add credentials once to `~/.gradle/gradle.properties`:

```properties
gpr.user=<your-github-username>
gpr.token=<personal-access-token-with-read:packages>
```

Then `./gradlew build` resolves every `net.badgersmc:nexus-*` artifact normally. To pick up in-progress Nexus changes from your local Maven cache, pass `-PuseMavenLocal=true` (after running `./gradlew -PuseMavenLocal=true publishToMavenLocal` in the Nexus repo).

CI uses the auto-provided `GITHUB_TOKEN` — no extra secrets required.

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
