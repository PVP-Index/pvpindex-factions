---
title: Core Settings
parent: Configuration
nav_order: 1
---

# Core Settings (`config.yml`)

Key groups:

- `factions.max-*` limits
- `factions.power.*` power model
- `factions.land.*` claiming limits and ratios
- `factions.economy.*` create/claim costs and tax
- `factions.fly.*` faction flight behavior
- `factions.notifications.*` notification controls

## Operational recommendations

- Keep `factions.land.max-per-command` conservative for performance.
- Enable tax gradually and monitor bank economy impact.
- Tune power regen and loss to match server PvP pacing.

## New tuning knobs

- `factions.power.tick-interval-seconds`
- `factions.map.once-radius`
- `factions.list.page-size`
- `factions.top.page-size`
- `factions.economy.bank.history.page-size`
- `factions.warp.list.page-size`
