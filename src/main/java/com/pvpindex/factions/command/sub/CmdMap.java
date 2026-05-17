package com.pvpindex.factions.command.sub;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.BoardEntry;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.bukkit.entity.Player;

/** {@code /f map [on|off|once] [--size=<size>]}. */
public final class CmdMap extends FactionCommand {

    public CmdMap() {
        super("map");
        setPermission("factions.cmd.map");
        setDescription("Show or toggle territory map notifications.");
        setOptionalArgs("[on|off|once]", "[--size=<size>]");
        setRequiresPlayer(true);
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final ParsedCommandArgs parsedArgs = parseArguments(ctx.getArgs(), Set.of("size"));
        if (parsedArgs.hasError()) {
            MsgUtil.send(player, parsedArgs.errorMessage());
            return;
        }
        final List<String> positional = parsedArgs.positionalArgs();
        if (positional.size() > 1) {
            MsgUtil.send(player, "<red>Usage: /f map [on|off|once] [--size=<size>]");
            return;
        }
        final String mode = positional.isEmpty() ? "" : positional.get(0).toLowerCase();
        Integer parsedSize = null;
        final String sizeArg = parsedArgs.optionValue("size");
        if (sizeArg != null) {
            try {
                parsedSize = Integer.parseInt(sizeArg);
            } catch (NumberFormatException ex) {
                MsgUtil.send(player, "<red>Map size must be a number.");
                return;
            }
            if (parsedSize < 1) {
                MsgUtil.send(player, "<red>Map size must be at least 1.");
                return;
            }
        }
        try {
            final PlayerModel model = ctx.getRepos().players().findOrCreate(player.getUniqueId().toString());
            if ("on".equals(mode)) {
                model.setTerritoryTitles(true);
                ctx.getRepos().players().save(model);
                MsgUtil.send(player, "<green>Territory titles enabled.");
                return;
            }
            if ("off".equals(mode)) {
                model.setTerritoryTitles(false);
                ctx.getRepos().players().save(model);
                MsgUtil.send(player, "<yellow>Territory titles disabled.");
                return;
            }
        } catch (StorageException e) {
            MsgUtil.send(player, "<red>Failed to update map preference.");
            return;
        }
        final int radius = parsedSize != null
            ? parsedSize
            : Math.max(1, ctx.getConfig().getMapOnceRadius());
        renderOnce(ctx, player, radius);
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        final List<String> args = ctx.getArgs();
        final boolean hasModeArg = args.stream().anyMatch(this::isModeToken);
        final boolean hasSizeArg = args.stream().anyMatch(this::isSizeToken);

        if (argIndex <= 1) {
            final java.util.ArrayList<String> out = new java.util.ArrayList<>();
            if (!hasModeArg) {
                out.addAll(List.of("on", "off", "once"));
            }
            if (!hasSizeArg) {
                out.addAll(List.of("--size=", "--size=1", "--size=2", "--size=3", "--size=4", "--size=5"));
            }
            return out;
        }
        return List.of();
    }

    private boolean isModeToken(final String token) {
        final String lowered = token.toLowerCase();
        return "on".equals(lowered) || "off".equals(lowered) || "once".equals(lowered);
    }

    private boolean isSizeToken(final String token) {
        return token.equalsIgnoreCase("--size") || token.toLowerCase().startsWith("--size=");
    }

