---
title: Admin and Moderation
parent: Features
nav_order: 11
---

# Admin and Moderation

Admin root:

- `/fa`

Core actions:

- `/fa bypass`
- `/fa claim`
- `/fa unclaim`
- `/fa disband <faction>`
- `/fa reload`
- `/fa safezone [one|square|circle|remove] [radius]`
- `/fa warzone [one|square|circle|remove] [radius]`

Permissions:

- `factions.admin` — full access to all `/fa` commands
- `factions.cmd.safezone` — assign / remove safe zone chunks (default op)
- `factions.cmd.warzone` — assign / remove war zone chunks (default op)

Config toggles:

- `factions.zones.safe-zone.enabled` (default `true`) — disable to treat safe zone chunks as Wilderness
- `factions.zones.war-zone.enabled` (default `true`) — disable to treat war zone chunks as Wilderness

Operational notes:

- Use admin claim/unclaim carefully in production to avoid player claim conflicts.
- Safe zone and war zone chunks can only be assigned or removed by admins — players cannot claim over them.
