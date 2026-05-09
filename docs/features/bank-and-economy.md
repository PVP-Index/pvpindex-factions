---
title: Bank and Economy
parent: Features
nav_order: 7
---

# Bank and Economy

Bank actions:

- Balance: `/f bank`
- Deposit: `/f bank deposit <amount>`
- Withdraw: `/f bank withdraw <amount>`
- Transfer: `/f bank transfer <faction> <amount>`
- History: `/f bank history [page]`

Tax engine:

- Periodic faction bank tax (optional)
- Transaction entries recorded as `TAX`

Config keys:

- `factions.economy.enabled`
- `factions.economy.cost-create`
- `factions.economy.cost-claim`
- `factions.economy.tax.enabled`
- `factions.economy.tax.rate`
- `factions.economy.tax.interval-hours`
- `factions.economy.tax.min-bank-balance`
- `factions.economy.tax.min-charge-amount`
- `factions.economy.tax.notify-members`
- `factions.economy.bank.history.page-size`
