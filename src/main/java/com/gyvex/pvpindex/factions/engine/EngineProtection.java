package com.gyvex.pvpindex.factions.engine;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.gyvex.pvpindex.factions.Relation;
import com.gyvex.pvpindex.factions.config.FactionsConfig;
import com.gyvex.pvpindex.factions.data.Repositories;
import com.gyvex.pvpindex.factions.data.model.BoardEntry;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.data.model.PlayerModel;
import com.gyvex.pvpindex.factions.util.MsgUtil;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;

/**
 * Prevents hostile interactions in claimed territory when the attacker has no permission.
 *
 * <p>Rules:
 * <ul>
 *   <li>Safezone: no pvp, no block break/place by non-members.</li>
 *   <li>Warzone: pvp allowed, no block break/place.</li>
 *   <li>Enemy territory: no block break/place.</li>
 *   <li>Own / ally territory: full access.</li>
 * </ul>
 */
public final class EngineProtection implements Listener {

    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;

    public EngineProtection(
            final Repositories repos, final FactionsConfig config, final Logger logger) {
        this.repos = repos;
        this.config = config;
        this.logger = logger;
    }

    public void register(final Plugin plugin) {
        org.bukkit.Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getChunk())) {
            event.setCancelled(true);
            MsgUtil.send(event.getPlayer(), "<red>You may not break blocks in this territory.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        if (!canModify(event.getPlayer(), event.getBlock().getChunk())) {
            event.setCancelled(true);
            MsgUtil.send(event.getPlayer(), "<red>You may not place blocks in this territory.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvp(final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)
                || !(event.getEntity() instanceof Player)) {
            return;
        }
        if (attacker.hasPermission("factions.bypass")) {
            return;
        }
        try {
            final org.bukkit.Chunk chunk = event.getEntity().getLocation().getChunk();
            final Optional<BoardEntry> entry = repos.board().findByChunk(
                chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            if (entry.isEmpty()) {
                // Wilderness — PvP follows server rules
                return;
            }
            final String factionId = entry.get().getFactionId();
            if (FactionModel.SAFEZONE_ID.equals(factionId)) {
                event.setCancelled(true);
                MsgUtil.send(attacker, "<red>PvP is disabled in the Safezone.");
            }
            // Warzone: PvP allowed — no cancel
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Failed to evaluate PvP protection", e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(final EntityExplodeEvent event) {
        event.blockList().removeIf(this::isProtectedFromExplosion);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(final BlockExplodeEvent event) {
        event.blockList().removeIf(this::isProtectedFromExplosion);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean isProtectedFromExplosion(final Block block) {
        try {
            final org.bukkit.Chunk chunk = block.getChunk();
            final Optional<BoardEntry> entry = repos.board().findByChunk(
                chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            return entry.isPresent();
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Failed to check explosion protection", e);
            return true; // Fail-safe: protect on error
        }
    }

    private boolean canModify(final Player player, final org.bukkit.Chunk chunk) {
        try {
            if (player.hasPermission("factions.bypass")) {
                return true;
            }
            final Optional<PlayerModel> pm = repos.players().find(player.getUniqueId().toString());
            if (pm.isPresent() && pm.get().isOverriding()) {
                return true;
            }
            final Optional<BoardEntry> entry = repos.board().findByChunk(
                chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            if (entry.isEmpty()) {
                return true; // Wilderness — allowed
            }
            final String factionId = entry.get().getFactionId();
            if (FactionModel.SAFEZONE_ID.equals(factionId) || FactionModel.WARZONE_ID.equals(factionId)) {
                return false; // System zones — no building
            }

            if (pm.isEmpty() || !pm.get().isInFaction()) {
                return false; // Non-member in claimed territory
            }
            if (factionId.equals(pm.get().getFactionId())) {
                return true; // Own territory
            }

            // Check relation — allies may build, enemies/neutrals may not
            final Optional<FactionModel> claimOwner = repos.factions().find(factionId);
            final Optional<FactionModel> playerFaction = repos.factions().find(pm.get().getFactionId());
            if (claimOwner.isEmpty() || playerFaction.isEmpty()) {
                return false;
            }
            final Relation rel = getRelation(playerFaction.get(), factionId);
            return rel == Relation.ALLY;
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Failed to check build protection", e);
            return false; // Fail-safe: deny on error
        }
    }

    private Relation getRelation(final FactionModel faction, final String otherFactionId) {
        final String relationJson = faction.getRelationsJson();
        if (relationJson == null) {
            return Relation.NEUTRAL;
        }
        final String token = "\"" + otherFactionId + "\":\"";
        final int start = relationJson.indexOf(token);
        if (start < 0) {
            return Relation.NEUTRAL;
        }
        final int valueStart = start + token.length();
        final int valueEnd = relationJson.indexOf('"', valueStart);
        if (valueEnd < 0) {
            return Relation.NEUTRAL;
        }
        try {
            return Relation.valueOf(relationJson.substring(valueStart, valueEnd));
        } catch (IllegalArgumentException e) {
            return Relation.NEUTRAL;
        }
    }
}
