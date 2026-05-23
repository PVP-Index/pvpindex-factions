---
title: dynmap
parent: Integrations
nav_order: 5
---

# dynmap Integration

dynmap integration renders faction territory claims as coloured area regions on the
live web map. Regions are created, updated, and removed automatically as chunks are
claimed and unclaimed.

**Supported dynmap versions:** 3.x (tested 3.4 through 3.8).

## Prerequisites

- dynmap 3.x installed and enabled on the server.
- `integrations.dynmap: true` in `config.yml` (enabled by default).

## Setup

1. Install dynmap on your server.
2. Confirm `integrations.dynmap: true` in `config.yml` (it is the default — no change
   needed for a fresh install).
3. Restart the server.

## Verifying the hook

A successful hook produces this line in the server log during startup:

```
dynmap hooked - faction territory layer enabled.
```

The plugin startup summary also reflects the integration state:

```
Integrations: ..., Dynmap=enabled, ...
```

After the server is up, open the dynmap web UI and confirm faction territory regions
are visible. Claim or unclaim a chunk and verify the overlay updates.

## Config reference

| Key | Default | Description |
|---|---|---|
| `integrations.dynmap` | `true` | Enables the dynmap faction territory layer |

## Notes

- The integration is soft-optional. If dynmap is not installed, the hook silently
  disables and no errors are logged.
- Changes to `integrations.dynmap` require a server restart.
- Territory updates are pushed to dynmap asynchronously on claim and unclaim events.
- The faction territory layer is registered under the marker set id `factions`.

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Startup log shows `Dynmap=disabled` | dynmap plugin not detected, or `integrations.dynmap: false` in config |
| Startup log shows `Failed to hook dynmap:` | dynmap loaded but the marker API was unavailable at hook time — check dynmap's own logs |
| Regions missing for some claims | dynmap render queue lag — trigger a `/dynmap fullrender` or wait for the next render cycle |
