---
title: TeamsAPI
parent: Integrations
nav_order: 8
---

# TeamsAPI Integration

TeamsAPI support is optional. PvPIndex Factions runs standalone, then registers
TeamsAPI providers when TeamsAPI is detected at startup.

**Requires TeamsAPI 1.5.0+** for subcommand API support.

## Setup

1. Install [TeamsAPI](https://modrinth.com/plugin/teams-api) 1.5.0 or newer.
2. Restart the server.
3. Check startup logs for TeamsAPI provider registration.

## Provided Adapters

- Teams service adapter
- Invite service adapter
- Warp service adapter
- Claim service adapter
- Power service adapter

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

## Verify

1. Run create/invite/join/claim/warp flows.
2. Confirm consumers of TeamsAPI receive expected team events and data.
3. For subcommands: install a plugin that registers a `TeamsSubcommand`, then
   confirm `/f <name>` dispatches correctly and tab-completion surfaces the name.
