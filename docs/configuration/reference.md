---
title: Full Configuration Reference
parent: Configuration
nav_order: 5
---

# Full Configuration Reference

This page documents all shipped configuration keys with a description and a concrete example.

## `config.yml`

### `factions.*`

| Key | Description | Example |
|---|---|---|
| `factions.max-members` | Maximum members per faction. | `max-members: 50` |
| `factions.max-warps` | Maximum warps allowed per faction. | `max-warps: 10` |
| `factions.max-allies` | Maximum ally relations per faction. | `max-allies: 5` |
| `factions.max-truces` | Maximum truce relations per faction. | `max-truces: 5` |
| `factions.invites.ttl-hours` | Invite expiration window (hours). | `invites: { ttl-hours: 72 }` |

### `factions.power.*`

| Key | Description | Example |
|---|---|---|
| `factions.power.per-player-max` | Maximum power a player can hold. | `per-player-max: 10.0` |
| `factions.power.regen-per-second` | Base power regen speed used by regen defaults. | `regen-per-second: 0.1` |
| `factions.power.regen-online` | Power gained each power tick while online. | `regen-online: 6.0` |
| `factions.power.regen-offline` | Power gained each power tick while offline. | `regen-offline: 3.0` |
| `factions.power.loss-on-death` | Power removed on death in relevant contexts. | `loss-on-death: 4.0` |
| `factions.power.grace-period-seconds` | Startup grace window before power-loss logic. | `grace-period-seconds: 3600` |
| `factions.power.tick-interval-seconds` | Interval for periodic power engine updates. | `tick-interval-seconds: 60` |
| `factions.power.gain-on-kill.enabled` | Whether killing an enemy player grants power to the killer. | `enabled: true` |
| `factions.power.gain-on-kill.amount` | Power awarded to the killer per player kill. | `amount: 2.0` |
| `factions.power.buy.enabled` | Opt-in: allow players to purchase personal power via `/f power buy`. Requires Vault. | `enabled: false` |
| `factions.power.buy.cost-per-point` | Money charged per 1 unit of power purchased. | `cost-per-point: 100.0` |
| `factions.power.buy.max-per-purchase` | Maximum power a player can buy in a single command. | `max-per-purchase: 5.0` |

### `factions.land.*`

| Key | Description | Example |
|---|---|---|
| `factions.land.buffer-zone` | Enemy territory buffer in chunks (`0` disables). | `buffer-zone: 0` |
| `factions.land.max-per-command` | Cap for multi-claim/unclaim modes (`fill/square/circle`). | `max-per-command: 200` |
| `factions.land.per-power` | Land-per-power ratio for claim capacity. | `per-power: 1.0` |
| `factions.land.max` | Absolute hard cap on faction land claims. | `max: 500` |

### `factions.map.*`

| Key | Description | Example |
|---|---|---|
| `factions.map.once-radius` | Radius used by `/f map once`. | `once-radius: 2` |

### `factions.list.*` and `factions.top.*`

| Key | Description | Example |
|---|---|---|
| `factions.list.page-size` | Entries per page for `/f list`. | `list: { page-size: 8 }` |
| `factions.top.page-size` | Entries per page for `/f top`. | `top: { page-size: 8 }` |

### `factions.economy.*`

| Key | Description | Example |
|---|---|---|
| `factions.economy.enabled` | Master toggle for faction economy logic. | `enabled: true` |
| `factions.economy.cost-create` | Cost to create a faction. | `cost-create: 50.0` |
| `factions.economy.cost-claim` | Cost per claimed chunk. | `cost-claim: 100.0` |
| `factions.economy.tax.enabled` | Enables periodic tax processing. | `tax: { enabled: false }` |
| `factions.economy.tax.rate` | Fraction charged each tax cycle. | `tax: { rate: 0.05 }` |
| `factions.economy.tax.interval-hours` | Hours between tax cycles. | `tax: { interval-hours: 24 }` |
| `factions.economy.tax.min-bank-balance` | Skip tax below this bank balance. | `tax: { min-bank-balance: 0.0 }` |
| `factions.economy.tax.min-charge-amount` | Skip tiny deductions below this amount. | `tax: { min-charge-amount: 0.01 }` |
| `factions.economy.tax.notify-members` | Notify online members when tax is charged. | `tax: { notify-members: true }` |
| `factions.economy.bank.history.page-size` | Entries per page for `/f bank history`. | `bank: { history: { page-size: 8 } }` |

### `factions.warp.*`

| Key | Description | Example |
|---|---|---|
| `factions.warp.list.page-size` | Entries per page for `/f warp list`. | `warp: { list: { page-size: 8 } }` |

### `factions.fly.*`

| Key | Description | Example |
|---|---|---|
| `factions.fly.enabled` | Master toggle for faction fly feature. | `enabled: true` |
| `factions.fly.disable-on-threat` | Disable fly when threat logic triggers. | `disable-on-threat: true` |
| `factions.fly.require-own-territory` | Restrict fly to own territory. | `require-own-territory: true` |

### `factions.chat.*`

| Key | Description | Example |
|---|---|---|
| `factions.chat.show-tag` | Show faction tag in chat formatting. | `show-tag: true` |
| `factions.chat.tag-format` | MiniMessage format for faction chat tag. | `tag-format: "<gray>[<gold>{faction_name}</gold>]</gray> "` |

### `factions.metrics.*`

| Key | Description | Example |
|---|---|---|
| `factions.metrics.bstats.enabled` | Enable anonymous bStats metrics collection. | `bstats: { enabled: true }` |

### `factions.updates.*`

