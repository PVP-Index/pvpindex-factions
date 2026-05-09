---
title: Customize GUI
parent: Step-by-Step Guides
nav_order: 4
---

# Customize GUI

## Goal

Tailor the `/f` GUI layout and actions for your server.

## Steps

1. Open `plugins/PvPIndexFactions/gui.yml`.
2. Review menu sections and slot mappings.
3. Adjust:
   - titles
   - item names/lore
   - action bindings
4. Keep action identifiers valid for existing command/service paths.
5. Restart (or reload if your version supports GUI reload safely).
6. Open the GUI in-game and click-test each primary action.

## UX tips

- Keep critical actions in predictable slots.
- Use consistent naming across faction/member/bank pages.
- Avoid destructive actions without confirmation affordances.

## Verify success

- GUI opens without errors.
- All configured buttons execute expected actions.
