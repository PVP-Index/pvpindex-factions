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
4. Victim's land is checked against their max land. If they are over, the claim continues.
5. **F5** — If `factions.overclaiming.offline-protection.enabled: true`, the claim is blocked
   when all members of the defending faction are currently offline.
6. **F6** — If `factions.war.shield.enabled: true`, the claim is blocked while the defending
   faction's configured war shield window is active.
7. Attacker receives `claim.overclaimed`; all online victim members receive `claim.overclaimed-victim`.

## Raidable state broadcast (F4)

When `factions.raidable.broadcast.enabled: true` (default), the power-tick engine notifies
faction members whenever their faction transitions between the safe and raidable states.
Set `factions.raidable.broadcast.server-wide: true` to also broadcast server-wide announcements.

## War shield (F6)

Admins can assign a daily UTC protection window to any faction:

```
/fa shield <faction> <start-hour (0-23)> <duration-hours>
/fa shield <faction> clear
```

While the window is active, the faction's land cannot be overclaimed. Requires
`factions.war.shield.enabled: true`.

## Config keys

- `factions.land.max-per-command`
- `factions.land.per-power`
- `factions.land.max`
- `factions.land.buffer-zone`
- `factions.overclaiming.enabled` (default `false`)
- `factions.overclaiming.require-enemy-relation` (default `true`)
- `factions.overclaiming.offline-protection.enabled` (default `false`) — block overclaim when all defenders offline (F5)
- `factions.raidable.broadcast.enabled` (default `true`) — notify members on raidable state change (F4)
- `factions.raidable.broadcast.server-wide` (default `false`) — also broadcast server-wide (F4)
- `factions.war.shield.enabled` (default `false`) — enable the war shield system (F6)
- `factions.war.shield.max-duration-hours` (default `8`) — maximum shield window an admin may set (F6)
