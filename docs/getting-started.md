---
title: Getting Started
nav_order: 2
has_children: true
---

# Getting Started

## Requirements

- Paper `1.21.4`
- Java `21+`
- File-system write access for plugin data and database files

## First production checklist

1. Confirm Java and Paper versions.
2. Install the plugin jar into `plugins/`.
3. Start once to generate defaults.
4. Configure `database.yml` and `config.yml`.
5. Restart server and verify boot logs.
6. Run `/f help` and `/fa help` as sanity checks.

## Recommended rollout strategy

- Test on a staging server first.
- Start with economy tax disabled until balancing is validated.
- Enable optional integrations one by one.

## Next step

Continue with [Installation](installation.md).
