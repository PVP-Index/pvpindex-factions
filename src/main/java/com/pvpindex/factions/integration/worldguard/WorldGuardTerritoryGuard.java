package com.pvpindex.factions.integration.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * WorldGuard-backed territory guard.
 *
 * <p>When {@code syncEnabled} is true, faction claim chunks are mirrored as WG
 * {@code ProtectedCuboidRegion}s by {@link WorldGuardRegionSync}, allowing WG to
 * handle build protection natively. The {@link #isFactionRegion(Location)} and
 * {@link #syncsBuildProtection()} methods are used by the engine to skip redundant
 * database queries on protected territory.
 */
public final class WorldGuardTerritoryGuard implements TerritoryGuard {

    private final boolean syncEnabled;

    public WorldGuardTerritoryGuard(final boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }

    @Override
    public boolean canModifyTerritory(final Player player, final Location location) {
        try {
            final LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            final RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            return query.testBuild(BukkitAdapter.adapt(location), localPlayer);
        } catch (Exception ignored) {
            return true;
        }
    }

    @Override
    public boolean syncsBuildProtection() {
        return syncEnabled;
    }

    /**
     * Returns true when the chunk at this location has a synced WG faction region.
     * Uses WG's in-memory region store — no database access.
     */
    @Override
    public boolean isFactionRegion(final Location location) {
        if (location.getWorld() == null) {
            return false;
        }
        try {
            final RegionManager rm = WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(BukkitAdapter.adapt(location.getWorld()));
            if (rm == null) {
                return false;
            }
            final String regionId = regionName(
                location.getWorld().getName(),
                location.getChunk().getX(),
                location.getChunk().getZ());
            return rm.getRegion(regionId) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Derives the WG region name for a faction claim at the given chunk coordinates.
     *
     * <p>Produces names in the form {@code f_<world>_<x>_<z>} using only
     * {@code [a-z0-9_]} characters. Negative coordinates are prefixed with {@code n}.
     */
    public static String regionName(final String world, final int chunkX, final int chunkZ) {
        final String w = world.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "_");
        final String x = chunkX < 0 ? "n" + (-chunkX) : String.valueOf(chunkX);
        final String z = chunkZ < 0 ? "n" + (-chunkZ) : String.valueOf(chunkZ);
        return "f_" + w + "_" + x + "_" + z;
    }
}

