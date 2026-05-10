PvPIndex Factions is a long-term modernization of the classic Factions experience for modern Paper servers.

The goal is simple: revive Factions with a timeless architecture, reliable persistence, clean gameplay UX, and API-connectable systems that can integrate with modern server ecosystems for years to come.

If you love classic Factions gameplay but need something maintainable, scalable, and integration-ready, PvPIndex Factions is built for exactly that.

## Supported versions

- Minecraft / Paper `1.21.*`
- Paper API line `26.1.*`
- Java `21+`

## Vision: timeless Factions, modern backend

PvPIndex Factions is not a quick port. It is an active refactor effort to make Factions:

- Easier to operate in production
- Easier to integrate with APIs and external systems
- Easier to maintain across future Minecraft/Paper updates
- More consistent for players and staff

This project keeps the recognizable Factions identity while replacing legacy internals with a modern service/repository model and optional integration adapters.

## What is modernized vs legacy Factions

Compared to older Factions-era plugins, PvPIndex Factions focuses on:

- Modern command ergonomics and guided help UX
- Persistent invite lifecycle with login notifications
- Better map readability and interaction flow
- More complete claim/unclaim modes
- Configurable GUI-driven navigation
- Optional tax/economy control for faction banks
- Cleaner permissions and admin workflows
- Soft-dependency integrations that fail gracefully
- Documentation and operational runbooks for real server owners

You keep the Factions gameplay loop, but gain a modern operational foundation.

## Why server owners choose PvPIndex Factions

- Modernized `/f` command experience with familiar muscle memory
- Persistent faction invites with login-time invite notifications
- Advanced land control: `claim`/`unclaim` modes (`one`, `square`, `circle`, `fill`, `auto`)
- Interactive `/f map` with clearer territory context
- Faction bank with deposit/withdraw/transfer/history and optional tax engine
- Configurable `/f` GUI via `gui.yml`
- Scales from standalone setup to larger integrated networks

## Feature overview

### Faction lifecycle

- Create, rename, describe, and disband factions
- Member administration with invite/revoke/accept/decline flows
- Leadership transfer and rank movement (promote/demote)
- Clear onboarding through `/f help`

### Land and map

- Claim/unclaim behavior designed for both casual and advanced use
- `square`, `circle`, and `fill` modes for territory management
- Auto claim/unclaim modes for movement-driven workflows
- Modernized `/f map` display and improved territorial context messaging

### Economy and bank

- Faction bank deposit, withdraw, transfer, and history
- Optional periodic tax engine with configurable rates/intervals
- Works with Vault-based economy stacks

### Homes and warps

- Faction home and warp management
- Warp listing/set/delete flows
- Compatibility-oriented command structure for player familiarity

### Notifications and QoL

- Invite visibility for online and returning players
- Territory and event notifications
- Player-level notification controls

## API and integration direction

PvPIndex’s long-term direction is an API-connectable Factions core.

That means systems are designed to be:

- Adapter-friendly
- Service-oriented
- Compatible with external plugins/services without hard coupling
- Reliable even when optional integrations are absent

This creates a stable base for future extensions, panels, tooling, and ecosystem plugins.

## Integrations

PvPIndex Factions runs standalone, and integrates when available:

- Vault (economy API)
- PlaceholderAPI
- TeamsAPI
- WorldGuard / WorldEdit
- dynmap
- EssentialsX
- LWC / LWCX

### Integration downloads

- PlaceholderAPI: https://modrinth.com/plugin/placeholderapi
- WorldEdit: https://modrinth.com/plugin/worldedit
- WorldGuard: https://modrinth.com/plugin/worldguard
- dynmap: https://modrinth.com/plugin/dynmap
- EssentialsX: https://modrinth.com/plugin/essentialsx
- LWC: https://modrinth.com/plugin/lwc
- Vault API: https://www.spigotmc.org/resources/vault.34315/ (official dependency page)
- TeamsAPI: https://modrinth.com/plugin/teams-api

## Configuration and operations

Generated config files:

- `config.yml`
- `database.yml`
- `messages.yml`
- `gui.yml`

Storage options:

- Embedded H2 for quick setup
- MySQL/MariaDB for larger networks

Docs include:

- Installation and setup guides
- Full config reference
- Feature-by-feature documentation
- Integrations setup notes
- Operations runbook and troubleshooting

## Visual preview

GUI:

![PvPIndex Factions GUI](https://i.ibb.co/Gfh4VcLv/image.png)

Map:

![PvPIndex Factions map](https://i.ibb.co/Jw4qzdqC/image.png)

Faction info:

![PvPIndex Factions info](https://i.ibb.co/Y469RX8j/image.png)

Top tab-complete:

![PvPIndex Factions top tabcomplete](https://i.ibb.co/5XY0cN0X/image.png)

## Commands at a glance

- Player root: `/f` (`/faction`, `/factions`)
- Admin root: `/fa` (`/factionadmin`)
- Highlights: `/f create`, `/f invite`, `/f join`, `/f claim`, `/f unclaim`, `/f map`, `/f info`, `/f top`, `/f bank`, `/f warp`

## Designed for migration-minded communities

If your server comes from legacy Factions communities, PvPIndex Factions is intentionally built to feel familiar while offering a cleaner and more future-proof base.

The objective is not to erase Factions identity; it is to preserve it and make it sustainable for modern server operations.

## Documentation

For setup, configuration, operations, and troubleshooting:

- Docs: https://pvp-index.github.io/pvpindex-factions
- Getting started: https://pvp-index.github.io/pvpindex-factions/getting-started
- Installation: https://pvp-index.github.io/pvpindex-factions/installation
- Step-by-step guides: https://pvp-index.github.io/pvpindex-factions/guides
- Core configuration: https://pvp-index.github.io/pvpindex-factions/configuration/core-settings
- Full configuration reference: https://pvp-index.github.io/pvpindex-factions/configuration/reference
- Features overview: https://pvp-index.github.io/pvpindex-factions/features
- Integrations docs: https://pvp-index.github.io/pvpindex-factions/integrations
- Operations runbook: https://pvp-index.github.io/pvpindex-factions/operations/admin-runbook
- Troubleshooting: https://pvp-index.github.io/pvpindex-factions/operations/troubleshooting
- Commands reference: https://pvp-index.github.io/pvpindex-factions/commands
- Permissions reference: https://pvp-index.github.io/pvpindex-factions/permissions
- Source: https://github.com/PVP-Index/pvpindex-factions
- Issues: https://github.com/PVP-Index/pvpindex-factions/issues

## License and attribution

- LGPL-3.0
- Derivative of MassiveCraft Factions (LGPL-3.0)
- Refactor by PvPIndex.com team (Shadow48402, Epildev)
