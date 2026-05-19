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
- `factions.overclaiming.*` overclaim and raid rules
- `factions.raidable.*` raidable state broadcast
- `factions.war.*` war shield
- `factions.economy.*` create/claim costs and tax
- `factions.fly.*` faction flight behavior
- `factions.notifications.*` notification controls

## Operational recommendations

- Keep `factions.land.max-per-command` conservative for performance.
- Enable tax gradually and monitor bank economy impact.
- Tune power regen and loss to match server PvP pacing.
- Enable war/power improvement features one at a time and observe their impact before stacking them.

## Power model tuning knobs

- `factions.power.tick-interval-seconds`
- `factions.power.gain-on-kill.enabled` / `.amount`
- `factions.power.gain-on-kill.scale.enabled` / `.min-factor` / `.max-factor` — scale kill rewards by power ratio (F3)
- `factions.power.buy.enabled` / `.cost-per-point` / `.max-per-purchase`
- `factions.power.inactive-exclusion.enabled` / `.days` — exclude long-offline members from max-land (F1)
- `factions.power.death-streak.enabled` / `.window-seconds` / `.multiplier` — escalate loss on consecutive deaths (F2)

## Overclaiming and raid rules

- `factions.overclaiming.enabled` (default `false`)
- `factions.overclaiming.require-enemy-relation` (default `true`)
- `factions.overclaiming.offline-protection.enabled` (default `false`) — block overclaim when all defenders offline (F5)

## Raidable broadcast

- `factions.raidable.broadcast.enabled` (default `true`) — notify members on raidable state change (F4)
- `factions.raidable.broadcast.server-wide` (default `false`) — also broadcast server-wide (F4)

## War shield

- `factions.war.shield.enabled` (default `false`) — enable the daily UTC protection window system (F6)
- `factions.war.shield.max-duration-hours` (default `8`) — maximum window an admin may assign (F6)

## Other tuning knobs

- `factions.map.once-radius`
- `factions.list.page-size`
- `factions.top.page-size`
- `factions.economy.bank.history.page-size`
- `factions.warp.list.page-size`
