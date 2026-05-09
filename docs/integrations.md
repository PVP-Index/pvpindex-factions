---
title: Integrations
parent: Reference
nav_order: 3
---

# Integrations

Optional integrations are soft-dependencies and fail gracefully when absent.

## Quick links

- [Configuration Overview](configuration/index.md)
- [Core Settings](configuration/core-settings.md)
- [Operations Runbook](operations/admin-runbook.md)

## Supported integrations

- Vault
- WorldGuard / WorldEdit
- PlaceholderAPI
- EssentialsX
- dynmap
- LWC / LWCX
- TeamsAPI

Control integration behavior in `config.yml` under `integrations.*`.

## Vault

Use Vault to connect your economy plugin to faction bank and cost flows.

1. Install Vault and an economy provider.
2. Ensure both plugins load before PvPIndex Factions.
3. Enable economy-related keys in `config.yml`.
4. Restart and verify startup logs.

See also:
- [Bank and Economy](features/bank-and-economy.md)
- [Core Settings](configuration/core-settings.md)

## PlaceholderAPI

Use PlaceholderAPI for placeholders in chat/scoreboard/plugin ecosystems.

1. Install PlaceholderAPI.
2. Enable placeholder integration keys in `config.yml`.
3. Restart and validate placeholders render in your target plugin.

## WorldGuard and WorldEdit

Use these for territory policy compatibility and admin workflows where supported.

1. Install WorldGuard and WorldEdit.
2. Enable relevant integration toggles in `config.yml`.
3. Test claim behavior in protected regions.

## dynmap

Use dynmap integration for web map territory visualization.

1. Install dynmap.
2. Enable dynmap integration in `config.yml`.
3. Restart and validate faction overlays on web map.

## TeamsAPI

TeamsAPI integration is optional and should never block standalone plugin operation.

1. Install a compatible TeamsAPI version.
2. Enable TeamsAPI integration keys in `config.yml`.
3. Restart and verify adapter startup logs.
4. Test invite/member synchronization flows.

See also:
- [Invites and Notifications](features/invites-and-notifications.md)
- [Membership and Ranks](features/membership-and-ranks.md)

## EssentialsX and LWC/LWCX

Install these only if you need those specific ecosystem hooks on your server.

1. Install target plugin(s).
2. Enable matching integration toggles in `config.yml`.
3. Restart and run your feature-level smoke tests.
