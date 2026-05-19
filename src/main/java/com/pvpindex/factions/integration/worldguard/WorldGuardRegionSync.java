package com.pvpindex.factions.integration.worldguard;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.BoardEntry;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.event.FactionChunkClaimEvent;
import com.pvpindex.factions.event.FactionChunkUnclaimEvent;
import com.pvpindex.factions.event.FactionDisbandEvent;
import com.pvpindex.factions.event.FactionJoinEvent;
import com.pvpindex.factions.event.FactionLeaveEvent;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Keeps WorldGuard {@code ProtectedCuboidRegion}s in sync with PvPIndex faction claims,
 * so that WG handles build protection natively without per-event database queries.
 *
 * <p>Region naming: {@code f_<world>_<chunkX>_<chunkZ>} — see
 * {@link WorldGuardTerritoryGuard#regionName(String, int, int)}.
 *
 * <p>Registration and startup sync are performed by {@code EnginesBootstrapComponent}
 * when {@code integrations.worldguard-sync-regions} is enabled in config.
 */
public final class WorldGuardRegionSync implements Listener {

    private final Repositories repos;
    private final Plugin plugin;
    private final Logger logger;

    public WorldGuardRegionSync(
            final Repositories repos,
            final Plugin plugin,
            final Logger logger) {
        this.repos = repos;
        this.plugin = plugin;
        this.logger = logger;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // -------------------------------------------------------------------------
    // Startup sync
    // -------------------------------------------------------------------------

    /**
     * Creates or updates WG regions for every currently-claimed chunk.
     * Saves each world's region manager once after processing all claims in that world.
     * Called synchronously during plugin startup; on large servers this may take a moment.
     */
    public void syncAll() {
        int created = 0;
        int failed = 0;
        try {
            final List<FactionModel> factions = repos.factions().findAll();
            for (final FactionModel faction : factions) {
                final List<BoardEntry> claims = repos.board().findByFactionId(faction.getId());
                if (claims.isEmpty()) {
                    continue;
                }
                final List<UUID> memberUuids = memberUuidsForFaction(faction.getId());
                for (final BoardEntry claim : claims) {
                    final World world = Bukkit.getWorld(claim.getWorldName());
                    if (world == null) {
                        continue;
                    }
                    final RegionManager rm = getRegionManager(world);
                    if (rm == null) {
                        continue;
                    }
                    addOrUpdateRegion(rm, world, claim.getWorldName(),
                        claim.getChunkX(), claim.getChunkZ(), memberUuids);
                    created++;
                }
            }
            // Save each affected world's region manager once
            saveAllWorlds();
        } catch (StorageException e) {
            failed++;
            logger.log(Level.WARNING, "WG sync: startup sync failed", e);
        }
        logger.info("WorldGuard region sync: " + created + " regions created/updated"
            + (failed > 0 ? ", " + failed + " error(s)" : "") + ".");
    }

    // -------------------------------------------------------------------------
    // Claim / unclaim events
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkClaim(final FactionChunkClaimEvent event) {
        try {
            final List<UUID> memberUuids = memberUuidsForFaction(event.getFaction().getId());
            final World world = Bukkit.getWorld(event.getWorldName());
            if (world == null) {
                return;
            }
            final RegionManager rm = getRegionManager(world);
            if (rm == null) {
                return;
            }
            addOrUpdateRegion(rm, world, event.getWorldName(),
                event.getChunkX(), event.getChunkZ(), memberUuids);
            saveAsync(rm, event.getWorldName());
        } catch (StorageException e) {
            logger.log(Level.WARNING,
                "WG sync: failed to create region on claim for faction "
                    + event.getFaction().getId(), e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnclaim(final FactionChunkUnclaimEvent event) {
        final World world = Bukkit.getWorld(event.getWorldName());
        if (world == null) {
            return;
        }
        final RegionManager rm = getRegionManager(world);
        if (rm == null) {
            return;
        }
        final String regionId = WorldGuardTerritoryGuard.regionName(
            event.getWorldName(), event.getChunkX(), event.getChunkZ());
        if (rm.getRegion(regionId) != null) {
            rm.removeRegion(regionId);
            saveAsync(rm, event.getWorldName());
        }
    }

    // -------------------------------------------------------------------------
    // Member events
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFactionJoin(final FactionJoinEvent event) {
        updateMemberInFactionRegions(event.getFaction().getId(), event.getPlayerUUID(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFactionLeave(final FactionLeaveEvent event) {
        updateMemberInFactionRegions(event.getFaction().getId(), event.getPlayerUUID(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFactionDisband(final FactionDisbandEvent event) {
        removeAllRegionsForFaction(event.getFaction().getId());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void addOrUpdateRegion(
            final RegionManager rm,
            final World world,
            final String worldName,
            final int chunkX,
            final int chunkZ,
            final List<UUID> memberUuids) {
        final String regionId = WorldGuardTerritoryGuard.regionName(worldName, chunkX, chunkZ);
        final int minX = chunkX << 4;
        final int minZ = chunkZ << 4;
        final BlockVector3 min = BlockVector3.at(minX, world.getMinHeight(), minZ);
        final BlockVector3 max = BlockVector3.at(minX + 15, world.getMaxHeight() - 1, minZ + 15);
        final ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionId, min, max);
        final DefaultDomain domain = new DefaultDomain();
        for (final UUID uuid : memberUuids) {
            domain.addPlayer(uuid);
        }
        region.setMembers(domain);
        rm.addRegion(region);
    }

    private void updateMemberInFactionRegions(
            final String factionId,
            final UUID playerUUID,
            final boolean add) {
        try {
            final List<BoardEntry> claims = repos.board().findByFactionId(factionId);
            final Set<String> affectedWorlds = new HashSet<>();
            for (final BoardEntry claim : claims) {
                final World world = Bukkit.getWorld(claim.getWorldName());
                if (world == null) {
                    continue;
                }
                final RegionManager rm = getRegionManager(world);
                if (rm == null) {
                    continue;
                }
                final String regionId = WorldGuardTerritoryGuard.regionName(
                    claim.getWorldName(), claim.getChunkX(), claim.getChunkZ());
                final ProtectedRegion region = rm.getRegion(regionId);
                if (region == null) {
                    continue;
                }
                if (add) {
                    region.getMembers().addPlayer(playerUUID);
                } else {
                    region.getMembers().removePlayer(playerUUID);
                }
                affectedWorlds.add(claim.getWorldName());
            }
            for (final String worldName : affectedWorlds) {
                final World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    continue;
                }
                final RegionManager rm = getRegionManager(world);
                if (rm != null) {
                    saveAsync(rm, worldName);
                }
            }
        } catch (StorageException e) {
            logger.log(Level.WARNING,
                "WG sync: failed to update member " + playerUUID + " in faction " + factionId, e);
        }
    }

    private void removeAllRegionsForFaction(final String factionId) {
        try {
            final List<BoardEntry> claims = repos.board().findByFactionId(factionId);
            final Set<String> affectedWorlds = new HashSet<>();
            for (final BoardEntry claim : claims) {
                final World world = Bukkit.getWorld(claim.getWorldName());
                if (world == null) {
                    continue;
                }
                final RegionManager rm = getRegionManager(world);
                if (rm == null) {
                    continue;
                }
                final String regionId = WorldGuardTerritoryGuard.regionName(
                    claim.getWorldName(), claim.getChunkX(), claim.getChunkZ());
                rm.removeRegion(regionId);
                affectedWorlds.add(claim.getWorldName());
            }
            for (final String worldName : affectedWorlds) {
                final World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    continue;
                }
                final RegionManager rm = getRegionManager(world);
                if (rm != null) {
                    saveAsync(rm, worldName);
                }
            }
        } catch (StorageException e) {
            logger.log(Level.WARNING,
                "WG sync: failed to remove regions for disbanded faction " + factionId, e);
        }
    }

    private List<UUID> memberUuidsForFaction(final String factionId) throws StorageException {
        if (FactionModel.SAFEZONE_ID.equals(factionId)
                || FactionModel.WARZONE_ID.equals(factionId)) {
            return List.of(); // no members — everyone denied from building
        }
        return repos.players().findByFactionId(factionId).stream()
            .map(PlayerModel::getId)
            .map(id -> {
                try {
                    return UUID.fromString(id);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            })
            .filter(uuid -> uuid != null)
            .toList();
    }

    private void saveAllWorlds() {
        for (final World world : Bukkit.getWorlds()) {
            final RegionManager rm = getRegionManager(world);
            if (rm != null) {
                try {
                    rm.save();
                } catch (Exception e) {
                    logger.log(Level.WARNING,
                        "WG sync: failed to save regions for world " + world.getName(), e);
                }
            }
        }
    }

    private void saveAsync(final RegionManager rm, final String worldName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                rm.save();
            } catch (Exception e) {
                logger.log(Level.WARNING,
                    "WG sync: async save failed for world " + worldName, e);
            }
        });
    }

    private RegionManager getRegionManager(final World world) {
        try {
            return WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(BukkitAdapter.adapt(world));
        } catch (Exception ignored) {
            return null;
        }
    }
}