| Key | Description | Example |
|---|---|---|
| `factions.updates.enabled` | Check for new plugin versions on startup (opt-in, disabled by default). | `enabled: false` |
| `factions.updates.notify-ops-on-join` | Notify online operators about available updates when they join. | `notify-ops-on-join: false` |

### `integrations.*`

| Key | Description | Example |
|---|---|---|
| `integrations.vault` | Enable Vault integration attempt. | `vault: true` |
| `integrations.worldguard` | Enable WorldGuard integration attempt. | `worldguard: true` |
| `integrations.dynmap` | Enable dynmap integration attempt. | `dynmap: true` |
| `integrations.placeholderapi` | Enable PlaceholderAPI integration attempt. | `placeholderapi: true` |
| `integrations.essentialsx.enabled` | Enable EssentialsX teleport routing for `/f home` and `/f warp`. Jailed players are blocked. Requires EssentialsX 2.19+. | `essentialsx: { enabled: false }` |
| `integrations.lwc.enabled` | Master toggle for LWC/LWCX interop. | `lwc: { enabled: true }` |
| `integrations.lwc.require-build-rights-to-create` | Block protection creation if builder lacks rights. | `lwc: { require-build-rights-to-create: true }` |
| `integrations.lwc.remove-if-no-build-rights` | Remove stale protections when owner loses rights. | `lwc: { remove-if-no-build-rights: true }` |
| `integrations.lwc.remove-on-claim-change` | Remove alien protections after claim changes. | `lwc: { remove-on-claim-change: true }` |

---

## `database.yml`

| Key | Description | Example |
|---|---|---|
| `type` | Backend type (`h2` or `mysql`). | `type: h2` |
| `h2.file` | Relative file path for embedded H2 database. | `h2: { file: data/factions }` |
| `mysql.host` | MySQL/MariaDB host. | `mysql: { host: localhost }` |
| `mysql.port` | MySQL/MariaDB port. | `mysql: { port: 3306 }` |
| `mysql.database` | Database/schema name. | `mysql: { database: factions }` |
| `mysql.username` | Database username. | `mysql: { username: root }` |
| `mysql.password` | Database password. | `mysql: { password: "" }` |
| `mysql.pool-size` | Hikari pool size for mysql backend. | `mysql: { pool-size: 10 }` |
| `debug.jaloquent-logging` | Enable SQL logging for debugging. | `debug: { jaloquent-logging: false }` |

---

## `gui.yml`

### Root keys

| Key | Description | Example |
|---|---|---|
| `gui.enabled` | Master toggle for GUI system. | `enabled: true` |
| `gui.default-menu` | Menu id opened by `/f` (no args). | `default-menu: main` |
| `gui.menus` | Menu map container. | `menus: { main: ... }` |

### Menu-level keys (`gui.menus.<id>.*`)

| Key | Description | Example |
|---|---|---|
| `title` | Inventory title (MiniMessage). | `title: "<gold><bold>Factions</bold></gold>"` |
| `size` | Inventory slot size (`9..54`, rounded by implementation). | `size: 54` |
| `items` | Item definitions map. | `items: { info: ... }` |

### Item keys (`gui.menus.<id>.items.<itemId>.*`)

| Key | Description | Example |
|---|---|---|
| `slot` | Inventory slot index. | `slot: 10` |
| `material` | Bukkit material name. | `material: BOOK` |
| `name` | Display name (MiniMessage + placeholders). | `name: "<gold>Your Faction</gold>"` |
| `lore` | Lore lines list. | `lore: ["<gray>Faction: <white>{faction}</white>"]` |
| `glow` | Enables glint visual. | `glow: true` |
| `action` | Click action type (`RUN_COMMAND`, `SUGGEST_COMMAND`, `OPEN_MENU`, `REFRESH`, `CLOSE`). | `action: RUN_COMMAND` |
| `command` | Command used by `RUN_COMMAND` / `SUGGEST_COMMAND`. | `command: "/f info"` |
| `menu` | Target menu id for `OPEN_MENU`. | `menu: "main"` |

---

## `messages.yml`

`messages.yml` is fully key-based localization config.

### How to treat message keys

- Every key path is a translatable text entry.
- Values support MiniMessage formatting.
- Placeholders like `{player}`, `{faction}`, `{amount}`, `{usage}` must be preserved.

### Current top-level groups

| Group | Purpose | Example key |
|---|---|---|
| `prefix` | Global plugin prefix line | `prefix` |
| `general` | Generic errors/system text | `general.no-permission` |
| `help` | Help screen copy | `help.title` |
| `faction` | Faction lifecycle messages | `faction.created` |
| `member` | Member/rank flow messages | `member.kicked` |
| `invite` | Invite lifecycle and prompts | `invite.summary` |
| `claim` | Claim/unclaim and territory text | `claim.claimed` |
| `bank` | Bank operations and tax notices | `bank.balance` |
| `home` | Home set/teleport messages | `home.set`, `home.teleported`, `home.jailed` |
| `warp` | Warp flow messages | `warp.set`, `warp.teleported`, `warp.jailed` |
| `relation` | Relation messages | `relation.set` |
| `power` | Power model messages | `power.too-low-raid`, `power.lost-on-death`, `power.kill-gained`, `power.buy-success` |
| `fly` | Fly state messages | `fly.enabled` |
| `lwc` | LWC integration notices | `lwc.stale-protection-removed` |
| `info` | Faction info formatting | `info.header` |
| `admin` | Admin-facing messages | `admin.reload` |

### Message key example

```yaml
invite:
  summary: "<gold>You have <white>{count}</white> pending faction invite(s):"
```

Description:

- `invite.summary` is sent during invite listing and login summary notifications.
