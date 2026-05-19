package com.pvpindex.factions.command.sub.admin;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.BoardEntry;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.util.MsgUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

/** {@code /fa warzone [one|square|circle|remove] [radius]}. */
public final class CmdAdminWarzone extends FactionCommand {

    public CmdAdminWarzone() {
        super("warzone");
        setPermission("factions.cmd.warzone");
        setDescription("Assign or remove war zone territory chunks.");
        setOptionalArgs("[one|square|circle|remove]", "[radius]");
        setRequiresPlayer(true);
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final String mode = ctx.arg(0).isBlank() ? "one" : ctx.arg(0).toLowerCase();

        if ("remove".equals(mode)) {
            final Chunk chunk = player.getLocation().getChunk();
            try {
                final Optional<BoardEntry> existing = ctx.getRepos().board().findByChunk(
                    chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                if (existing.isPresent()
                        && FactionModel.WARZONE_ID.equals(existing.get().getFactionId())) {
                    ctx.getRepos().board().unclaimChunk(
                        chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                    MsgUtil.send(player, "<green>Removed war zone from this chunk.");
                } else {
                    MsgUtil.send(player, "<red>This chunk is not a war zone.");
                }
            } catch (Exception e) {
                MsgUtil.send(player, "<red>Failed to remove war zone chunk.");
            }
            return;
        }

        final int radius = parseRadius(ctx.arg(1));
        final int max = Math.max(1, ctx.getConfig().getLandMaxPerCommand());
        final Chunk center = player.getLocation().getChunk();
        final List<Chunk> targets = switch (mode) {
            case "square" -> collectSquare(center, radius, max);
            case "circle" -> collectCircle(center, radius, max);
            default -> List.of(center);
        };

        int assigned = 0;
        for (final Chunk chunk : targets) {
            try {
                ctx.getRepos().board().claimChunk(
                    chunk.getWorld().getName(), chunk.getX(), chunk.getZ(),
                    FactionModel.WARZONE_ID);
                assigned++;
            } catch (Exception ignored) {
                // Keep processing remaining chunks.
            }
        }
        MsgUtil.send(player, "<green>Assigned <white>" + assigned + "<green> chunk(s) as war zone.");
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
