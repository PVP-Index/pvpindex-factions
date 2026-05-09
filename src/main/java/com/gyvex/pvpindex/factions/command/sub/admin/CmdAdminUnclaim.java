package com.gyvex.pvpindex.factions.command.sub.admin;

import com.gyvex.pvpindex.factions.command.CommandContext;
import com.gyvex.pvpindex.factions.command.FactionCommand;
import com.gyvex.pvpindex.factions.data.model.BoardEntry;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.util.MsgUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

/** {@code /fa unclaim <faction> [all|one|square|circle|fill] [radius]}. */
public final class CmdAdminUnclaim extends FactionCommand {

    public CmdAdminUnclaim() {
        super("unclaim");
        setPermission("factions.cmd.claim.other");
        setDescription("Unclaim land for another faction.");
        setRequiredArgs("<faction>");
        setOptionalArgs("[all|one|square|circle|fill]", "[radius]");
        setRequiresPlayer(true);
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> faction;
        try {
            faction = ctx.getRepos().factions().findByName(ctx.arg(0));
        } catch (Exception e) {
            MsgUtil.send(player, "<red>Failed to load faction.");
            return;
        }
        if (faction.isEmpty()) {
            MsgUtil.send(player, "<red>Faction not found.");
            return;
        }
        final String mode = ctx.arg(1).isBlank() ? "one" : ctx.arg(1).toLowerCase();
        if ("all".equals(mode)) {
            try {
                final List<BoardEntry> claims = ctx.getRepos().board().findByFactionId(faction.get().getId());
                for (final BoardEntry claim : claims) {
                    ctx.getRepos().board().unclaimChunk(claim.getWorldName(), claim.getChunkX(), claim.getChunkZ());
                }
                MsgUtil.send(player, "<yellow>Admin-unclaimed all <white>" + claims.size()
                    + "<yellow> chunks for <white>" + faction.get().getName() + "<yellow>.");
                return;
            } catch (Exception e) {
                MsgUtil.send(player, "<red>Failed to unclaim all faction land.");
                return;
            }
        }
        final int radius = parseRadius(ctx.arg(2));
        final int max = Math.max(1, ctx.getConfig().getLandMaxPerCommand());
        final Chunk center = player.getLocation().getChunk();
        final List<Chunk> targets = switch (mode) {
            case "square" -> collectSquare(center, radius, max);
            case "circle" -> collectCircle(center, radius, max);
            case "fill" -> collectSquare(center, 1, max);
            default -> List.of(center);
        };
        int removed = 0;
        for (final Chunk chunk : targets) {
            try {
                final Optional<BoardEntry> entry =
                    ctx.getRepos().board().findByChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                if (entry.isPresent() && faction.get().getId().equals(entry.get().getFactionId())) {
                    ctx.getRepos().board().unclaimChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                    removed++;
                }
            } catch (Exception ignored) {
                // Keep processing remaining chunks.
            }
        }
        MsgUtil.send(player, "<yellow>Admin-unclaimed <white>" + removed + "<yellow> chunk(s) for <white>"
            + faction.get().getName() + "<yellow>.");
    }

    private int parseRadius(final String value) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 1;
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
