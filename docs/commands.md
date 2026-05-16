---
title: Commands
parent: Reference
nav_order: 1
---

# Commands

## Player root

- `/f` (aliases: `/faction`, `/factions`)

> **TeamsAPI subcommands:** When [TeamsAPI](../integrations/teamsapi.md) 1.5.0+
> is present, `/f <name>` will also dispatch to any `TeamsSubcommand` registered
> by other plugins via `TeamsAPI.registerSubcommand()`. These commands appear in
> tab-completion alongside native commands.

## Admin root

- `/fa` (aliases: `/factionadmin`)

---

## Player commands (`/f`)

| Command | Permission | Description |
|---|---|---|
| `/f help` | — | List all available faction commands. |
| `/f create <name>` | `factions.cmd.create` | Create a new faction. |
| `/f disband` | `factions.cmd.disband` | Disband your faction (owner only). |
| `/f info [name]` | — | Show information about a faction. |
| `/f list [page]` | `factions.cmd.list` | List factions. |
| `/f top [page] [sort]` | `factions.cmd.top` | Show top factions by power, land, or bank. |
| `/f join [factionName]` | `factions.cmd.join` | Accept an invite and join a faction. |
| `/f leave` | `factions.cmd.leave` | Leave your current faction. |
| `/f kick <player>` | `factions.cmd.kick` | Kick a member from your faction. |
| `/f promote <player>` | `factions.cmd.promote` | Promote a faction member one rank. |
| `/f demote <player>` | `factions.cmd.demote` | Demote a faction member one rank. |
| `/f leader <player> [confirm]` | `factions.cmd.leader` | Transfer faction ownership to another member. |
| `/f rename <name>` | `factions.cmd.rename` | Rename your faction. |
| `/f desc <text...>` | `factions.cmd.desc` | Set faction description. |
| `/f home` | `factions.cmd.home` | Teleport to faction home. |
| `/f sethome` | `factions.cmd.sethome` | Set faction home at your current location. |
| `/f unsethome [confirm]` | `factions.cmd.sethome` | Unset faction home. |
| `/f fly` | `factions.cmd.fly` | Toggle faction fly (within own territory). |
| `/f map [on\|off\|once]` | `factions.cmd.map` | Show or toggle territory map notifications. |
| `/f notify [status\|invites\|territory\|tax\|all] [on\|off]` | `factions.cmd.notify` | Manage your faction notification preferences. |
| `/f gui [menu]` | `factions.cmd.gui` | Open the factions GUI. |

### `/f claim`

| Command | Permission | Description |
|---|---|---|
| `/f claim` | `factions.cmd.claim` | Claim the chunk you are standing in (one). |
| `/f claim one` | `factions.cmd.claim` | Claim the current chunk. |
| `/f claim auto [on\|off]` | `factions.cmd.claim` | Toggle auto-claim mode as you walk. |
| `/f claim square [radius]` | `factions.cmd.claim` | Claim a square of chunks around you. |
| `/f claim circle [radius]` | `factions.cmd.claim` | Claim a circle of chunks around you. |
| `/f claim fill` | `factions.cmd.claim` | Claim nearby unclaimed chunks. |
| `/f claim nearby [radius]` | `factions.cmd.claim` | Claim nearby chunks. |
| `/f claim at <chunkX> <chunkZ>` | `factions.cmd.claim` | Claim a specific chunk by coordinates. |
| `/f unclaim` | `factions.cmd.unclaim` | Unclaim the chunk you are standing in. |
| `/f unclaim auto [on\|off]` | `factions.cmd.unclaim` | Toggle auto-unclaim mode. |
| `/f unclaim square [radius]` | `factions.cmd.unclaim` | Unclaim a square of chunks. |
| `/f unclaim circle [radius]` | `factions.cmd.unclaim` | Unclaim a circle of chunks. |
| `/f unclaim fill` | `factions.cmd.unclaim` | Unclaim nearby chunks. |
| `/f unclaim all` | `factions.cmd.unclaim` | Unclaim all faction land. |

### `/f invite`

| Command | Permission | Description |
|---|---|---|
| `/f invite <player>` | `factions.cmd.invite` | Invite a player to your faction. |
| `/f invite list [faction]` | `factions.cmd.invite.list` | List pending faction invites. |
| `/f invite revoke <player>` | `factions.cmd.invite.revoke` | Revoke a pending invite. |
| `/f invite accept <faction>` | `factions.cmd.join` | Accept a faction invite. |
| `/f invite decline <faction>` | `factions.cmd.invite` | Decline a faction invite. |
| `/f invite declineall` | `factions.cmd.invite` | Decline all pending invites. |

### `/f relation`

| Command | Permission | Description |
|---|---|---|
| `/f relation <faction> <relation>` | `factions.cmd.relation` | Set relation with another faction (`ally`, `truce`, `neutral`, `enemy`). |
| `/f relation list [ally\|truce\|neutral\|enemy]` | `factions.cmd.relation` | List faction relations. |
| `/f relation wishes` | `factions.cmd.relation` | Show your faction's pending relation wishes. |

### `/f bank`

Requires Vault economy.

| Command | Permission | Description |
|---|---|---|
| `/f bank` | `factions.cmd.bank` | Show current faction bank balance. |
| `/f bank deposit <amount>` | `factions.cmd.bank` | Deposit money into the faction bank. |
| `/f bank withdraw <amount>` | `factions.cmd.bank` | Withdraw money from the faction bank. |
| `/f bank transfer <faction> <amount>` | `factions.cmd.bank.transfer` | Transfer money to another faction's bank. |
| `/f bank history [page]` | `factions.cmd.bank.history` | View faction bank transaction history. |

### `/f warp`

| Command | Permission | Description |
|---|---|---|
| `/f warp <name>` | `factions.cmd.warp` | Teleport to a faction warp. |
| `/f warp set <name>` | `factions.cmd.setwarp` | Set a faction warp at your current location. |
| `/f warp delete <name>` | `factions.cmd.setwarp` | Delete a faction warp. |
| `/f warp list [page]` | `factions.cmd.warp` | List faction warps. |

### `/f power`

| Command | Permission | Description |
|---|---|---|
| `/f power` | `factions.cmd.power` | Show power help. |
| `/f power buy <amount>` | `factions.cmd.power.buy` | Purchase personal power with money. Requires Vault and `factions.power.buy.enabled: true`. |

---

## Admin commands (`/fa`)

| Command | Permission | Description |
|---|---|---|
| `/fa help` | `factions.admin` | List admin commands. |
| `/fa bypass` | `factions.admin` | Toggle admin protection bypass mode. |
| `/fa reload` | `factions.admin` | Reload plugin config from disk. |
| `/fa disband <faction>` | `factions.cmd.disband.other` | Disband any faction by name. |
| `/fa claim <faction> [one\|square\|circle\|fill] [radius]` | `factions.cmd.claim.other` | Claim land for another faction. |
| `/fa unclaim <faction> [all\|one\|square\|circle\|fill] [radius]` | `factions.cmd.claim.other` | Unclaim land for another faction. |

---

## See also

- [Permissions](../permissions.md)
- [Features](../features/index.md)
- [Configuration Reference](../configuration/reference.md)