    private void renderOnce(final CommandContext ctx, final Player player, final int radius) {
        final int cx = player.getLocation().getChunk().getX();
        final int cz = player.getLocation().getChunk().getZ();
        final int playerY = player.getLocation().getBlockY();
        final String world = player.getWorld().getName();
        final String playerFactionId = resolvePlayerFactionId(ctx, player);
        MsgUtil.send(player, "<dark_gray>----------------------------------------");
        MsgUtil.send(player, "<gold> Territory Map <gray>(" + world + " @ " + cx + ", " + cz + ")");
        MsgUtil.send(player, "<gray>Use <white>/f map on</white> to keep territory titles enabled.");
        for (int z = cz - radius; z <= cz + radius; z++) {
            final StringBuilder lineBuilder = new StringBuilder("<dark_gray>| ");
            for (int x = cx - radius; x <= cx + radius; x++) {
                lineBuilder.append(cellTag(ctx, world, x, z, cx, cz, playerY, playerFactionId));
                lineBuilder.append("<dark_gray> ");
            }
            lineBuilder.append("<dark_gray>| ");
            MsgUtil.send(player, lineBuilder.toString());
        }
        MsgUtil.send(player, "<gray>Legend: <white>■ <gray>you, <green>■ <gray>your faction, "
            + "<yellow>■ <gray>other faction, <aqua>■ <gray>safezone, <red>■ <gray>warzone, "
            + "<dark_gray>■ <gray>wilderness");
        MsgUtil.send(player, "<dark_gray>----------------------------------------");
    }

    private String cellTag(
            final CommandContext ctx,
            final String world,
            final int x,
            final int z,
            final int cx,
            final int cz,
            final int playerY,
            final String playerFactionId) {
        final String locationHint = "<gray>Chunk X: <white>" + x + "</white><newline>"
            + "<gray>Chunk Z: <white>" + z + "</white><newline>"
            + "<gray>Player Y: <white>" + playerY + "</white>";
        if (x == cx && z == cz) {
            return "<hover:show_text:'<white>Your current chunk</white><newline>"
                + locationHint + "<newline><dark_gray>Click to refresh map'>"
                + "<click:run_command:'/f map once'><white>\u25a0</white></click></hover>";
        }
        try {
            final Optional<BoardEntry> entry = ctx.getRepos().board().findByChunk(world, x, z);
            if (entry.isEmpty()) {
                final String claimCmd = "/f claim at " + x + " " + z;
                return "<hover:show_text:'<gray>Wilderness</gray><newline>"
                    + locationHint + "<newline><green>Click to claim this chunk'>"
                    + "<click:run_command:'" + claimCmd
                    + "'><dark_gray>\u25a0</dark_gray></click></hover>";
            }
            final String factionId = entry.get().getFactionId();
            if (FactionModel.SAFEZONE_ID.equals(factionId)) {
                return "<hover:show_text:'<aqua>Safezone</aqua><newline>"
                    + locationHint + "'><aqua>\u25a0</aqua></hover>";
            }
            if (FactionModel.WARZONE_ID.equals(factionId)) {
                return "<hover:show_text:'<red>Warzone</red><newline>"
                    + locationHint + "'><red>\u25a0</red></hover>";
            }
            final Optional<FactionModel> faction = ctx.getRepos().factions().find(factionId);
            final String factionName = faction.map(FactionModel::getName).orElse("Unknown");
            if (playerFactionId != null && playerFactionId.equals(factionId)) {
                return "<hover:show_text:'<green>Your faction</green><newline>"
                    + "<gray>Faction: <white>" + factionName + "</white><newline>"
                    + locationHint + "<newline><dark_gray>Click for faction info'>"
                    + "<click:suggest_command:'/f info " + factionName
                    + "'><green>\u25a0</green></click></hover>";
            }
            return "<hover:show_text:'<yellow>Claimed land</yellow><newline>"
                + "<gray>Faction: <white>" + factionName + "</white><newline>"
                + locationHint + "<newline><dark_gray>Click for faction info'>"
                + "<click:suggest_command:'/f info " + factionName
                + "'><yellow>\u25a0</yellow></click></hover>";
        } catch (StorageException e) {
            return "<hover:show_text:'<red>Failed to load claim data</red><newline>"
                + locationHint + "'><dark_gray>\u25a0</dark_gray></hover>";
        }
    }

    private String resolvePlayerFactionId(final CommandContext ctx, final Player player) {
        try {
            final Optional<PlayerModel> model = ctx.getRepos().players().find(player.getUniqueId().toString());
            return model.map(PlayerModel::getFactionId).orElse(null);
        } catch (StorageException ignored) {
            return null;
        }
    }

}
