---
title: Configure MySQL
parent: Step-by-Step Guides
nav_order: 2
---

# Configure MySQL

## Goal

Run PvPIndex Factions on MySQL/MariaDB instead of embedded H2.

## Steps

1. Create a database and user on your MySQL/MariaDB server.
2. Open `plugins/PvPIndexFactions/database.yml`.
3. Set:
   - `type: mysql`
   - `mysql.host`
   - `mysql.port`
   - `mysql.database`
   - `mysql.username`
   - `mysql.password`
4. Keep pool defaults first, then tune after load testing.
5. Restart the server.
6. Check logs for successful connection and table initialization.

## Hardening tips

- Use a dedicated DB user with only required permissions.
- Restrict DB access to your server IP.
- Enable TLS if the database is remote.

## Verify success

- No SQL connection errors at startup.
- Faction actions (`/f create`, `/f claim`) persist after restart.
