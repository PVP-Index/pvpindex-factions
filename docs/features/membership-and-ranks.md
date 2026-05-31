---
title: Membership and Ranks
parent: Features
nav_order: 2
---

# Membership and Ranks

Membership actions:

- Join/leave: `/f join`, `/f leave`
- Remove member: `/f kick <player>`

Rank management:

- Promote: `/f promote <player>`
- Demote: `/f demote <player>`
- Transfer leadership: `/f leader <player>`
- Role management: `/f role list|create|rename|setpriority|setprefix|delete|assign`

## Custom Roles and Prefixes

- Custom roles are configurable through `roles.custom.*` in `roles.yml`.
- Prefix editing is configurable through `roles.prefix.*` in `roles.yml`.
- Per-faction role overrides are globally controlled by `roles.overrides.enabled` in `roles.yml`.
- Core roles (`Owner`, `Officer`, `Member`) remain protected from destructive edits.
- Role authority is enforced consistently across `/f promote`, `/f demote`, `/f leader`, and `/f role assign`.

## Notes

- Rank boundaries and ownership safeguards are enforced.
- Leadership transfer uses a confirmation guard for self-targeting scenarios.

## Member size cap

A server administrator can limit how many members a faction may have by setting
`factions.max-members` in `config.yml`. A value of `0` (the default) means unlimited.

When the cap is reached:
- `/f join` is blocked with a "faction is full" message.
- Invites can still be sent, but accepting them is blocked until a slot opens.
