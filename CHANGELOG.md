# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project follows Semantic Versioning where practical.

## [Unreleased]

### Added

### Changed

## [1.0.7] - 2026-05-19

### Added

- Startup update-check integration using [ez-plugins/mc-plugin-update-notifier](https://github.com/ez-plugins/mc-plugin-update-notifier) with chained sources:
  Modrinth public API as primary and GitHub public API as fallback.
  Modrinth queries loaders `paper`, `folia`, and `spigot` so the check works on all supported
  server software.
- Operator join notification when an update is available, including clickable release URL output.
- New update config keys:
  - `factions.updates.enabled` (default `false` — opt-in)
  - `factions.updates.notify-ops-on-join` (default `false`)
- New message keys:
  - `update.available`
  - `update.url`
- **DiscordSRV integration** (`integrations.discordsrv.enabled: true`):
  - Faction create, disband, ally, truce, and enemy-declared events are broadcast to a
    Discord channel via DiscordSRV (pure reflection — no hard compile-time dependency).
  - Per-event toggles and Discord-markdown message templates are configurable in `config.yml`
    under `integrations.discordsrv.events.*`.
  - `channel-id` key routes messages to a specific text channel; leave empty to use DiscordSRV's
    main linked channel.
  - New config keys:
    - `integrations.discordsrv.enabled` (default `false`)
    - `integrations.discordsrv.channel-id` (default `""`)
    - `integrations.discordsrv.events.faction-created.enabled` / `.message`
    - `integrations.discordsrv.events.faction-disbanded.enabled` / `.message`
    - `integrations.discordsrv.events.relation-ally.enabled` / `.message`
    - `integrations.discordsrv.events.relation-truce.enabled` / `.message`
    - `integrations.discordsrv.events.relation-enemy.enabled` / `.message`
- **EssentialsX integration overhaul** (`integrations.essentialsx.enabled: true`):
  - `/f warp` teleports now route through EssentialsX alongside `/f home`.
  - EssentialsX `/back` location is recorded before every teleport so players can return with `/back`.
  - Jailed players are blocked from `/f home` and `/f warp` with a message.
  - Detected EssentialsX version is logged at startup.
  - New `messages.yml` keys: `home.teleported`, `home.teleport-failed`, `home.jailed`,
    `warp.teleported`, `warp.teleport-failed`, `warp.jailed`.
- **Safe zones and war zones** (`factions.zones.safe-zone.enabled`, `factions.zones.war-zone.enabled`):
  - Both zones are enabled by default. Disabling a zone causes its chunks to behave as
    Wilderness — protection, PvP rules, and power-loss suppression are all inactive.
  - New admin commands `/fa safezone` and `/fa warzone` let operators assign chunks to each
    zone in one-shot, square, or circle modes (with an optional `remove` sub-mode).
  - New permissions: `factions.cmd.safezone`, `factions.cmd.warzone` (default `op`).
- **Overclaiming** (`factions.overclaiming.enabled: false` — opt-in):
  - When enabled, a faction can claim an enemy's chunk if the victim's land count exceeds
    their current maximum land (power-based). Border adjacency is waived for overclaims.
  - Optional guard `factions.overclaiming.require-enemy-relation: true` restricts overclaims
    to factions that have declared `ENEMY` relation.
  - Attacker receives a `claim.overclaimed` notification; all online victim members receive
    `claim.overclaimed-victim` showing remaining chunk count.
  - `FactionChunkClaimEvent` gains an optional `overclaimedFromFaction` field populated on
    overclaim so third-party plugins can observe the event.
  - New `messages.yml` keys: `claim.overclaimed`, `claim.overclaimed-victim`,
    `claim.enemy-not-raidable`.
- **Power and war improvements** — six optional, independently toggleable features that
  improve PvP/raiding pacing. All are disabled by default:
  - **F1 — Inactive member exclusion** (`factions.power.inactive-exclusion.enabled: false`):
    members who have been offline longer than `inactive-exclusion.days` do not contribute to
    the faction's max-land calculation. Their stored power is unchanged; only the land-cap
    computation skips them, discouraging dead factions from holding territory indefinitely.
  - **F2 — Death streak multiplier** (`factions.power.death-streak.enabled: false`):
    consecutive deaths within a rolling `window-seconds` window each multiply the power loss
    by `multiplier^streak`. After the window expires the streak resets. A separate
    `power.death-streak-penalty` message is sent on streak deaths.
    Persisted in two new `PlayerModel` columns: `last_death_at`, `death_streak`.
  - **F3 — Scaled kill rewards** (`factions.power.gain-on-kill.scale.enabled: false`):
    kill power gain is multiplied by `clamp(victimPower / killerPower, min-factor, max-factor)`.
    Rewarding high-risk kills and reducing farming incentive.
  - **F4 — Raidable state broadcast** (`factions.raidable.broadcast.enabled: true`):
    after each power tick, the engine detects factions that cross the raidable threshold and
    notifies members. Optional `server-wide: true` broadcasts to all online players.
    The raidable flag is persisted in a new `FactionModel` column: `is_raidable`.
  - **F5 — Offline protection** (`factions.overclaiming.offline-protection.enabled: false`):
    blocks overclaiming when all members of the defending faction are currently offline,
    preventing pure offline raiding.
  - **F6 — War shield** (`factions.war.shield.enabled: false`):
    a faction may be assigned a daily UTC protection window by an admin using the new
    `/fa shield <faction> <start-hour> <duration-hours>` command. During the window the
    faction's land cannot be overclaimed. Persisted in two new `FactionModel` columns:
    `shield_start_hour`, `shield_duration_hours`.
  - New admin command: `/fa shield <faction> <clear|<start-hour (0-23)> <duration-hours>>`.
  - New permission: `factions.cmd.shield` (default `op`, child of `factions.admin`).
  - New `config.yml` keys:
    - `factions.power.inactive-exclusion.enabled` / `.days`
    - `factions.power.death-streak.enabled` / `.window-seconds` / `.multiplier`
    - `factions.power.gain-on-kill.scale.enabled` / `.min-factor` / `.max-factor`
    - `factions.raidable.broadcast.enabled` / `.server-wide`
    - `factions.overclaiming.offline-protection.enabled`
    - `factions.war.shield.enabled` / `.max-duration-hours`
  - New `messages.yml` keys: `power.death-streak-penalty`, `raidable.*`, `shield.*`,
    `claim.enemy-offline-protected`, `claim.shield-active`.
- **`/f powerhistory [<player>] [<page>]`** (`alias: phist`):
  shows a paginated log of significant power changes (death, kill, and purchase events).
  Players can view their own history without any extra permission.
  Viewing another player's history requires `factions.cmd.power.history.other` (default `op`).
  All message templates are configurable in `messages.yml` under `power.history-*`.
- **TeamsAPI 1.8.0 power history provider**: when TeamsAPI 1.8.0+ is present the plugin
  registers a `TeamsPowerHistoryService` implementation that exposes all recorded power-change
  events through the standard TeamsAPI surface.
  Consumer plugins can read player and team history, filter by time range, add external
  entries, and remove or clear records without touching the database directly.
  The provider is registered reflectively so servers running TeamsAPI 1.7.x or earlier
  start cleanly and the provider is silently skipped.
  New permissions: `factions.cmd.power.history` (default `true`),
  `factions.cmd.power.history.other` (default `op`).

### Changed

- **TeamsAPI dependency updated to 1.8.0** (was 1.7.0).
- Update notifier library is now explicitly relocated in shading:
  `com.github.ezplugins.updater` -> `com.pvpindex.lib.updater`, preventing runtime classpath conflicts.
- Update check is **disabled by default** (`factions.updates.enabled: false`). Opt in by setting it
  to `true`.
- `factions.updates.modrinth-slug`, `factions.updates.github-owner`, and
  `factions.updates.github-repo` are no longer exposed as config keys; the values are hardcoded
  in the plugin and the config entries are ignored if present.
- EssentialsX interop now uses the compile-time EssentialsX API instead of reflection, making
  it resilient to future EssentialsX API changes and easier to diagnose when something breaks.
- Plugin validates that the plugin named `Essentials` actually implements `IEssentials` before
  enabling the integration; logs a warning and falls back to noop if not.

## [1.0.6] - 2026-05-18

### Added

- **TeamsAPI 1.6 relation provider**: the plugin now implements the `TeamsRelationService`
  interface introduced in TeamsAPI 1.6. External plugins and scripts can read and write
  inter-faction relations (`ALLY`, `TRUCE`, `NEUTRAL`, `ENEMY`) through the standard TeamsAPI
  surface. `TeamRelationChangeEvent` is fired on every relation change, allowing third-party
  plugins to observe or cancel relation updates before they are persisted.

### Changed

- **TeamsAPI dependency updated to 1.6.1** (was 1.5.0).

### Fixed

- **Plugin crashed on startup when TeamsAPI 1.5.x was installed** (`NoClassDefFoundError:
  TeamsRelationService`): the `TeamsRelationService` interface was introduced in TeamsAPI 1.6
  and did not exist in older installations. A direct bytecode reference to it in the registrar
  caused the JVM bytecode verifier to fail when loading the class, crashing the plugin on
  startup. All references to `TeamsRelationService` and its concrete adapter are now loaded via
  reflection so that TeamsAPI 1.5.x servers start cleanly and the relation provider is silently
  skipped when TeamsAPI < 1.6 is detected.

- **Stale relation entries after faction disband**: when a faction was disbanded, references
  to it stored in other factions' relation maps were not removed, leaving orphaned entries in
  the database. Disbanding a faction now clears all incoming relation references across every
  other faction.

## [1.0.5] - 2026-05-16

### Added

- **Folia support**: the plugin now runs on Folia servers alongside Bukkit, Spigot, and Paper.
  All scheduled tasks use a new scheduler abstraction that automatically picks the correct
  Bukkit or Folia scheduler at runtime. `plugin.yml` now declares `folia-supported: true`.
- **Correct chat formatting on Spigot**: chat-format events are now handled through the right
  API on each platform — `AsyncChatEvent` on Paper and `AsyncPlayerChatEvent` on Spigot.

### Fixed

- **Plugin did not start when TeamsAPI or EzCountdown were absent**:
  Servers without TeamsAPI or EzCountdown installed logged `NoClassDefFoundError` at startup
  and the plugin failed to load entirely. Both integrations are now fully isolated — the plugin
  always starts cleanly and activates each integration only when its plugin is present.

- **Plugin did not start on Spigot** (`NoClassDefFoundError` at startup):
  Several internal classes referenced Adventure API types that are absent or incompatible on
  Spigot 1.21.4 and 1.21.11, causing the JVM to crash during class initialisation before any
  commands or events could be registered. All Adventure-typed state has been moved into
  lazily-loaded inner classes that Spigot never touches at startup.

- **Messages showed no colour or formatting on Spigot**:
  Plugin messages appeared as raw plain text because Spigot does not bundle Adventure at runtime.
  The plugin now ships its own copy of Adventure (MiniMessage + legacy serialiser) so colours,
  bold, italic, and other formatting always render correctly on Spigot.

- **All commands crashed on Spigot** (`/f info`, `/f create`, and every other command):
  Every command thrown on Spigot produced `UnsupportedOperationException: No JsonComponentSerializer
  implementation found`, making the plugin completely unusable. The GSON serialiser is now bundled
  in the plugin JAR, and the initialisation sequence explicitly uses the plugin classloader so that
  Java's `ServiceLoader` locates the implementation at runtime rather than falling back to a stub
  that throws.

- **Hover tooltips did nothing on Spigot** (`/f info` member list, `/f map` territory tiles):
  After the command crash was resolved, hover and click events were still silently dropped because
  the Spigot message path was sending plain §-coded strings. Messages to players are now serialised
  to JSON via the bundled GSON serialiser and delivered through the BungeeCord chat API, so hover
  and click events work correctly on Spigot.

- **Intermittent shutdown error from bStats metrics** (rare):
  On servers that shut down shortly after startup, bStats background tasks occasionally ran after
  the database connection was already closed, logging `HikariDataSource has been closed`. A
  shutdown flag now prevents metrics tasks from querying the database once the plugin has stopped.


## [1.0.4] - 2026-05-16

### Added

- [TeamsAPI](https://modrinth.com/plugin/teams-api) 1.5.0 subcommand provider support: `/f <name>` now dispatches to any
  `TeamsSubcommand` registered via `TeamsAPI.registerSubcommand()`, with permission
  checks and usage-hint fallback. Tab-completion also surfaces registered subcommand
  names and forwards argument-level completions to each subcommand's own `tabComplete()`.

### Changed

- Upgraded [TeamsAPI](https://modrinth.com/plugin/teams-api) dependency from `1.4.0` to `1.5.0`.
- PvPIndex Factions now declares `loadbefore` for [EzShops](https://modrinth.com/plugin/ezshops),
  [EzAuction](https://modrinth.com/plugin/ezauction), [EzRTP](https://modrinth.com/plugin/ezplugins-ezrtp), and
  [EzClean](https://modrinth.com/plugin/ezclean) so those plugins always load after the faction provider is available.

## [1.0.3] - 2026-05-12

### Added

- New `notifications.yml` config file consolidating all notification settings (inbox, member join, economy tax, EzCountdown).
- EzCountdown relation announcements now fall back to a server-wide chat broadcast when EzCountdown is absent or disabled in config.

### Changed

- EzCountdown integration is now fully optional: relation announcements are delivered via chat by default and only use EzCountdown when the plugin is present and `notifications.ezcountdown.enabled` is `true`.
- Notification settings moved from `config.yml` to `notifications.yml`: `notify-members` (economy tax) and all `integrations.ezcountdown.*` keys.

## [1.0.2] - 2026-05-11

### Added

- `/f map` now supports a size option: `--size=<size>`.
- Shared command argument parsing coverage for valued long options (`--key=value` and `--key value`), including error handling tests.
- New reusable faction member notification helper (`FactionMemberNotifier`) for consistent online-member notifications across features.
- New predefined factions system with separate `pre-defined.yml` storage and explicit in-memory reload flow.
- New `/f predefined` command tree (with `/f prefined` alias): `create`, `claim`, `sethome`, `reload`, and `list`.
- New predefined defaults and policy toggles in `pre-defined.yml`: `enabled` (default `false`), `case-sensitive`, and `block-disband`.
- `/f info` can now display relationship sections, configurable via:
  - `factions.info.relations.show-allies` (default `true`)
  - `factions.info.relations.show-truces` (default `false`)
  - `factions.info.relations.show-neutrals` (default `false`)
  - `factions.info.relations.show-enemies` (default `false`)

### Changed

- `/f map once` default render radius increased to `3` via config default (`factions.map.once-radius`).
- `/f map` row suffix formatting no longer appends `z=<value>` at line ends.
- `/f map` tile hover text now uses explicit coordinate labels for clarity: `Chunk X`, `Chunk Z`, and `Player Y`.
- `/f create <name>` now enforces a predefined-name whitelist when predefined mode is enabled.
- Improved relation workflow and UX:
  - `/f relation` now supports `relationship` alias.
  - Ally/truce now support pending-to-mutual confirmation flow.
  - Enemy/neutral relations are mirrored for consistency.
  - Ally/truce limits are enforced via relation limits.
- Tab completion improvements:
  - Fixed `/f relation <faction> <relation>` completion.
  - Fixed `/f relationship ...` completion.
  - Improved `/f map` completion for mixed argument order (`on|off|once` with `--size`).
- Join and relation notifications now use configurable message keys in `messages.yml`.
- Added predefined command permissions and configurable predefined messages in `messages.yml`.

### Fixed

- Fixed invite acceptance join path that could fail with database NOT NULL constraint errors when player rows were first created.
- Faction members are now notified when a player successfully joins their faction.
- Predefined factions can be protected from disbanding (player and admin disband paths) when policy is enabled.

## [1.0.1] - 2026-05-10

### Added

- `/f power buy <amount>` subcommand: opt-in feature (disabled by default) allowing players to purchase personal power with money via Vault. Configurable cost per point and per-purchase cap (`factions.power.buy.*`).
- New `config.yml` keys: `factions.power.gain-on-kill.enabled`, `factions.power.gain-on-kill.amount`, `factions.power.buy.enabled`, `factions.power.buy.cost-per-point`, `factions.power.buy.max-per-purchase`.
- New `messages.yml` keys: `power.lost-on-death`, `power.kill-gained`, `power.buy-success`, `power.buy-disabled`, `power.buy-no-vault`, `power.buy-invalid-amount`, `power.buy-already-max`, `power.buy-insufficient-funds`.

### Changed

**Teams API updated**

The TeamsAPI integration now has access to faction claims and faction power, making it possible to connect other plugins to it and making a more unique Factions experience per server.

- TeamsAPI `ClaimService` adapter: claim and unclaim individual chunks, bulk-unclaim by team, query claims by chunk or team, check whether a chunk is claimed or owned by a specific team, and compute max claim allowance from total power.
- TeamsAPI `PowerService` adapter: get and set individual player power (clamped to configured max), query total and max power for a team.
- `FactionTeamClaim` value type bridging internal `BoardEntry` data to the TeamsAPI `TeamClaim` contract.
- Updated [TeamsAPI](https://modrinth.com/plugin/teams-api) dependency from `1.3.0` to `1.4.0`.

**Power engine improved**

To create a gameplay flow around power we have decided to make it kill based by default with an opt-in power buy feature (`/f power buy <amount>`).

- Power loss on death: players lose configurable power (`factions.power.loss-on-death`, default 4.0) when killed; respects the server-start grace period and skips safezone territory.
- Power gain on kill: killing an enemy player grants the killer configurable power (`factions.power.gain-on-kill.amount`, default 2.0); opt-out via `factions.power.gain-on-kill.enabled`.

### Fixed

- Plugin crashed on startup with `NoClassDefFoundError` for TeamsAPI classes when TeamsAPI was absent or not yet loaded. Removed `load: STARTUP` so the plugin loads in the default `POSTWORLD` phase (respecting `softdepend` order), and changed `BootstrapContext` adapter fields to `Object` to prevent eager JVM resolution of TeamsAPI interface types.

## [1.0.0] - 2026-05-10

### Added

- Initial modern PvPIndex Factions release baseline.
- Release workflow with changelog-driven GitHub release notes and marketplace publish flow.

### Changed

- Documentation and listing improvements for server-owner onboarding and publishing.
