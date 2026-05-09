---
title: First Server Setup
parent: Step-by-Step Guides
nav_order: 1
---

# First Server Setup

## Goal

Deploy PvPIndex Factions on a fresh Paper server with safe defaults.

## Steps

1. Verify runtime versions:
   - Paper `1.21.4`
   - Java `21+`
2. Place the plugin jar into `plugins/`.
3. Start the server once, then stop it after files are generated.
4. Confirm these files exist:
   - `plugins/PvPIndexFactions/config.yml`
   - `plugins/PvPIndexFactions/database.yml`
   - `plugins/PvPIndexFactions/messages.yml`
   - `plugins/PvPIndexFactions/gui.yml`
5. Keep `database.yml` on `type: h2` for first boot unless you already run MySQL.
6. Start the server again and watch startup logs:
   - database initialized
   - command roots registered (`/f`, `/fa`)
   - optional integrations detected (or skipped gracefully)
7. In-game sanity checks:
   - `/f help`
   - `/fa help`
   - `/f create <name>`
   - `/f map once`

## Verify success

- No startup errors in console.
- `/f` commands respond correctly.
- Faction creation and map display work.
