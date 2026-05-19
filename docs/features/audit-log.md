---
title: Audit Log
parent: Features
nav_order: 12
---

# Audit Log

The audit log records key faction actions and lets officers review their faction's history
in-game. Staff can view the audit log for any faction.

## Commands

| Command | Permission | Description |
|---|---|---|
| `/f audit [page]` | `factions.cmd.audit` | Page through your faction's audit log (officer or above). |
| `/f audit [page] --action=<action>` | `factions.cmd.audit` | Filter audit log by action type. |
| `/fa audit <faction> [page]` | `factions.admin` | View the audit log for any faction (staff, console-friendly). |
| `/fa audit <faction> [page] --action=<action>` | `factions.admin` | View filtered audit log for any faction. |

## Tracked actions

| Action ID | Description |
|---|---|
| `claim` | A chunk was claimed for the faction. |
| `unclaim` | A chunk was unclaimed. |
| `relation-change` | A relation was set with another faction. |
| `kick` | A member was kicked from the faction. |
| `promote` | A member was promoted one rank. |
| `demote` | A member was demoted one rank. |
| `bank-deposit` | Money was deposited into the faction bank. |
| `bank-withdraw` | Money was withdrawn from the faction bank. |
| `bank-transfer` | Money was transferred to another faction's bank. |

## Config keys

| Key | Default | Description |
|---|---|---|
| `factions.audit.page-size` | `10` | Entries per page for `/f audit` and `/fa audit`. |

## Permissions

| Node | Default | Description |
|---|---|---|
| `factions.cmd.audit` | `op` | Access to `/f audit`. Enforced at officer rank in-game. |
| `factions.admin` | op | Access to `/fa audit <faction>`. |

## Operational notes

- Audit entries are stored in the database and survive restarts.
- Officers and above can view their own faction's log; members cannot.
- The `--action` filter accepts any action ID listed in the table above.
  Pass an invalid ID to see the list of valid values.
- Log entries are shown newest-first.
