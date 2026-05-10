---
title: Troubleshooting
parent: Operations
nav_order: 2
---

# Troubleshooting

This page is written for server owners running production servers.

## Plugin does not enable on startup

Symptoms:
- Plugin is red in `/plugins`
- Console shows enable-time errors

Checks:
1. Confirm server version is Paper `1.21.4`.
2. Confirm Java runtime is `21+` (`java -version`).
3. Ensure only one factions plugin is installed to avoid command conflicts.
4. Check `plugins/PvPIndexFactions/` is writable.

Fix:
1. Remove incompatible or duplicate factions jars.
2. Update Java runtime to 21+.
3. Restart and watch first error in console (root cause is usually the first stack trace).

## Database connection errors

Symptoms:
- Startup logs show SQL connection timeout/authentication errors
- Factions data does not persist after restart

Checks:
1. Open `plugins/PvPIndexFactions/database.yml`.
2. Verify `type` is correct (`h2` or `mysql`).
3. For MySQL: host, port, database, username, password, firewall.
4. Confirm MySQL user has permissions for schema/table creation.

Fix:
1. Correct credentials and host settings.
2. Test DB reachability from host machine.
3. Restart server and verify initialization logs.

## Invites are missing or expired unexpectedly

Symptoms:
- Players cannot accept invite they expected
- `/f join` shows no pending invites

Checks:
1. Review `factions.invites.ttl-hours` in `config.yml`.
2. Confirm enough time has not passed since invite creation.
3. Ask player to relog to trigger join notification summary.

Fix:
1. Increase invite TTL if your server expects longer response times.
2. Re-send invite if expired.
3. Use `/f invite list` to verify current active invites.

## Claiming or unclaiming does not work

Symptoms:
- `/f claim` or `/f unclaim` returns denial messages
- Map click-to-claim appears inactive

Checks:
1. Confirm player is in a faction with required rank.
2. Check claim limits and power.
3. Verify economy costs can be paid (if enabled).
4. Verify territory is not blocked by protection integrations.
5. Confirm admin bypass state is not causing confusion during tests.

Fix:
1. Resolve rank/permission limits.
2. Adjust power/economy settings or faction bank balance.
3. Review `integrations.*` toggles for WorldGuard/LWC interactions.

## `/f map` looks wrong or not updating

Symptoms:
- Map output is stale, clipped, or confusing
- Territory transitions are not shown

Checks:
1. Verify map mode: `/f map once|on|off`.
2. Confirm player notification settings: `/f notify territory on`.
3. Confirm claims actually exist in nearby chunks.

Fix:
1. Toggle map off/on to refresh stream mode.
2. Re-test in an area with known claimed chunks.
3. Restart server if recent config changes were not reloaded.

## Economy and bank issues

Symptoms:
- Deposit/withdraw/transfer fails
- Bank tax behaves unexpectedly

Checks:
1. Confirm Vault and an economy provider are installed.
2. Verify economy toggles under `factions.economy.*`.
3. Review tax settings:
   - `factions.economy.tax.enabled`
   - `factions.economy.tax.rate`
   - `factions.economy.tax.interval-hours`
   - `factions.economy.tax.min-bank-balance`

Fix:
1. Install/enable Vault ecosystem plugins.
2. Tune tax values for your economy scale.
3. Use `/f bank history` to inspect transaction flow.

## Performance and stability checks

Symptoms:
- Lag spikes during high faction activity
- Slow command response in large datasets

Checks:
1. Use MySQL for larger networks instead of embedded H2.
2. Review server timings while players use `/f map` and claim modes.
3. Ensure host has enough memory and disk throughput.

Fix:
1. Move to MySQL and tune pool size.
2. Reduce aggressive map usage radius if configured high.
3. Restart during low-traffic windows after major config updates.

## When to escalate

Collect this before requesting support:
1. Full startup log section for PvPIndex Factions.
2. Current `config.yml`, `database.yml` (with secrets removed).
3. Exact command used and player-facing output.
4. Java version, Paper build, plugin version.
