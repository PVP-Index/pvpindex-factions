---
title: Vault
parent: Integrations
nav_order: 1
---

# Vault Integration

Vault enables economy-backed faction actions, including create/claim costs and bank flows.

## Config Keys

- `integrations.vault`
- `factions.economy.*`

## Setup

1. Install Vault.
2. Install an economy plugin that provides a Vault economy service.
3. Ensure `integrations.vault: true` in `config.yml`.
4. Restart and check startup logs for Vault availability.

## Verify

1. Run an economy-backed action such as faction creation or bank deposit.
2. Confirm balances update in your economy plugin.

## Related

- [Bank and Economy](../features/bank-and-economy.md)
- [Core Settings](../configuration/core-settings.md)
