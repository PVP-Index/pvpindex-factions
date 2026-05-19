---
title: WorldGuard and WorldEdit
parent: Integrations
nav_order: 4
---

# WorldGuard and WorldEdit Integration

This integration provides territory guard behavior for protected-region compatibility and,
optionally, high-performance build protection through native WorldGuard region management.

## Config Keys

| Key | Default | Description |
|---|---|---|
| `integrations.worldguard` | `true` | Enable the WorldGuard integration. |
| `integrations.worldguard-sync-regions` | `false` | Mirror faction claims as WG regions for native build protection (see below). |

## Basic Setup

1. Install both WorldGuard and WorldEdit.
2. Set `integrations.worldguard: true`.
3. Restart and test claim/unclaim behavior in and around protected regions.

## Region Sync (Performance Mode)

When `integrations.worldguard-sync-regions: true` is set, PvPIndex Factions mirrors every
claimed chunk as a `ProtectedCuboidRegion` inside WorldGuard. This allows WG to handle
block-break and block-place protection natively, which eliminates database queries on
most block events.

**How it works:**

- WG evaluates events at `NORMAL` priority. Faction members are added to their region as
  WG domain members so WG passes their actions through.
- The protection engine runs at `HIGH` with `ignoreCancelled = true`. For enemy players,
  WG has already cancelled the event before the engine sees it -- no DB query is needed.
- Allies are not added to WG regions. A secondary `HIGHEST ignoreCancelled = false` handler
  un-cancels their events after a lightweight DB check.
- Safezone and warzone chunks get WG regions with no members, so WG denies all building there.

**Startup sync:** on plugin load, all currently-claimed chunks are registered as WG regions.
Claim, unclaim, join, leave, and disband events keep the regions up to date while the server
is running.

**Requirements:**

- WorldGuard must be installed and loaded.
- Toggling the option requires a restart.

**When to enable:** recommended for servers with high claim density or high block-event
throughput where DB lookups on every block break become a measurable bottleneck.

## Verify

1. Attempt territory actions inside a protected region.
2. Confirm behavior matches your region policy expectations.
