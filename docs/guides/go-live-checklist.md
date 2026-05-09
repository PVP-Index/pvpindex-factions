---
title: Go-Live Checklist
parent: Step-by-Step Guides
nav_order: 5
---

# Go-Live Checklist

## Goal

Reduce production risk before opening factions gameplay to players.

## Checklist

1. Database backups configured and tested.
2. `messages.yml` validated for formatting/placeholders.
3. Core permissions assigned:
   - player command nodes
   - moderator/admin nodes
4. Economy settings reviewed:
   - bank tax on/off
   - claim costs
   - power loss/death rules
5. Integrations tested one by one (Vault, PlaceholderAPI, etc.).
6. Invite lifecycle tested:
   - invite
   - offline login notification
   - accept/decline/declineall
7. Land flow tested:
   - claim modes
   - unclaim modes
   - map rendering and claim via map
8. Load test with staff on a staging copy.
9. Final restart and log review before launch.

## Launch-day checks

1. Monitor console for storage/message errors.
2. Watch faction creation/claim events in first hour.
3. Keep rollback backup for the first 24 hours.
