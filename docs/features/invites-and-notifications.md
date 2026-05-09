---
title: Invites and Notifications
parent: Features
nav_order: 3
---

# Invites and Notifications

Invite flow:

- Send: `/f invite <player>`
- Accept/decline: `/f invite accept <faction>`, `/f invite decline <faction>`
- Bulk decline: `/f invite declineall`
- Revoke/list: `/f invite revoke <player>`, `/f invite list`

Player-facing join UX:

- `/f join` without args lists pending invites with clickable accepts.
- Login invite summary is delivered through notification engine.

Config:

- `factions.invites.ttl-hours`

Per-player controls:

- `/f notify invites on|off`
