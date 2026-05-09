---
title: GUI Configuration
parent: Configuration
nav_order: 4
---

# GUI Configuration (`gui.yml`)

The `/f` GUI is fully configurable via menu definitions.

## Structure

- `gui.enabled`
- `gui.default-menu`
- `gui.menus.<menu-id>.title`
- `gui.menus.<menu-id>.size`
- `gui.menus.<menu-id>.items.*`

## Supported actions

- `RUN_COMMAND`
- `SUGGEST_COMMAND`
- `OPEN_MENU`
- `REFRESH`
- `CLOSE`

## Placeholders

- `{player}`
- `{faction}`
- `{faction_members}`
- `{faction_land}`
- `{faction_bank}`
- `{power}`
- `{max_power}`
