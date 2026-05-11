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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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
        final ParsedMapArgs parsed = parseArgs(ctx.getArgs());
        if (parsed.error != null) {
            MsgUtil.send(player, parsed.error);
            return;
        }
        final String mode = parsed.mode;
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
        final int radius = parsed.size != null
            ? parsed.size
            : Math.max(1, ctx.getConfig().getMapOnceRadius());
        renderOnce(ctx, player, radius);
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return List.of("on", "off", "once", "--size=");
        }
        if (argIndex == 1) {
            return List.of("--size=");
        }
        return List.of();
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
            Component line = Component.text("| ", NamedTextColor.DARK_GRAY);
            for (int x = cx - radius; x <= cx + radius; x++) {
                line = line.append(cellComponent(ctx, world, x, z, cx, cz, playerY, playerFactionId))
                    .append(Component.text(" ", NamedTextColor.DARK_GRAY));
            }
            line = line.append(Component.text("| ", NamedTextColor.DARK_GRAY));
            MsgUtil.send(player, line);
        }
        MsgUtil.send(player, "<gray>Legend: <white>■ <gray>you, <green>■ <gray>your faction, "
            + "<yellow>■ <gray>other faction, <aqua>■ <gray>safezone, <red>■ <gray>warzone, "
            + "<dark_gray>■ <gray>wilderness");
        MsgUtil.send(player, "<dark_gray>----------------------------------------");
    }

    private Component cellComponent(
            final CommandContext ctx,
            final String world,
            final int x,
            final int z,
            final int cx,
            final int cz,
            final int playerY,
            final String playerFactionId) {
        final Component symbol = Component.text("■");
        final String locationHint = "<gray>Chunk X: <white>" + x + "</white><newline>"
            + "<gray>Chunk Z: <white>" + z + "</white><newline>"
            + "<gray>Player Y: <white>" + playerY + "</white>";
        if (x == cx && z == cz) {
                return symbol.color(NamedTextColor.WHITE)
                    .hoverEvent(HoverEvent.showText(MsgUtil.parse("<white>Your current chunk</white><newline>"
                    + locationHint + "<newline><dark_gray>Click to refresh map")))
                    .clickEvent(ClickEvent.runCommand("/f map once"));
        }
        try {
            final Optional<BoardEntry> entry = ctx.getRepos().board().findByChunk(world, x, z);
            if (entry.isEmpty()) {
                final String claimCmd = "/f claim at " + x + " " + z;
                return symbol.color(NamedTextColor.DARK_GRAY)
                    .hoverEvent(HoverEvent.showText(MsgUtil.parse("<gray>Wilderness</gray><newline>"
                        + locationHint + "<newline><green>Click to claim this chunk")))
                    .clickEvent(ClickEvent.runCommand(claimCmd));
            }
            final String factionId = entry.get().getFactionId();
            if (FactionModel.SAFEZONE_ID.equals(factionId)) {
                return symbol.color(NamedTextColor.AQUA)
                    .hoverEvent(HoverEvent.showText(MsgUtil.parse("<aqua>Safezone</aqua><newline>"
                        + locationHint)));
            }
            if (FactionModel.WARZONE_ID.equals(factionId)) {
                return symbol.color(NamedTextColor.RED)
                    .hoverEvent(HoverEvent.showText(MsgUtil.parse("<red>Warzone</red><newline>"
                        + locationHint)));
            }
            final Optional<FactionModel> faction = ctx.getRepos().factions().find(factionId);
            final String factionName = faction.map(FactionModel::getName).orElse("Unknown");
            if (playerFactionId != null && playerFactionId.equals(factionId)) {
                return symbol.color(NamedTextColor.GREEN)
                    .hoverEvent(HoverEvent.showText(MsgUtil.parse("<green>Your faction</green><newline>"
                        + "<gray>Faction: <white>" + factionName + "</white><newline>"
                        + locationHint + "<newline><dark_gray>Click for faction info")))
                    .clickEvent(ClickEvent.suggestCommand("/f info " + factionName));
            }
            return symbol.color(NamedTextColor.YELLOW)
                .hoverEvent(HoverEvent.showText(MsgUtil.parse("<yellow>Claimed land</yellow><newline>"
                    + "<gray>Faction: <white>" + factionName + "</white><newline>"
                    + locationHint + "<newline><dark_gray>Click for faction info")))
                .clickEvent(ClickEvent.suggestCommand("/f info " + factionName));
        } catch (StorageException e) {
            return symbol.color(NamedTextColor.DARK_GRAY)
                .hoverEvent(HoverEvent.showText(MsgUtil.parse("<red>Failed to load claim data</red><newline>"
                    + locationHint)));
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

    private ParsedMapArgs parseArgs(final List<String> args) {
        String mode = "";
        Integer size = null;
        for (final String rawArg : args) {
            final String arg = rawArg.toLowerCase();
            if (arg.startsWith("--size=")) {
                final String value = rawArg.substring("--size=".length()).trim();
                if (value.isEmpty()) {
                    return new ParsedMapArgs(null, null, "<red>Map size is required: <white>--size=<number></white>");
                }
                try {
                    final int parsed = Integer.parseInt(value);
                    if (parsed < 1) {
                        return new ParsedMapArgs(null, null, "<red>Map size must be at least 1.");
                    }
                    size = parsed;
                } catch (NumberFormatException ex) {
                    return new ParsedMapArgs(null, null, "<red>Map size must be a number.");
                }
                continue;
            }
            if (mode.isEmpty()) {
                mode = arg;
            } else {
                return new ParsedMapArgs(null, null, "<red>Usage: /f map [on|off|once] [--size=<size>]");
            }
        }
        return new ParsedMapArgs(mode, size, null);
    }

    private static final class ParsedMapArgs {
        private final String mode;
        private final Integer size;
        private final String error;

        private ParsedMapArgs(final String mode, final Integer size, final String error) {
            this.mode = mode;
            this.size = size;
            this.error = error;
        }
    }
}
