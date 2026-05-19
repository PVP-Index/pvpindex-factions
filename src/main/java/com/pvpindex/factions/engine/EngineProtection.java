package com.pvpindex.factions.engine;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.FactionFlag;
import com.pvpindex.factions.Relation;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.BoardEntry;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.integration.worldguard.TerritoryGuard;
import com.pvpindex.factions.service.FlagService;
import com.pvpindex.factions.util.MsgUtil;
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
import org.bukkit.event.block.BlockSpreadEvent;
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
    private final FlagService flagService;
    private final TerritoryGuard territoryGuard;
    private final boolean wgSync;
    private final Logger logger;

    public EngineProtection(
            final Repositories repos,
            final FactionsConfig config,
            final FlagService flagService,
            final TerritoryGuard territoryGuard,
            final Logger logger) {
        this.repos = repos;
        this.config = config;
        this.flagService = flagService;
        this.territoryGuard = territoryGuard;
        this.wgSync = territoryGuard != null && territoryGuard.syncsBuildProtection();
        this.logger = logger;
    }

    public void register(final Plugin plugin) {
        org.bukkit.Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        final boolean allowed = wgSync
            ? canModifyWgSync(event.getPlayer(), event.getBlock())
            : canModify(event.getPlayer(), event.getBlock().getChunk());
        if (!allowed) {
            event.setCancelled(true);
            MsgUtil.send(event.getPlayer(), "<red>You may not break blocks in this territory.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final boolean allowed = wgSync
            ? canModifyWgSync(event.getPlayer(), event.getBlock())
            : canModify(event.getPlayer(), event.getBlock().getChunk());
        if (!allowed) {
            event.setCancelled(true);
            MsgUtil.send(event.getPlayer(), "<red>You may not place blocks in this territory.");
        }
    }

    /**
     * WG-sync mode: un-cancel events that WG denied for allies or override-mode admins.
     * Runs after WG (NORMAL) and our own HIGH handler so it only sees WG-cancelled events.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreakAllyUnlock(final BlockBreakEvent event) {
        if (!wgSync || !event.isCancelled()) {
            return;
        }
        if (tryUnlockForAlliedPlayer(event.getPlayer(), event.getBlock())) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlaceAllyUnlock(final BlockPlaceEvent event) {
        if (!wgSync || !event.isCancelled()) {
            return;
        }
        if (tryUnlockForAlliedPlayer(event.getPlayer(), event.getBlock())) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvp(final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)
                || !(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (attacker.hasPermission("factions.bypass")) {
            return;
        }
        try {
            // Friendly-fire check — applies everywhere (not just claimed land)
            if (flagService != null) {
                final Optional<PlayerModel> attackerModel =
                    repos.players().find(attacker.getUniqueId().toString());
                final Optional<PlayerModel> victimModel =
                    repos.players().find(victim.getUniqueId().toString());
                if (attackerModel.isPresent() && victimModel.isPresent()
                        && attackerModel.get().isInFaction()
                        && attackerModel.get().getFactionId().equals(victimModel.get().getFactionId())) {
                    final Optional<FactionModel> factionOpt =
                        repos.factions().find(attackerModel.get().getFactionId());
                    if (factionOpt.isPresent()
                            && !flagService.getFlag(factionOpt.get(), FactionFlag.FRIENDLY_FIRE)) {
                        event.setCancelled(true);
                        MsgUtil.send(attacker, "<red>Friendly fire is disabled in your faction.");
                        return;
                    }
                }
            }

            final org.bukkit.Chunk chunk = event.getEntity().getLocation().getChunk();
            final Optional<BoardEntry> entry = repos.board().findByChunk(
                chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            if (entry.isEmpty()) {
                // Wilderness — PvP follows server rules
                return;
            }
            final String factionId = entry.get().getFactionId();
            if (FactionModel.SAFEZONE_ID.equals(factionId) && config.isSafeZoneEnabled()) {
                event.setCancelled(true);
                MsgUtil.send(attacker, "<red>PvP is disabled in the Safezone.");
                return;
            }
            if (FactionModel.WARZONE_ID.equals(factionId)) {
                // Warzone: PvP always allowed
                return;
            }
            // Regular faction territory — check PvP flag
            if (flagService != null) {
                final Optional<FactionModel> claimOwner = repos.factions().find(factionId);
                if (claimOwner.isPresent()
                        && !flagService.getFlag(claimOwner.get(), FactionFlag.PVP)) {
                    event.setCancelled(true);
                    MsgUtil.send(attacker, "<red>PvP is disabled in this territory.");
                }
            }
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockSpread(final BlockSpreadEvent event) {
        // Cancel fire spread in territory where the fire-spread flag is off
        if (event.getSource().getType() != org.bukkit.Material.FIRE) {
            return;
        }
        try {
            final org.bukkit.Chunk chunk = event.getSource().getChunk();
            final Optional<BoardEntry> entry = repos.board().findByChunk(
                chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            if (entry.isEmpty()) {
                return;
            }
            final String factionId = entry.get().getFactionId();
            if (FactionModel.SAFEZONE_ID.equals(factionId)
                    || FactionModel.WARZONE_ID.equals(factionId)) {
                return;
            }
            if (flagService != null) {
                final Optional<FactionModel> factionOpt = repos.factions().find(factionId);
                if (factionOpt.isPresent()
                        && !flagService.getFlag(factionOpt.get(), FactionFlag.FIRE_SPREAD)) {
                    event.setCancelled(true);
                }
            }
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Failed to check fire-spread protection", e);
        }
    }

    private boolean isProtectedFromExplosion(final Block block) {
        try {
            final org.bukkit.Chunk chunk = block.getChunk();
            final Optional<BoardEntry> entry = repos.board().findByChunk(
                chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            if (entry.isEmpty()) {
                return false;
            }
            final String factionId = entry.get().getFactionId();
            if (FactionModel.SAFEZONE_ID.equals(factionId)
                    || FactionModel.WARZONE_ID.equals(factionId)) {
                return true; // Always protect zones
            }
            if (flagService != null) {
                final Optional<FactionModel> factionOpt = repos.factions().find(factionId);
                if (factionOpt.isPresent()) {
                    // If explosions flag is ON, allow (not protected); if OFF, protect
                    return !flagService.getFlag(factionOpt.get(), FactionFlag.EXPLOSIONS);
                }
            }
            return true; // Default: protect claimed territory
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Failed to check explosion protection", e);
            return true; // Fail-safe: protect on error
        }
    }

    /**
     * Fast-path for WG-sync mode. WG already validated membership at NORMAL priority;
     * by the time this runs (HIGH, ignoreCancelled=true) WG has cancelled enemy blocks.
     * We only do a DB lookup for wilderness (no faction region) and bypass permission.
     */
    private boolean canModifyWgSync(final Player player, final org.bukkit.block.Block block) {
        if (player.hasPermission("factions.bypass")) {
            return true;
        }
        // WG region present → WG already enforced membership; our handler lets it through
        if (territoryGuard.isFactionRegion(block.getLocation())) {
            return true;
        }
        // No WG faction region → wilderness or un-synced; fall through to DB check
        return canModify(player, block.getChunk());
    }

    /**
     * Returns true if the block event should be un-cancelled for the player.
     * Used by the HIGHEST handlers in WG-sync mode to restore access for allies and
     * override-mode admins who were denied by WG at NORMAL priority.
     */
    private boolean tryUnlockForAlliedPlayer(final Player player,
            final org.bukkit.block.Block block) {
        try {
            if (!territoryGuard.isFactionRegion(block.getLocation())) {
                return false; // not a faction region — let WG decision stand
            }
            if (player.hasPermission("factions.bypass")) {
                return true;
            }
            final Optional<PlayerModel> pm = repos.players().find(player.getUniqueId().toString());
            if (pm.isPresent() && pm.get().isOverriding()) {
                return true;
            }
            if (pm.isEmpty() || !pm.get().isInFaction()) {
                return false;
            }
            final Optional<BoardEntry> entry = repos.board().findByChunk(
                block.getWorld().getName(), block.getChunk().getX(), block.getChunk().getZ());
            if (entry.isEmpty()) {
                return false;
            }
            final String factionId = entry.get().getFactionId();
            if (FactionModel.SAFEZONE_ID.equals(factionId)
                    || FactionModel.WARZONE_ID.equals(factionId)) {
                return false;
            }
            if (factionId.equals(pm.get().getFactionId())) {
                return true; // own faction — un-cancel (shouldn't reach here normally)
            }
            final Optional<FactionModel> playerFaction =
                repos.factions().find(pm.get().getFactionId());
            if (playerFaction.isEmpty()) {
                return false;
            }
            return getRelation(playerFaction.get(), factionId) == Relation.ALLY;
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Failed to evaluate ally unlock", e);
            return false;
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
            if (FactionModel.SAFEZONE_ID.equals(factionId) && config.isSafeZoneEnabled()) {
                return false; // Safezone — no building
            }
            if (FactionModel.WARZONE_ID.equals(factionId) && config.isWarZoneEnabled()) {
                return false; // Warzone — no building
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
