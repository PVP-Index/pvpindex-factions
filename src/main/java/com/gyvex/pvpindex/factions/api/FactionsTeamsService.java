package com.gyvex.pvpindex.factions.api;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.gyvex.pvpindex.factions.data.Repositories;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.data.model.PlayerModel;
import com.gyvex.pvpindex.factions.data.model.RankModel;
import com.gyvex.pvpindex.factions.service.FactionServiceImpl;
import com.skyblockexp.teamsapi.event.TeamCreateEvent;
import com.skyblockexp.teamsapi.event.TeamDeleteEvent;
import com.skyblockexp.teamsapi.event.TeamJoinEvent;
import com.skyblockexp.teamsapi.event.TeamLeaveEvent;
import com.skyblockexp.teamsapi.event.TeamRoleChangeEvent;
import com.skyblockexp.teamsapi.model.Team;
import com.skyblockexp.teamsapi.model.TeamMember;
import com.skyblockexp.teamsapi.model.TeamRole;
import com.skyblockexp.teamsapi.api.TeamsService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;

/**
 * Adapts the internal {@link FactionServiceImpl} to the TeamsAPI {@link TeamsService}
 * interface.
 *
 * <p>Mutation methods delegate to {@link FactionServiceImpl} (which fires internal
 * faction events) and then additionally fire the corresponding TeamsAPI events so
 * that other plugins using TeamsAPI are notified.
 *
 * <p>This class is only instantiated when TeamsAPI is present on the server.
 */
public class FactionsTeamsService implements TeamsService {

    private final FactionServiceImpl impl;
    private final Repositories repos;
    private final Logger logger;

    public FactionsTeamsService(final FactionServiceImpl impl) {
        this.impl = impl;
        this.repos = impl.getRepos();
        this.logger = impl.getLogger();
    }

    // -------------------------------------------------------------------------
    // Simple delegation — read-only queries
    // -------------------------------------------------------------------------

    @Override
    public boolean hasTeam(final UUID playerUUID) {
        return impl.isInFaction(playerUUID);
    }

    @Override
    public boolean teamExists(final String name) {
        return impl.getFactionByName(name).isPresent();
    }

    @Override
    public boolean isMember(final UUID teamId, final UUID playerUUID) {
        try {
            final Optional<PlayerModel> pm = repos.players().find(playerUUID.toString());
            return pm.isPresent() && teamId.toString().equals(pm.get().getFactionId());
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to check isMember", e);
            return false;
        }
    }

    @Override
    public Optional<Team> getPlayerTeam(final UUID playerUUID) {
        return impl.getFactionByPlayer(playerUUID).map(this::wrap);
    }

    @Override
    public Optional<Team> getTeamByName(final String name) {
        return impl.getFactionByName(name).map(this::wrap);
    }

    @Override
    public Optional<Team> getTeam(final UUID teamId) {
        return impl.getFactionById(teamId.toString()).map(this::wrap);
    }

    @Override
    public Collection<Team> getAllTeams() {
        final Collection<FactionModel> factions = impl.getAllFactions();
        final List<Team> teams = new ArrayList<>(factions.size());
        for (final FactionModel f : factions) {
            teams.add(wrap(f));
        }
        return teams;
    }

