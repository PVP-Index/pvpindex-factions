---
title: Installation
nav_order: 3
parent: Getting Started
---

# Installation

## 1. Install plugin

Copy the shaded jar into:

```text
plugins/
```

## 2. Generate config files

Start the server once. PvPIndex Factions will generate:

- `plugins/PvPIndexFactions/config.yml`
- `plugins/PvPIndexFactions/database.yml`
- `plugins/PvPIndexFactions/messages.yml`
- `plugins/PvPIndexFactions/gui.yml`

## 3. Configure backend

Edit `database.yml`:

- `type: h2` for embedded storage
- `type: mysql` for external database servers

## 4. Restart and verify

- Restart server
- Check startup logs for integration detection and DB initialization
- Validate command registration (`/f`, `/fa`)
