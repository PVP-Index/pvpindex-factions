# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project follows Semantic Versioning where practical.

## [Unreleased]

### Added

### Changed

## [1.0.5] - 2026-05-16

### Added

- Folia support: scheduler abstraction layer (`CancelableTask`, `TaskScheduler`, `PlatformDetector`,
  `BukkitTaskScheduler`, `FoliaTaskScheduler`) routes all scheduled work through the correct
  Bukkit or Folia scheduler at runtime.
- `plugin.yml` now declares `folia-supported: true`.
- Dual chat-format listener: `EngineChat` detects Paper or Spigot at startup and registers
  `PaperChatListener` (`AsyncChatEvent` + adventure renderer) on Paper and `LegacyChatListener`
  (`AsyncPlayerChatEvent` + `setFormat`) on Spigot.
- GitHub Actions `server-startup` workflow: spins up Paper, Folia, and Spigot servers in a
  matrix on every pull request and validates that the plugin boots successfully. Spigot is
  compiled from source via BuildTools (cached after the first run).
- `SchedulerSmokeTest` (12 tests) covering `BukkitTaskScheduler` and `FoliaTaskScheduler` paths.
- `EngineChatListenerTest` (6 tests) covering both the Paper and Spigot chat-listener paths.
- `TeamsApiRegistrar` interface: isolates the TeamsAPI lifecycle contract from bootstrap code,
  carrying no TeamsAPI imports so it is safe to reference unconditionally.
- `TeamsApiRegistrarImpl`: concrete registrar that holds every `FactionsTeams*` adapter and
  every `TeamsAPI.register*()`/`TeamsAPI.unregister*()` call — loaded via reflection only.
- `ServicesBootstrapComponentTest` (5 tests): verifies standalone startup when TeamsAPI is
  absent, confirms internal services are always wired, and covers `stop()` lifecycle paths.

### Fixed

- **TeamsAPI startup crash**: `NoClassDefFoundError` was thrown at server startup when TeamsAPI
  was not installed, because `ServicesBootstrapComponent` imported `FactionsTeams*` adapter
  classes whose interfaces live in the TeamsAPI JAR. The fix moves all TeamsAPI references
  into a new `TeamsApiRegistrarImpl` class that is loaded exclusively via `Class.forName()`
  only after TeamsAPI has been confirmed present on the server.

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
