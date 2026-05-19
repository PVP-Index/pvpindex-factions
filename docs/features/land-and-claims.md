---
title: Land and Claims
parent: Features
nav_order: 4
---

# Land and Claims

Claim modes:

- `/f claim`
- `/f claim one`
- `/f claim square <radius>`
- `/f claim circle <radius>`
- `/f claim fill`
- `/f claim nearby`
- `/f claim at <chunkX> <chunkZ>`

Unclaim modes:

- `/f unclaim`
- `/f unclaim one`
- `/f unclaim square <radius>`
- `/f unclaim circle <radius>`
- `/f unclaim fill`
- `/f unclaim all confirm`

Auto territory:

- `/f claim auto on|off`
- `/f unclaim auto on|off`

## Overclaiming

When `factions.overclaiming.enabled` is `true`, a faction may claim an enemy's chunk if the
victim's current land count exceeds their maximum land (power-based). Border adjacency is not
required for overclaims. The feature is opt-in and disabled by default.

Overclaim flow:

1. Attacker runs `/f claim` on an already-claimed enemy chunk.
2. Plugin checks that overclaiming is enabled and the chunk is not a system zone.
3. If `factions.overclaiming.require-enemy-relation: true` (default), the victim must be `ENEMY`.
4. Victim's land is checked against their max land. If they are over, the chunk is taken.
5. Attacker receives `claim.overclaimed`; all online victim members receive `claim.overclaimed-victim`.

## Config keys

- `factions.land.max-per-command`
- `factions.land.per-power`
- `factions.land.max`
- `factions.land.buffer-zone`
- `factions.overclaiming.enabled` (default `false`)
- `factions.overclaiming.require-enemy-relation` (default `true`)
