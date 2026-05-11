---
title: Placeholder Reference
parent: Integrations
nav_order: 3
---

# Available Placeholders

PvPIndex Factions registers placeholders with identifier `pvpindex`:

- `%pvpindex_faction_name%`
- `%pvpindex_faction_power%`
- `%pvpindex_faction_members%`
- `%pvpindex_faction_land%`
- `%pvpindex_faction_bank%`
- `%pvpindex_player_power%`

## Return Values

- `faction_name`: faction name, or `None` if player is not in a faction.
- `faction_power`: summed faction member power as an integer string.
- `faction_members`: number of members in faction.
- `faction_land`: claimed chunk count.
- `faction_bank`: faction bank balance as a numeric string.
- `player_power`: current player power.

## Notes

- If a value cannot be resolved, PlaceholderAPI may receive an empty value.
- Faction-related placeholders return neutral defaults (`None`, `0`, or `0.0`) when the player is not in a faction.
