# PvPIndex Factions

A modern factions plugin for Paper 1.21.4, backed by Jaloquent persistence and optional TeamsAPI integration.

> This project is a refactor of [MassiveCraft Factions](https://github.com/MassiveCraft/Factions), which is also licensed under LGPL-3.0.
> Both projects remain open-source under the same license terms.

---

## Documentation

Project documentation for server owners is available in [`/docs`](docs) and is
published through the GitHub Pages docs workflow.

---

## Requirements

| Requirement | Version |
|---|---|
| Paper | 1.21.4 |
| Java | 21+ |

## Optional Integrations

The plugin starts without any of these. Each integration is enabled only when the provider plugin is present and the corresponding toggle in `config.yml` is `true`.

| Plugin | Effect when present |
|---|---|
| Vault | Economy costs for faction creation and land claiming |
| WorldGuard / WorldEdit | Territory protection hooks |
| PlaceholderAPI | Faction placeholders for scoreboards and chat |
| TeamsAPI | Team synchronization adapter |
| EssentialsX | Route `/f home` through the Essentials teleport system |
| dynmap | Render faction territory on the dynmap web map |
| EzEconomy | Alternative economy provider |
| LWC / LWCX | Remove stale chest protections when chunk ownership changes |

---

## Building

```bash
# Compile and run tests
mvn test

# Build the shaded plugin jar (output: target/pvpindex-factions-*.jar)
mvn package

# Tests + Checkstyle
mvn verify
```

The project targets Java 21 bytecode (`-release 21`) and can be compiled with any JDK 21+.
Checkstyle enforces UTF-8 encoding, no star imports, no trailing whitespace, and lines ≤ 130 characters.

---

## Installation

1. Drop the shaded jar into your server's `plugins/` directory.
2. Start the server once to generate default configuration files under `plugins/PvPIndexFactions/`.
3. Edit `config.yml` and `database.yml` as needed, then restart.

---

## Configuration

### `database.yml`

| Key | Default | Description |
|---|---|---|
| `type` | `h2` | Database backend: `h2` (embedded) or `mysql` |
| `h2.file` | `data/factions` | H2 file path relative to the plugin data folder |
| `mysql.host` | `localhost` | MySQL/MariaDB hostname |
| `mysql.port` | `3306` | MySQL/MariaDB port |
| `mysql.database` | `factions` | Database name |
| `mysql.username` | `root` | Database user |
| `mysql.pool-size` | `10` | HikariCP connection pool size |
| `debug.jaloquent-logging` | `false` | Print every SQL query to the console |

### `config.yml` highlights

| Setting | Default | Description |
|---|---|---|
| `factions.max-members` | `50` | Maximum members per faction |
| `factions.max-warps` | `10` | Maximum warps per faction |
| `factions.max-allies` | `5` | Maximum allied factions |
| `factions.max-truces` | `5` | Maximum truce relationships |
| `factions.invites.ttl-hours` | `72` | Pending invite expiry time |
| `factions.power.per-player-max` | `10.0` | Max power each online player contributes |
| `factions.power.regen-per-second` | `0.1` | Power regeneration rate while online |
| `factions.power.loss-on-death` | `4.0` | Power lost on death in enemy/war territory |
| `factions.power.grace-period-seconds` | `3600` | Startup grace period before power loss applies |
| `factions.land.buffer-zone` | `0` | Minimum chunk buffer between enemy territories (0 = off) |
| `factions.land.max-per-command` | `200` | Max chunks claimable in one fill/circle/square command |
| `factions.economy.cost-create` | `50.0` | Vault cost to create a faction |
| `factions.economy.cost-claim` | `100.0` | Vault cost per claimed chunk |
| `factions.economy.tax.enabled` | `false` | Periodic tax on faction bank balance |
| `factions.economy.tax.rate` | `0.05` | Fraction deducted per tax interval |
| `factions.economy.tax.interval-hours` | `24` | Tax interval in hours |
| `factions.fly.enabled` | `true` | Allow faction flight in own territory |
| `factions.fly.disable-on-threat` | `true` | Cancel flight when an enemy enters the chunk |
| `factions.fly.require-own-territory` | `true` | Restrict flight to own faction's territory |
| `factions.chat.show-tag` | `true` | Prepend faction tag in global chat |

---

## Commands

All player commands use `/f` (aliases: `/faction`, `/factions`).  
Admin commands use `/fa` (aliases: `/factionadmin`) and require the `factions.admin` permission.

### Player commands (`/f`)

| Command | Description |
|---|---|
| `help` | List available commands |
| `create <name>` | Create a new faction |
| `disband` | Disband your faction |
| `join <faction>` | Join a faction you have been invited to |
| `leave` | Leave your current faction |
| `invite <player>` | Invite a player to your faction |
| `invite list` | List pending invitations |
| `invite accept <faction>` | Accept a pending invite |
| `invite decline <faction>` | Decline a pending invite |
| `invite decline-all` | Decline all pending invites |
| `invite revoke <player>` | Revoke a sent invitation |
| `kick <player>` | Kick a member from your faction |
| `promote <player>` | Promote a member one rank |
| `demote <player>` | Demote a member one rank |
| `leader <player>` | Transfer faction leadership |
| `info [faction]` | Show faction info |
| `list` | List all factions |
| `top` | Leaderboard ranked by power |
| `map` | ASCII territory map of the surrounding area |
| `claim [mode]` | Claim land for your faction |
| `unclaim` | Unclaim the current chunk |
| `sethome` | Set faction home to your current location |
| `home` | Teleport to faction home |
| `unsethome` | Remove the faction home |
| `fly` | Toggle faction flight in own territory |
| `desc <text>` | Set faction description |
| `rename <name>` | Rename your faction |
| `relation <faction> <ally\|truce\|enemy\|neutral>` | Set relation with another faction |
| `relation list` | List current relations |
| `relation wishes` | List pending relation requests |
| `warp <name>` | Teleport to a faction warp |
| `warp set <name>` | Create or update a faction warp |
| `warp delete <name>` | Delete a faction warp |
| `warp list` | List all faction warps |
| `bank` | Show faction bank balance |
| `bank deposit <amount>` | Deposit money into the faction bank |
| `bank withdraw <amount>` | Withdraw money from the faction bank |
| `bank transfer <faction> <amount>` | Transfer money to another faction's bank |
| `bank history` | View recent bank transactions |

### Admin commands (`/fa`)

| Command | Description |
|---|---|
| `help` | List admin commands |
| `reload` | Reload `config.yml` and `messages.yml` without restart |
| `bypass` | Toggle admin bypass mode (ignore faction build/interact permissions) |
| `claim <faction>` | Force-claim the current chunk for a faction |
| `unclaim` | Force-unclaim the current chunk |
| `disband <faction>` | Force-disband any faction |

---

## Permissions

| Node | Description |
|---|---|
| `factions.admin` | Access to all `/fa` admin commands |

All player-facing `/f` commands are available without an explicit permission node.

---

## Architecture

```
PvPIndexFactions          - Bukkit entry point; delegates to Bootstrap
Bootstrap                 - Ordered startup/shutdown of components
  InfrastructureBootstrapComponent  - DatabaseManager, Repositories
  ServicesBootstrapComponent        - FactionService, MemberService, ...
  EnginesBootstrapComponent         - Bukkit listeners, scheduled tasks
  CommandsBootstrapComponent        - /f and /fa command trees
  OptionalHooksBootstrapComponent   - Vault, TeamsAPI, PAPI, dynmap, LWC
```

Data access flows through `DatabaseManager` → `Repositories` → individual repository classes.
Domain logic lives in services; Bukkit listener/scheduling logic lives in engines.
Commands are thin adapters that validate input and delegate to services.

---

## License

This program is free software: you can redistribute it and/or modify it under the terms of the
**GNU Lesser General Public License version 3** (LGPL-3.0), as published by the Free Software Foundation.

See [licenses/LGPL.txt](licenses/LGPL.txt) and [licenses/LICENCE.txt](licenses/LICENCE.txt) for the full license text.

This project is a derivative work of [MassiveCraft Factions](https://github.com/MassiveCraft/Factions),
copyright MassiveCraft, also distributed under LGPL-3.0.

Refactor attribution: this modern PvPIndex refactor is maintained by the **PvPIndex.com team**
(Shadow48402, Epildev).

### Third-Party Components

The distributed plugin jar bundles the following libraries, relocated under `com.gyvex.pvpindex.lib`:

| Library | License |
|---|---|
| [Jaloquent](https://github.com/EzFramework/jaloquent) (EzFramework) | MIT |
| [H2 Database Engine](https://h2database.com) | EPL-2.0 / MPL-2.0 |
| [HikariCP](https://github.com/brettwooldridge/HikariCP) | Apache-2.0 |
| [MySQL Connector/J](https://dev.mysql.com/doc/connector-j/) | GPL-2.0 with FOSS Exception |
| [Gson](https://github.com/google/gson) | Apache-2.0 (see [licenses/gson-license.txt](licenses/gson-license.txt)) |

MySQL Connector/J is included under the [GPL-2.0 FOSS Exception](https://www.mysql.com/about/legal/licensing/foss-exception/),
which permits its use in LGPL-licensed open-source software.
