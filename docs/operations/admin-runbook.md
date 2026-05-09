---
title: Admin Runbook
parent: Operations
nav_order: 1
---

# Admin Runbook

## Daily checks

- Confirm plugin enabled successfully after restart.
- Check faction economy/tax behavior.
- Verify claim/unclaim workflows and event logs.

## Safe changes

- Message and GUI updates can usually be reloaded with `/fa reload`.
- Database backend changes require maintenance window and restart.

## Incident handling

- Use logs first for database and dependency failures.
- Disable optional integration toggles if third-party plugin is unstable.
