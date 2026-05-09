package com.pvpindex.factions.api;

import com.pvpindex.factions.data.model.WarpModel;
import com.skyblockexp.teamsapi.model.TeamWarp;
import java.time.Instant;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Adapts a {@link WarpModel} to the TeamsAPI {@link TeamWarp} interface.
 */
public final class FactionTeamWarp implements TeamWarp {

    private final WarpModel warp;

    public FactionTeamWarp(final WarpModel warp) {
        this.warp = warp;
    }

    @Override
    public UUID getTeamId() {
        return UUID.fromString(warp.getFactionId());
    }

    @Override
    public String getName() {
        return warp.getName();
    }

    @Override
    public Location getLocation() {
        final World world = Bukkit.getWorld(warp.getWorld());
        return new Location(world, warp.getX(), warp.getY(), warp.getZ(),
            warp.getYaw(), warp.getPitch());
    }

    @Override
    public UUID getCreatorUUID() {
        final String creatorId = warp.getCreatorId();
        return creatorId != null ? UUID.fromString(creatorId) : null;
    }

    @Override
    public Instant getCreatedAt() {
        return Instant.ofEpochMilli(warp.getCreatedAt());
    }
}
