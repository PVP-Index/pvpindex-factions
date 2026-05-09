package com.gyvex.pvpindex.factions.integration.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * WorldGuard-backed territory guard.
 */
public final class WorldGuardTerritoryGuard implements TerritoryGuard {

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
}

