# EnthusiaGiveaway

Paper 1.21 plugin for scheduled giveaways with admin GUI, free entry, and celebratory winner announcements (pixel-art skin head in chat).

Follows the **SPEAR** development methodology — see `docs/`.

## Stack

- Kotlin 2.1 on JDK 21
- Paper 1.21.11 API
- Nexus (DI + coroutines bridge, BadgersMC)
- InventoryFramework 0.11.6 for GUI menus
- SQLite + HikariCP + Exposed for persistence
- PlaceholderAPI (optional) for command placeholder expansion

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
