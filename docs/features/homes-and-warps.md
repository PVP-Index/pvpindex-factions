---
title: Homes and Warps
parent: Features
nav_order: 6
---

# Homes and Warps

Home:

- Set: `/f sethome`
- Use: `/f home`
- Remove: `/f unsethome confirm`

Warps:

- Teleport: `/f warp <name>` (or `/f warp <name> <password>` for password-protected warps)
- Set/update: `/f warp set <name> [here|x y z world]`
- Delete: `/f warp delete <name>`
- List: `/f warp list [page]`
- Set password (officer+): `/f warp password <name> [password | clear]`
- Set per-use cost (officer+): `/f warp cost <name> <amount>`

### Warp passwords

Officers and above can protect a warp with a password:

```
/f warp password spawn secret123   # set
/f warp password spawn             # view current status
/f warp password spawn clear       # remove
```

Players must supply the correct password as a second argument:

```
/f warp spawn secret123
```

### Warp per-use cost

Officers and above can charge players a Vault economy fee each time a warp is used:

```
/f warp cost spawn 50.0   # charge 50 per teleport
/f warp cost spawn 0      # make free again
```

Requires an economy provider (e.g. Vault + EssentialsX). Players with insufficient
balance are blocked. The charge is deducted immediately before teleportation.

Config keys:

- `factions.max-warps`
- `factions.warp.list.page-size`
