package com.pvpindex.factions.engine;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.BoardEntry;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.util.MsgUtil;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

/**
 * Announces territory transitions when a player crosses chunk borders.
 */
public final class EnginePlayerMove implements Listener {

    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;

    public EnginePlayerMove(
            final Repositories repos, final FactionsConfig config, final Logger logger) {
        this.repos = repos;
        this.config = config;
        this.logger = logger;
    }

    public void register(final Plugin plugin) {
        org.bukkit.Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(final PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }
        final Chunk from = event.getFrom().getChunk();
        final Chunk to = event.getTo().getChunk();
        if (!from.getWorld().equals(to.getWorld())) {
            return;
        }
        try {
            final Optional<PlayerModel> playerModelOpt = repos.players().find(event.getPlayer().getUniqueId().toString());
            if (playerModelOpt.isPresent() && !playerModelOpt.get().hasTerritoryTitles()) {
                return;
            }
            final TerritoryInfo fromInfo = resolveTerritory(from);
            final TerritoryInfo toInfo = resolveTerritory(to);
            if (!fromInfo.id().equals(toInfo.id())) {
                if (toInfo.claimed()) {
                    final String leader = formatLeader(toInfo.faction());
                    final int members = countMembers(toInfo.faction());
                    final int land = repos.board().countByFactionId(toInfo.faction().getId());
                    final double power = computeFactionPower(toInfo.faction());
                    MsgUtil.send(event.getPlayer(), MsgUtil.factionInfoHover(
                        "<gold>You entered claimed faction territory: <white>" + toInfo.name(),
                        toInfo.name(),
                        "<gray>Leader: <white>" + leader,
                        "<gray>Members: <white>" + members,
                        "<gray>Land: <white>" + land,
                        "<gray>Power: <white>" + String.format(java.util.Locale.ROOT, "%.1f", power)));
                } else {
                    MsgUtil.send(event.getPlayer(), "<gray>You entered: <white>" + toInfo.name());
                }
            }
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Failed to check territory for move event", e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private TerritoryInfo resolveTerritory(final Chunk chunk) throws StorageException {
        final Optional<BoardEntry> entry = repos.board().findByChunk(
            chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        if (entry.isEmpty()) {
            return new TerritoryInfo(FactionModel.WILDERNESS_ID, "Wilderness", false, null);
        }
        final String factionId = entry.get().getFactionId();
        if (FactionModel.SAFEZONE_ID.equals(factionId)) {
            return new TerritoryInfo(FactionModel.SAFEZONE_ID, "Safezone", false, null);
        }
        if (FactionModel.WARZONE_ID.equals(factionId)) {
            return new TerritoryInfo(FactionModel.WARZONE_ID, "Warzone", false, null);
        }
        final Optional<FactionModel> faction = repos.factions().find(factionId);
        if (faction.isEmpty()) {
            return new TerritoryInfo(FactionModel.WILDERNESS_ID, "Wilderness", false, null);
        }
        return new TerritoryInfo(faction.get().getId(), faction.get().getName(), true, faction.get());
    }

    private String formatLeader(final FactionModel faction) {
        if (faction.getOwnerId() == null || faction.getOwnerId().isBlank()) {
            return "Unknown";
        }
        try {
            final org.bukkit.OfflinePlayer owner = org.bukkit.Bukkit.getOfflinePlayer(
                java.util.UUID.fromString(faction.getOwnerId()));
            return owner.getName() == null ? faction.getOwnerId() : owner.getName();
        } catch (IllegalArgumentException ignored) {
            return faction.getOwnerId();
        }
    }

    private int countMembers(final FactionModel faction) throws StorageException {
        return repos.players().findByFactionId(faction.getId()).size();
    }

    private double computeFactionPower(final FactionModel faction) throws StorageException {
        double total = faction.getPowerBoost();
        for (final PlayerModel member : repos.players().findByFactionId(faction.getId())) {
            total += member.getPower();
        }
        return total;
    }

    private record TerritoryInfo(String id, String name, boolean claimed, FactionModel faction) {
    }
}
