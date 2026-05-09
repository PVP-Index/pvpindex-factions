package com.gyvex.pvpindex.factions.api;

import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.data.model.WarpModel;
import com.gyvex.pvpindex.factions.service.FactionServiceImpl;
import com.gyvex.pvpindex.factions.service.WarpServiceImpl;
import com.skyblockexp.teamsapi.event.TeamWarpDeleteEvent;
import com.skyblockexp.teamsapi.event.TeamWarpSetEvent;
import com.skyblockexp.teamsapi.model.TeamWarp;
import com.skyblockexp.teamsapi.api.TeamsWarpService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
 * Adapts the internal {@link WarpServiceImpl} to the TeamsAPI
 * {@link TeamsWarpService} interface.
 *
 * <p>This class is only instantiated when TeamsAPI is present on the server.
 */
public class FactionsTeamsWarpService implements TeamsWarpService {

    private final WarpServiceImpl impl;
    private final FactionServiceImpl factionImpl;

    public FactionsTeamsWarpService(
            final WarpServiceImpl impl,
            final FactionServiceImpl factionImpl) {
        this.impl = impl;
        this.factionImpl = factionImpl;
    }

    @Override
    public boolean setWarp(
            final UUID teamId,
            final String name,
            final Location location,
            final UUID creatorUUID) {
        final Optional<FactionModel> faction = factionImpl.getFactionById(teamId.toString());
        if (faction.isEmpty() || location.getWorld() == null) {
            return false;
        }
        final FactionTeam team = wrap(faction.get());
        final TeamWarpSetEvent event = new TeamWarpSetEvent(
            team, name, location, creatorUUID != null ? creatorUUID : new UUID(0L, 0L));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        return impl.setWarp(teamId.toString(), name, location, creatorUUID);
    }

    @Override
    public boolean removeWarp(final UUID teamId, final String name) {
        final Optional<FactionModel> faction = factionImpl.getFactionById(teamId.toString());
        if (faction.isPresent()) {
            final FactionTeam team = wrap(faction.get());
            final TeamWarpDeleteEvent event = new TeamWarpDeleteEvent(team, name);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
        }
        return impl.deleteWarp(teamId.toString(), name);
    }

    @Override
    public Optional<TeamWarp> getWarp(final UUID teamId, final String name) {
        return impl.getWarp(teamId.toString(), name).map(FactionTeamWarp::new);
    }

    @Override
    public Collection<TeamWarp> getWarps(final UUID teamId) {
        final List<WarpModel> warpModels = impl.getWarps(teamId.toString());
        final List<TeamWarp> result = new ArrayList<>(warpModels.size());
        for (final WarpModel wm : warpModels) {
            result.add(new FactionTeamWarp(wm));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private FactionTeam wrap(final FactionModel faction) {
        return new FactionTeam(
            faction,
            factionImpl.getRepos(),
            factionImpl.getConfig(),
            factionImpl.getLogger());
    }
}