    @Override
    public int getTeamCount() {
        try {
            return repos.factions().countAll();
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to count factions", e);
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // Mutations — delegate to impl then fire TeamsAPI events
    // -------------------------------------------------------------------------

    @Override
    public Optional<Team> createTeam(final String name, final UUID ownerUUID) {
        // impl fires FactionCreateEvent then returns the saved model
        final Optional<FactionModel> result = impl.createFaction(name, ownerUUID);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        final FactionTeam team = wrap(result.get());
        Bukkit.getPluginManager().callEvent(new TeamCreateEvent(team, ownerUUID));
        return Optional.of(team);
    }

    @Override
    public boolean deleteTeam(final UUID teamId) {
        // Fetch before deletion so we can wrap for the TeamsAPI event
        final Optional<FactionModel> faction = impl.getFactionById(teamId.toString());
        if (faction.isEmpty()) {
            return false;
        }
        final FactionTeam team = wrap(faction.get());
        // impl fires FactionDisbandEvent then deletes all data
        final boolean deleted = impl.disbandFaction(teamId.toString());
        if (deleted) {
            Bukkit.getPluginManager().callEvent(new TeamDeleteEvent(team));
        }
        return deleted;
    }

    @Override
    public boolean addMember(final UUID teamId, final UUID playerUUID, final TeamRole role) {
        // impl fires FactionJoinEvent
        final boolean joined = impl.joinFaction(teamId.toString(), playerUUID);
        if (joined) {
            final Optional<FactionModel> faction = impl.getFactionById(teamId.toString());
            faction.ifPresent(f ->
                Bukkit.getPluginManager().callEvent(
                    new TeamJoinEvent(wrap(f), playerUUID, role)));
        }
        return joined;
    }

    @Override
    public boolean removeMember(final UUID teamId, final UUID playerUUID) {
        try {
            final Optional<FactionModel> faction = impl.getFactionById(teamId.toString());
            if (faction.isEmpty()) {
                return false;
            }
            final Optional<PlayerModel> pm = repos.players().find(playerUUID.toString());
            if (pm.isEmpty() || !teamId.toString().equals(pm.get().getFactionId())) {
                return false;
            }
            final RankModel rank = pm.get().getRankId() != null
                ? repos.ranks().find(pm.get().getRankId()).orElse(null)
                : null;
            final FactionTeam team = wrap(faction.get());
            final FactionTeamMember member = new FactionTeamMember(pm.get(), rank);

            // impl fires FactionLeaveEvent
            final boolean removed = impl.removeMember(teamId.toString(), playerUUID);
            if (removed) {
                Bukkit.getPluginManager().callEvent(
                    new TeamLeaveEvent(team, playerUUID, member.getRole()));
            }
            return removed;
        } catch (StorageException e) {
            logger.log(Level.SEVERE,
                "Failed to remove member " + playerUUID + " from " + teamId, e);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // TeamsAPI-specific membership operations (no internal-service equivalent)
    // -------------------------------------------------------------------------

    @Override
    public boolean setMemberRole(final UUID teamId, final UUID playerUUID, final TeamRole role) {
        try {
            final Optional<PlayerModel> pm = repos.players().find(playerUUID.toString());
            if (pm.isEmpty() || !teamId.toString().equals(pm.get().getFactionId())) {
                return false;
            }

            final String factionId = teamId.toString();
            final List<RankModel> ranks = repos.ranks().findByFactionId(factionId);

            final int targetPriority = FactionTeam.roleToPriority(role);
            final RankModel targetRank = ranks.stream()
                .filter(r -> r.getPriority() == targetPriority)
                .findFirst()
                .orElse(null);

            if (targetRank == null) {
                return false;
            }

            final TeamRole oldRole = getMemberRole(teamId, playerUUID).orElse(TeamRole.MEMBER);
            pm.get().setRankId(targetRank.getId());
            repos.players().save(pm.get());

            final FactionTeam team = wrap(repos.factions().find(factionId).orElseThrow());
            Bukkit.getPluginManager().callEvent(
                new TeamRoleChangeEvent(team, playerUUID, oldRole, role));
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE,
                "Failed to set role for " + playerUUID + " in " + teamId, e);
            return false;
        }
    }

    @Override
    public Optional<TeamRole> getMemberRole(final UUID teamId, final UUID playerUUID) {
        try {
            final Optional<PlayerModel> pm = repos.players().find(playerUUID.toString());
            if (pm.isEmpty() || !teamId.toString().equals(pm.get().getFactionId())) {
                return Optional.empty();
            }
            if (pm.get().getRankId() == null) {
                return Optional.of(TeamRole.MEMBER);
            }
            final Optional<RankModel> rank = repos.ranks().find(pm.get().getRankId());
            if (rank.isEmpty()) {
                return Optional.of(TeamRole.MEMBER);
            }
            final int priority = rank.get().getPriority();
            if (priority >= RankModel.PRIORITY_OWNER) {
                return Optional.of(TeamRole.OWNER);
            } else if (priority >= RankModel.PRIORITY_OFFICER) {
                return Optional.of(TeamRole.ADMIN);
            }
            return Optional.of(TeamRole.MEMBER);
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to get member role", e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<TeamMember> getMemberInfo(final UUID teamId, final UUID playerUUID) {
        try {
            final Optional<PlayerModel> pm = repos.players().find(playerUUID.toString());
            if (pm.isEmpty() || !teamId.toString().equals(pm.get().getFactionId())) {
                return Optional.empty();
            }
            final RankModel rank = pm.get().getRankId() != null
                ? repos.ranks().find(pm.get().getRankId()).orElse(null)
                : null;
            return Optional.of(new FactionTeamMember(pm.get(), rank));
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to get member info", e);
            return Optional.empty();
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private FactionTeam wrap(final FactionModel faction) {
        return new FactionTeam(faction, repos, impl.getConfig(), logger);
    }
}
