package com.gyvex.pvpindex.factions.command.sub;

import com.gyvex.pvpindex.factions.command.CommandContext;
import com.gyvex.pvpindex.factions.command.CommandGuards;
import com.gyvex.pvpindex.factions.command.FactionCommand;
import com.gyvex.pvpindex.factions.data.model.AutoTerritoryMode;
import com.gyvex.pvpindex.factions.data.model.BoardEntry;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.engine.AutoTerritoryModeCache;
import com.gyvex.pvpindex.factions.engine.EngineChunkChange;
import com.gyvex.pvpindex.factions.service.FactionService;
import com.gyvex.pvpindex.factions.util.MsgUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.bukkit.entity.Player;

/** {@code /f unclaim} — Unclaim the chunk you are standing in. */
public final class CmdUnclaim extends FactionCommand {

    private final EngineChunkChange engineChunkChange;
    private final FactionService factionService;
    private final AutoTerritoryModeCache autoModeCache;

    public CmdUnclaim(
            final EngineChunkChange engineChunkChange,
            final FactionService factionService,
            final AutoTerritoryModeCache autoModeCache) {
        super("unclaim");
        setPermission("factions.cmd.unclaim");
        setDescription("Unclaim the chunk you are standing in.");
        setOptionalArgs("[one|auto|square|circle|fill|all]");
        setRequiresPlayer(true);
        this.engineChunkChange = engineChunkChange;
        this.factionService = factionService;
        this.autoModeCache = autoModeCache;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        if (ctx.getArgs().isEmpty() || "one".equalsIgnoreCase(ctx.arg(0))) {
            if (engineChunkChange.unclaim(player, player.getLocation().getChunk())) {
                MsgUtil.send(player, "<yellow>Chunk unclaimed.");
            }
            return;
        }
        final String mode = ctx.arg(0).toLowerCase();
        if ("auto".equals(mode)) {
            final String desired = ctx.arg(1).toLowerCase();
            final boolean enabled = desired.isBlank()
                ? autoModeCache.getMode(player.getUniqueId()) != AutoTerritoryMode.UNCLAIM
                : "on".equals(desired);
            final AutoTerritoryMode newMode = enabled ? AutoTerritoryMode.UNCLAIM : AutoTerritoryMode.OFF;
            if (!autoModeCache.setMode(player.getUniqueId(), newMode)) {
                MsgUtil.send(player, "<red>Failed to persist auto-unclaim preference.");
                return;
            }
            MsgUtil.send(player, enabled
                ? "<green>Auto-unclaim enabled (basic mode)."
                : "<yellow>Auto-unclaim disabled.");
            return;
        }
        if ("all".equals(mode)) {
            if (!CommandGuards.requireOwner(player, factionService)) {
                return;
            }
            if (!"confirm".equalsIgnoreCase(ctx.arg(1))) {
                MsgUtil.send(player, "<red>Use /f unclaim all confirm to unclaim all faction land.");
                return;
            }
            final Optional<FactionModel> faction = factionService.getFactionByPlayer(player.getUniqueId());
            if (faction.isEmpty()) {
                MsgUtil.send(player, "<red>You are not in a faction.");
                return;
            }
            final List<BoardEntry> claims;
            try {
                claims = ctx.getRepos().board().findByFactionId(faction.get().getId());
            } catch (Exception e) {
                MsgUtil.send(player, "<red>Failed to load claims.");
                return;
            }
            int removed = 0;
            for (final BoardEntry claim : claims) {
                final org.bukkit.Chunk chunk = player.getWorld().getChunkAt(claim.getChunkX(), claim.getChunkZ());
                if (engineChunkChange.unclaim(player, chunk)) {
                    removed++;
                }
            }
            MsgUtil.send(player, "<yellow>Unclaimed <white>" + removed + "<yellow> chunk(s).");
            return;
        }
        final int max = Math.max(1, ctx.getConfig().getLandMaxPerCommand());
        final org.bukkit.Chunk center = player.getLocation().getChunk();
        final List<org.bukkit.Chunk> chunks = switch (mode) {
            case "square" -> collectSquare(center, parseRadius(ctx.arg(1)), max);
            case "circle" -> collectCircle(center, parseRadius(ctx.arg(1)), max);
            case "fill" -> collectSquare(center, 1, max);
            default -> List.of(center);
        };
        int success = 0;
        for (final org.bukkit.Chunk chunk : chunks) {
            if (engineChunkChange.unclaim(player, chunk)) {
                success++;
            }
        }
        MsgUtil.send(player, "<yellow>Unclaimed <white>" + success + "<yellow> chunk(s).");
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return List.of("one", "auto", "square", "circle", "fill", "all");
        }
        if (argIndex == 1 && "auto".equalsIgnoreCase(ctx.arg(0))) {
            return List.of("on", "off");
        }
        if (argIndex == 1 && "all".equalsIgnoreCase(ctx.arg(0))) {
            return List.of("confirm");
        }
        return List.of();
    }

    private int parseRadius(final String arg) {
        try {
            return Math.max(1, Integer.parseInt(arg));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private List<org.bukkit.Chunk> collectSquare(final org.bukkit.Chunk center, final int radius, final int max) {
        final Set<String> seen = new HashSet<>();
        final java.util.ArrayList<org.bukkit.Chunk> out = new java.util.ArrayList<>();
        for (int dx = -radius; dx <= radius && out.size() < max; dx++) {
            for (int dz = -radius; dz <= radius && out.size() < max; dz++) {
                final org.bukkit.Chunk chunk = center.getWorld().getChunkAt(center.getX() + dx, center.getZ() + dz);
                final String key = chunk.getX() + ":" + chunk.getZ();
                if (seen.add(key)) {
                    out.add(chunk);
                }
            }
        }
        return out;
    }

    private List<org.bukkit.Chunk> collectCircle(final org.bukkit.Chunk center, final int radius, final int max) {
        final Set<String> seen = new HashSet<>();
        final java.util.ArrayList<org.bukkit.Chunk> out = new java.util.ArrayList<>();
        final int r2 = radius * radius;
        for (int dx = -radius; dx <= radius && out.size() < max; dx++) {
            for (int dz = -radius; dz <= radius && out.size() < max; dz++) {
                if ((dx * dx) + (dz * dz) > r2) {
                    continue;
                }
                final org.bukkit.Chunk chunk = center.getWorld().getChunkAt(center.getX() + dx, center.getZ() + dz);
                final String key = chunk.getX() + ":" + chunk.getZ();
                if (seen.add(key)) {
                    out.add(chunk);
                }
            }
        }
        return out;
    }

}
