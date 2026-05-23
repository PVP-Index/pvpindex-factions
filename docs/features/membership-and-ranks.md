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

## Notes

- Rank boundaries and ownership safeguards are enforced.
- Leadership transfer uses a confirmation guard for self-targeting scenarios.

## Member size cap

A server administrator can limit how many members a faction may have by setting
`factions.max-members` in `config.yml`. A value of `0` (the default) means unlimited.

When the cap is reached:
- `/f join` is blocked with a "faction is full" message.
- Invites can still be sent, but accepting them is blocked until a slot opens.
