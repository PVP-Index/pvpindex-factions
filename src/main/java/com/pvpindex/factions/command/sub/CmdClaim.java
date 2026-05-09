package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.AutoTerritoryMode;
import com.pvpindex.factions.engine.AutoTerritoryModeCache;
import com.pvpindex.factions.engine.EngineChunkChange;
import com.pvpindex.factions.integration.worldguard.TerritoryGuard;
import com.pvpindex.factions.util.MsgUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** {@code /f claim} — Claim the chunk you are standing in. */
public final class CmdClaim extends FactionCommand {

    private final EngineChunkChange engineChunkChange;
    private final TerritoryGuard territoryGuard;
    private final AutoTerritoryModeCache autoModeCache;

    public CmdClaim(
            final EngineChunkChange engineChunkChange,
            final TerritoryGuard territoryGuard,
            final AutoTerritoryModeCache autoModeCache) {
        super("claim");
        setPermission("factions.cmd.claim");
        setDescription("Claim the chunk you are standing in.");
        setOptionalArgs("[one|auto|square|circle|fill|nearby|at]");
        setRequiresPlayer(true);
        this.engineChunkChange = engineChunkChange;
        this.territoryGuard = territoryGuard;
        this.autoModeCache = autoModeCache;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        if (ctx.getArgs().isEmpty() || "one".equalsIgnoreCase(ctx.arg(0))) {
            if (!territoryGuard.canModifyTerritory(player, player.getLocation())) {
                MsgUtil.send(player, "<red>You cannot claim land in this protected region.");
                return;
            }
            if (engineChunkChange.claim(player, player.getLocation().getChunk())) {
                MsgUtil.send(player, "<green>Chunk claimed!");
            }
            return;
        }
        final String mode = ctx.arg(0).toLowerCase();
        if ("auto".equals(mode)) {
            final String desired = ctx.arg(1).toLowerCase();
            final boolean enabled = desired.isBlank()
                ? autoModeCache.getMode(player.getUniqueId()) != AutoTerritoryMode.CLAIM
                : "on".equals(desired);
            final AutoTerritoryMode newMode = enabled ? AutoTerritoryMode.CLAIM : AutoTerritoryMode.OFF;
            if (!autoModeCache.setMode(player.getUniqueId(), newMode)) {
                MsgUtil.send(player, "<red>Failed to persist auto-claim preference.");
                return;
            }
            MsgUtil.send(player, enabled
                ? "<green>Auto-claim enabled (basic mode)."
                : "<yellow>Auto-claim disabled.");
            return;
        }
        if ("at".equals(mode)) {
            claimAt(ctx, player);
            return;
        }
        final int max = Math.max(1, ctx.getConfig().getLandMaxPerCommand());
        final Chunk center = player.getLocation().getChunk();
        final List<Chunk> chunks = switch (mode) {
            case "square" -> collectSquare(center, parseRadius(ctx.arg(1)), max);
            case "circle" -> collectCircle(center, parseRadius(ctx.arg(1)), max);
            case "fill" -> collectSquare(center, 1, max);
            case "nearby" -> collectSquare(center, parseRadius(ctx.arg(1)), max);
            default -> List.of(center);
        };
        int success = 0;
        for (final Chunk chunk : chunks) {
            if (!territoryGuard.canModifyTerritory(
                    player, chunk.getBlock(8, player.getLocation().getBlockY(), 8).getLocation())) {
                continue;
            }
            if (engineChunkChange.claim(player, chunk)) {
                success++;
            }
        }
        MsgUtil.send(player, "<green>Claimed <white>" + success + "<green> chunk(s).");
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return List.of("one", "auto", "square", "circle", "fill", "nearby", "at");
        }
        if (argIndex == 1 && "auto".equalsIgnoreCase(ctx.arg(0))) {
            return List.of("on", "off");
        }
        if (argIndex == 1 && "at".equalsIgnoreCase(ctx.arg(0))) {
            return List.of(String.valueOf(((Player) ctx.getSender()).getLocation().getChunk().getX()));
        }
        if (argIndex == 2 && "at".equalsIgnoreCase(ctx.arg(0))) {
            return List.of(String.valueOf(((Player) ctx.getSender()).getLocation().getChunk().getZ()));
        }
        return List.of();
    }

    private void claimAt(final CommandContext ctx, final Player player) {
        final int x = parseChunkCoordinate(ctx.arg(1), Integer.MIN_VALUE);
        final int z = parseChunkCoordinate(ctx.arg(2), Integer.MIN_VALUE);
        if (x == Integer.MIN_VALUE || z == Integer.MIN_VALUE) {
            MsgUtil.send(player, "<red>Usage: /f claim at <chunkX> <chunkZ>");
            return;
        }
        final World world = player.getWorld();
        final Chunk target = world.getChunkAt(x, z);
        if (!territoryGuard.canModifyTerritory(
                player, target.getBlock(8, player.getLocation().getBlockY(), 8).getLocation())) {
            MsgUtil.send(player, "<red>You cannot claim land in this protected region.");
            return;
        }
        if (engineChunkChange.claim(player, target)) {
            MsgUtil.send(player, "<green>Chunk claimed at <white>" + x + "<gray>,<white>" + z + "<green>.");
        }
    }

    private int parseRadius(final String arg) {
        try {
            return Math.max(1, Integer.parseInt(arg));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private int parseChunkCoordinate(final String arg, final int fallback) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private List<Chunk> collectSquare(final Chunk center, final int radius, final int max) {
        final Set<String> seen = new HashSet<>();
        final java.util.ArrayList<Chunk> out = new java.util.ArrayList<>();
        for (int dx = -radius; dx <= radius && out.size() < max; dx++) {
            for (int dz = -radius; dz <= radius && out.size() < max; dz++) {
                final Chunk chunk = center.getWorld().getChunkAt(center.getX() + dx, center.getZ() + dz);
                final String key = chunk.getX() + ":" + chunk.getZ();
                if (seen.add(key)) {
                    out.add(chunk);
                }
            }
        }
        return out;
    }

    private List<Chunk> collectCircle(final Chunk center, final int radius, final int max) {
        final Set<String> seen = new HashSet<>();
        final java.util.ArrayList<Chunk> out = new java.util.ArrayList<>();
        final int r2 = radius * radius;
        for (int dx = -radius; dx <= radius && out.size() < max; dx++) {
            for (int dz = -radius; dz <= radius && out.size() < max; dz++) {
                if ((dx * dx) + (dz * dz) > r2) {
                    continue;
                }
                final Chunk chunk = center.getWorld().getChunkAt(center.getX() + dx, center.getZ() + dz);
                final String key = chunk.getX() + ":" + chunk.getZ();
                if (seen.add(key)) {
                    out.add(chunk);
                }
            }
        }
        return out;
    }

}
