---
title: Customize Messages
parent: Step-by-Step Guides
nav_order: 3
---

# Customize Messages

## Goal

Brand and localize player-facing output with `messages.yml`.

## Steps

1. Open `plugins/PvPIndexFactions/messages.yml`.
2. Edit prefixes first so all messages match your server style.
3. Update key gameplay text:
   - invites
   - claim/unclaim feedback
   - bank actions
   - territory enter/leave titles
4. Keep MiniMessage placeholders unchanged unless you know the code path.
5. If supported in your build, run `/fa reload`; otherwise restart.
6. Validate in-game:
   - `/f help`
   - `/f invite`
   - `/f map`
   - entering claimed land

## Translation workflow

1. Duplicate `messages.yml` to a translation branch.
2. Translate values only (not keys/placeholders).
3. Test all major flows before production rollout.

## Verify success

- Messages render without raw `<placeholder>` text.
- No missing-key fallbacks appear in chat/logs.
