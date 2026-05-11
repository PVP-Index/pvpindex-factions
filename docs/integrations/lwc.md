---
title: LWC / LWCX
parent: Integrations
nav_order: 7
---

# LWC / LWCX Integration

LWC integration helps reconcile chest protections when territory ownership changes.

## Config Keys

- `integrations.lwc.enabled`
- `integrations.lwc.require-build-rights-to-create`
- `integrations.lwc.remove-if-no-build-rights`
- `integrations.lwc.remove-on-claim-change`

## Setup

1. Install LWC or LWCX.
2. Enable `integrations.lwc.enabled`.
3. Tune the LWC behavior keys to match your server policy.
4. Restart and run claim/unclaim tests.

## Verify

1. Create protections in faction land.
2. Transfer/lose ownership of chunks and confirm stale protections are handled as configured.
