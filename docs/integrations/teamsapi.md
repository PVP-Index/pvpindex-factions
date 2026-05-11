---
title: TeamsAPI
parent: Integrations
nav_order: 8
---

# TeamsAPI Integration

TeamsAPI support is optional. PvPIndex Factions runs standalone, then registers TeamsAPI providers when TeamsAPI is detected.

## Setup

1. Install a compatible TeamsAPI version.
2. Restart the server.
3. Check startup logs for TeamsAPI provider registration.

## Provided Adapters

- Teams service adapter
- Invite service adapter
- Warp service adapter
- Claim service adapter
- Power service adapter

## Verify

1. Run create/invite/join/claim/warp flows.
2. Confirm consumers of TeamsAPI receive expected team events and data.
