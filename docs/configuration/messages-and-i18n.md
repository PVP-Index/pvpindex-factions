---
title: Messages and Translation
parent: Configuration
nav_order: 3
---

# Messages and Translation (`messages.yml`)

PvPIndex Factions supports key-based message templates with MiniMessage formatting.

## Translation workflow

1. Copy `messages.yml`
2. Translate values (keep keys unchanged)
3. Preserve placeholders such as `{player}`, `{faction}`, `{amount}`
4. Reload with `/fa reload`

## Best practices

- Do not rename keys.
- Keep placeholders intact.
- Keep action labels concise for chat readability.
