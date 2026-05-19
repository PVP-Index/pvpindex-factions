---
title: Faction Flags
parent: Features
nav_order: 12
---

# Faction Flags

Faction flags are per-faction boolean toggles that control gameplay behavior inside a
faction's claimed territory or for its members. Officers and above can manage flags
in-game; admins can override any flag regardless of config.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/f flag` | Show all flags and their current values for your faction. | `factions.cmd.flag` |
| `/f flag list` | Explicit alias for the above. | `factions.cmd.flag` |
| `/f flag set <flag> [on\|off]` | Toggle (no third arg) or set a specific value. | `factions.cmd.flag.set` |
| `/fa flag <faction> <flag> [on\|off]` | Admin override -- bypasses `player-editable` config. | `factions.admin` |

## Built-in Flags

| Flag | Default | Description |
|---|---|---|
| `pvp` | `true` | Allow PvP inside this faction's claimed territory. |
| `friendly-fire` | `false` | Allow faction members to damage each other anywhere. |
| `explosions` | `false` | Allow explosions to destroy terrain in claimed territory. |
| `fire-spread` | `false` | Allow fire to spread inside claimed territory. |
| `open` | `false` | Allow any player to join without a pending invite. |

## Config

Default values and whether players can edit each flag are set per-flag in `config.yml`
under `factions.flags.*`:

```yaml
factions:
  flags:
    pvp:
      default: true
      player-editable: true
    friendly-fire:
      default: false
      player-editable: true
    explosions:
      default: false
      player-editable: true
    fire-spread:
      default: false
      player-editable: true
    open:
      default: false
      player-editable: true
```

Setting `player-editable: false` prevents officers from changing a flag via `/f flag set`.
Admins can still override it with `/fa flag`.

## Permissions

- `factions.cmd.flag` -- view flags (default `true`)
- `factions.cmd.flag.set` -- set flags (default `true`, officer rank enforced in-game)
- `factions.admin` -- admin override via `/fa flag`
