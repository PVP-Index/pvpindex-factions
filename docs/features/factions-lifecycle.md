---
title: Faction Lifecycle
parent: Features
nav_order: 1
---

# Faction Lifecycle

Core lifecycle actions:

- Create: `/f create <name>`
- Rename: `/f rename <name>`
- Describe: `/f desc <text...>`
- Set MOTD: `/f motd <text...>` or `/f motd clear`
- Disband: `/f disband`

## Faction MOTD

Officers and above can set a message of the day for their faction:

```
/f motd Welcome to the faction! PvP event on Friday.
/f motd clear
```

The MOTD is shown in chat each time a faction member logs in.
Leave the MOTD unset (or clear it) to suppress the login message.

## Notes

- Name conflicts are blocked.
- Disband clears claims, warps, invites, and membership links.

## Faction info

Use `/f info` (or `/f show`) to inspect faction details, members, leader, bank and land status.

![F info output](https://i.ibb.co/Y469RX8j/image.png)

## See also

- [Membership and Ranks](membership-and-ranks.md)
- [Invites and Notifications](invites-and-notifications.md)
