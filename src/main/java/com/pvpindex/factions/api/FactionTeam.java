package com.pvpindex.factions.api;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.model.RankModel;
import com.pvpindex.factions.data.model.FactionModel;
import com.skyblockexp.teamsapi.model.Team;
import com.skyblockexp.teamsapi.model.TeamMember;
import com.skyblockexp.teamsapi.model.TeamRole;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapts a {@link FactionModel} to the TeamsAPI {@link Team} interface.
 *
 * <p>Members are loaded lazily and cached for the lifetime of this adapter instance.
 */
public final class FactionTeam implements Team {

    private final FactionModel model;
    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;

    /** Lazy-loaded and cached member list. */
    private List<TeamMember> membersCache;

    public FactionTeam(
            final FactionModel model,
            final Repositories repos,
            final FactionsConfig config,
            final Logger logger) {
        this.model = model;
        this.repos = repos;
        this.config = config;
        this.logger = logger;
    }

    @Override
    public UUID getId() {
        return UUID.fromString(model.getId());
    }

    @Override
    public String getName() {
        return model.getName();
    }

    @Override
    public String getDisplayName() {
        return model.getName();
    }

    @Override
    public UUID getOwnerUUID() {
        final String ownerId = model.getOwnerId();
        if (ownerId == null) {
            return null;
        }
        try {
            return UUID.fromString(ownerId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public Collection<TeamMember> getMembers() {
        if (membersCache == null) {
            membersCache = loadMembers();
        }
        return membersCache;
    }

    @Override
    public Collection<UUID> getMemberUUIDs() {
        final List<UUID> uuids = new ArrayList<>();
        for (final TeamMember member : getMembers()) {
            uuids.add(member.getPlayerUUID());
        }
        return uuids;
    }

    @Override
    public int getSize() {
        return getMembers().size();
    }

    @Override
    public int getMaxSize() {
        return config.getMaxMembers();
    }

    @Override
    public Optional<TeamMember> getMember(final UUID playerUuid) {
        return getMembers().stream()
            .filter(m -> m.getPlayerUUID().equals(playerUuid))
            .findFirst();
    }

    @Override
    public boolean isMember(final UUID playerUuid) {
        return getMember(playerUuid).isPresent();
    }

    @Override
    public boolean isOwner(final UUID playerUuid) {
        final String ownerId = model.getOwnerId();
        return ownerId != null && ownerId.equals(playerUuid.toString());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private List<TeamMember> loadMembers() {
        final List<TeamMember> result = new ArrayList<>();
        try {
            final List<PlayerModel> playerModels = repos.players().findByFactionId(model.getId());
            for (final PlayerModel pm : playerModels) {
                final RankModel rank = resolveRank(pm);
                result.add(new FactionTeamMember(pm, rank));
            }
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to load members for faction " + model.getId(), e);
        }
        return result;
    }

    private RankModel resolveRank(final PlayerModel player) {
        if (player.getRankId() == null) {
            return null;
        }
        try {
            return repos.ranks().find(player.getRankId()).orElse(null);
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Failed to resolve rank " + player.getRankId(), e);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Package-private: expose the underlying model
    // -------------------------------------------------------------------------

    FactionModel getModel() {
        return model;
    }

    /** Invalidate the member cache (call after membership changes). */
    public void invalidateCache() {
        membersCache = null;
    }

    // -------------------------------------------------------------------------
    // TeamRole helper
    // -------------------------------------------------------------------------

    /**
     * Convert a {@link TeamRole} to the rank priority threshold used by this plugin.
     *
     * @param role TeamsAPI role
     * @return priority value
     */
    public static int roleToPriority(final TeamRole role) {
        return switch (role) {
            case OWNER -> RankModel.PRIORITY_OWNER;
            case ADMIN -> RankModel.PRIORITY_OFFICER;
            case MEMBER -> RankModel.PRIORITY_MEMBER;
        };
    }
}
