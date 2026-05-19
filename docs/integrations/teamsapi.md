---
title: TeamsAPI
parent: Integrations
nav_order: 8
---

# TeamsAPI Integration

TeamsAPI support is optional. PvPIndex Factions runs standalone, then registers
TeamsAPI providers when TeamsAPI is detected at startup.

**Requires TeamsAPI 1.5.0+** for subcommand API support.
**Requires TeamsAPI 1.6.0+** for relation service support.
**Requires TeamsAPI 1.7.0+** for SafeZone/WarZone territory types and notification service support.
**Requires TeamsAPI 1.8.0+** for power history service support.

## Setup

1. Install [TeamsAPI](https://modrinth.com/plugin/teams-api) 1.8.0 or newer.
2. Restart the server.
3. Check startup logs for TeamsAPI provider registration.

## Provided Adapters

| Adapter | Minimum TeamsAPI |
|---------|-----------------|
| Teams service | 1.0.0 |
| Invite service | 1.1.0 |
| Warp service | 1.2.0 |
| Claim service (with SafeZone/WarZone) | 1.7.0 |
| Power service | 1.4.0 |
| Relation service | 1.6.0 |
| Notification service | 1.7.0 || Power history service | 1.8.0 |
## Claim Territory Types (TeamsAPI 1.7+)

PvPIndex Factions exposes territory classification to TeamsAPI consumers via
`ClaimTerritoryType`:

| Type | Description |
|------|-------------|
| `WILDERNESS` | Unclaimed chunk |
| `TEAM` | Claimed by a regular player faction |
| `SAFE_ZONE` | Server-admin safe zone (no PvP, no power loss) |
| `WAR_ZONE` | Server-admin war zone (always contested territory) |

Consumers can query the territory type for any chunk:

```java
TeamsClaimService claims = TeamsAPI.getClaimService();
ClaimTerritoryType type = claims.getTerritoryTypeAt(worldName, chunkX, chunkZ);
boolean safe = claims.isSafeZone(worldName, chunkX, chunkZ);
boolean war  = claims.isWarZone(worldName, chunkX, chunkZ);
```

Safe zones and war zones can also be created or removed via the claim service:

```java
// Claim as safe zone (admin action)
claims.claimSafeZone(actorUUID, worldName, chunkX, chunkZ);

// Claim as war zone (admin action)
claims.claimWarZone(actorUUID, worldName, chunkX, chunkZ);

// Remove a safe/war zone chunk
claims.unclaimSpecialZone(actorUUID, worldName, chunkX, chunkZ);
```

`TeamClaim.getOwningTeamId()` returns `Optional.empty()` for safe zones and war
zones because those territories are server-admin owned.

## Notification Service (TeamsAPI 1.7+)

PvPIndex Factions registers a `TeamsNotificationService` provider that delivers
in-game notifications to online players.

### Built-in type support

| `TeamNotificationType` | Persisted preference | Default |
|------------------------|---------------------|---------|
| `TEAM_INVITE` | `notify_invites` per-player column | enabled |
| All other types | — (always enabled) | enabled |

Players can toggle invite notifications via `/f notifications`.

### Sending a notification

```java
TeamsNotificationService notif = TeamsAPI.getNotificationService();
if (notif != null) {
    notif.sendNotification(myPlugin, playerUUID, TeamNotificationType.GENERAL, "Hello!");
}
```

### Custom notification types

Custom string types are accepted and delivered to online players, but per-player
preferences are not persisted for custom types:

```java
notif.sendNotification(myPlugin, playerUUID, "myplugin:custom_event", "Custom alert!");
```

## Subcommand API

As of TeamsAPI 1.5.0, third-party plugins can register subcommands under `/f`
using `TeamsAPI.registerSubcommand()`. PvPIndex Factions dispatches any
unrecognized `/f <name>` invocation to matching registered `TeamsSubcommand`
instances.

### Dispatch behaviour

1. The player runs `/f <name> [args...]`.
2. PvPIndex Factions checks its own command registry first.
3. If no internal command matches, it iterates all registered `TeamsSubcommand`
   instances looking for a case-insensitive name match.
4. If the subcommand declares a permission and the sender lacks it, a
   no-permission message is shown and dispatch stops.
5. If `sub.execute(sender, args)` returns `false`, a usage hint from
   `sub.getUsage()` is shown.

Tab-completion works in parallel: registered subcommand names are appended to
native `/f` completions at position 1, and `sub.tabComplete(sender, args)` is
called for deeper argument positions when that subcommand is selected.

### Registering a TeamsSubcommand

```java
public class MySubcommand implements TeamsSubcommand {
    @Override public String getName()        { return "mysubcmd"; }
    @Override public String getDescription() { return "My custom subcommand."; }
    @Override public String getPermission()  { return "myplugin.mysubcmd"; }
    @Override public String getUsage()       { return "/f mysubcmd [args]"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage("Hello from mysubcmd!");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
```

Register in your plugin's `onEnable()`:

```java
TeamsAPI.registerSubcommand(this, new MySubcommand());
```

Unregister in `onDisable()`:

```java
TeamsAPI.unregisterSubcommand(mySubcommandInstance);
```

## Power History Service (TeamsAPI 1.8+)

PvPIndex Factions registers a `TeamsPowerHistoryService` provider when TeamsAPI 1.8.0 or
newer is detected at startup. The provider is registered reflectively, so servers running
TeamsAPI 1.7.x or earlier start cleanly and the provider is silently skipped.

The in-game `/f powerhistory [<player>] [<page>]` command (alias `phist`) is the player-facing
view of the same data.

### Reading power history

```java
if (!TeamsAPI.isPowerHistoryAvailable()) return;
TeamsPowerHistoryService phist = TeamsAPI.getPowerHistoryService();

// Last 20 entries for a player
List<TeamPowerHistoryEntry> entries = phist.getPlayerPowerHistory(playerUUID, 20);

// Entries in a time range
List<TeamPowerHistoryEntry> recent = phist.getPlayerPowerHistory(
        playerUUID,
        Instant.now().minus(7, ChronoUnit.DAYS),
        Instant.now(),
        100);

// All entries for a whole team (aggregated across members)
List<TeamPowerHistoryEntry> teamHistory = phist.getTeamPowerHistory(teamUUID, 50);
```

Each `TeamPowerHistoryEntry` exposes:

| Method | Notes |
|--------|-------|
| `getEntryId()` | Unique UUID for the entry |
| `getPlayerUUID()` | Player whose power changed |
| `getType()` → `TeamPowerHistoryType` | `GAIN` or `LOSS` |
| `getDelta()` | Signed power change (positive = gain, negative = loss) |
| `getReason()` | Upper-case reason string (e.g. `KILL`, `DEATH`, `PASSIVE`) |
| `getOccurredAt()` | `Instant` the event was recorded |
| `getTeamId()` | Always `Optional.empty()` (not stored in this schema) |
| `getActorUUID()` | Always `Optional.empty()` (not stored in this schema) |
| `getDetails()` | Always `Optional.empty()` (not stored in this schema) |

### Writing power history entries

```java
phist.addPowerHistoryEntry(
        entryUUID,
        playerUUID,
        TeamPowerHistoryType.GAIN,
        PowerGainSource.GAMEPLAY,
        +5.0,
        "EXTERNAL_REWARD",
        Instant.now());
```

To remove or clear entries:

```java
phist.removePowerHistoryEntry(entryUUID);   // single entry
phist.clearPlayerPowerHistory(playerUUID);  // all entries for a player
phist.clearTeamPowerHistory(teamUUID);      // all entries for all members of a team
```

`updatePowerHistoryEntry` is not supported and always returns `false` — the underlying
schema does not store actor, team, or details fields needed for meaningful updates.

## Verify

1. Run create/invite/join/claim/warp flows.
2. Confirm consumers of TeamsAPI receive expected team events and data.
3. For territory types: claim a safezone or warzone chunk and verify
   `getTerritoryTypeAt` returns the correct `ClaimTerritoryType`.
4. For notifications: call `sendNotification` and confirm the message is
   delivered to an online player; verify `isNotificationEnabled` reflects
   the player's `/f notifications` preference for `TEAM_INVITE`.
5. For subcommands: install a plugin that registers a `TeamsSubcommand`, then
   confirm `/f <name>` dispatches correctly and tab-completion surfaces the name.
6. For power history: call `getPlayerPowerHistory` and confirm entries are
   returned for a player who has died or gained power; verify that
   `addPowerHistoryEntry` and `removePowerHistoryEntry` round-trip correctly.
