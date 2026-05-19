---
title: EssentialsX
parent: Integrations
nav_order: 6
---

# EssentialsX Integration

When enabled, `/f home` and `/f warp` teleport through EssentialsX's async teleport system
instead of the native Bukkit `player.teleport()` call.

## What the integration does

- Routes `/f home` and `/f warp` through EssentialsX, respecting any configured teleport delay
  or safety checks EssentialsX enforces.
- Records the player's current location as the EssentialsX `/back` destination before teleporting,
  so players can return with `/back` after using `/f home` or `/f warp`.
- Blocks `/f home` and `/f warp` while the player is jailed in EssentialsX.

## Requirements

- EssentialsX **2.19 or later** (2.21+ recommended).
- `integrations.essentialsx.enabled: true` in `config.yml`.

## Config Keys

| Key | Default | Description |
|---|---|---|
| `integrations.essentialsx.enabled` | `false` | Enable EssentialsX teleport routing for `/f home` and `/f warp`. |

## Setup

1. Install EssentialsX.
2. Set `integrations.essentialsx.enabled: true` in `config.yml`.
3. Restart the server.
4. Check the startup log for:
   ```
   EssentialsX 2.x.x detected — home/warp teleport interop enabled.
   ```

## Verify

| Scenario | Expected behaviour |
|---|---|
| `/f home` | Teleports via EssentialsX; `/back` returns to previous position |
| `/f warp <name>` | Same async route as `/f home` |
| Player is jailed | `/f home` and `/f warp` blocked with a message |
| EssentialsX absent or disabled | Plugin starts cleanly; teleports use native Bukkit fallback |

## Behaviour without EssentialsX

The integration is entirely optional. If EssentialsX is not installed, not enabled in config,
or does not implement the expected API, the plugin falls back to native teleportation silently.
No features are lost; `/f home` and `/f warp` still work normally.
