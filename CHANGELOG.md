# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project follows Semantic Versioning where practical.

## [Unreleased]

### Added

### Changed

## [1.0.2] - 2026-05-11

### Added

- `/f map` now supports a size option: `--size=<size>`.
- Shared command argument parsing coverage for valued long options (`--key=value` and `--key value`), including error handling tests.
- New reusable faction member notification helper (`FactionMemberNotifier`) for consistent online-member notifications across features.
- `/f info` can now display relationship sections, configurable via:
  - `factions.info.relations.show-allies` (default `true`)
  - `factions.info.relations.show-truces` (default `false`)
  - `factions.info.relations.show-neutrals` (default `false`)
  - `factions.info.relations.show-enemies` (default `false`)

### Changed

- `/f map once` default render radius increased to `3` via config default (`factions.map.once-radius`).
- `/f map` row suffix formatting no longer appends `z=<value>` at line ends.
- `/f map` tile hover text now uses explicit coordinate labels for clarity: `Chunk X`, `Chunk Z`, and `Player Y`.
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

### Fixed

- Fixed invite acceptance join path that could fail with database NOT NULL constraint errors when player rows were first created.
- Faction members are now notified when a player successfully joins their faction.

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
