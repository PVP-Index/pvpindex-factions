package com.pvpindex.factions.engine;

import com.pvpindex.factions.data.model.AutoTerritoryMode;
import com.pvpindex.factions.integration.worldguard.TerritoryGuard;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Applies auto-claim / auto-unclaim modes when players cross chunk borders.
 */
public final class EngineAutoTerritory implements Listener {

    private final EngineChunkChange engineChunkChange;
    private final TerritoryGuard territoryGuard;
    private final AutoTerritoryModeCache modeCache;

    public EngineAutoTerritory(
            final EngineChunkChange engineChunkChange,
            final TerritoryGuard territoryGuard,
            final AutoTerritoryModeCache modeCache) {
        this.engineChunkChange = engineChunkChange;
        this.territoryGuard = territoryGuard;
        this.modeCache = modeCache;
    }

    public void register(final Plugin plugin) {
        org.bukkit.Bukkit.getPluginManager().registerEvents(this, plugin);
        for (final Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            modeCache.hydrate(player.getUniqueId());
        }
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        modeCache.hydrate(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        modeCache.evict(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(final PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }
        final Player player = event.getPlayer();
        final Chunk to = event.getTo().getChunk();

        final AutoTerritoryMode mode = modeCache.getMode(player.getUniqueId());
        if (mode == AutoTerritoryMode.CLAIM) {
            if (territoryGuard.canModifyTerritory(player, event.getTo())) {
                engineChunkChange.claim(player, to);
            }
            return;
        }

        if (mode == AutoTerritoryMode.UNCLAIM) {
            engineChunkChange.unclaim(player, to);
        }
    }
}
