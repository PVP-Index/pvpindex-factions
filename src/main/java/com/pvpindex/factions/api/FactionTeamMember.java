package com.pvpindex.factions.api;

import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.model.RankModel;
import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.model.TeamMember;
import com.skyblockexp.teamsapi.model.TeamRole;
import com.skyblockexp.teamsapi.model.TeamRoleDefinition;
import java.time.Instant;
import java.util.UUID;

/**
 * Adapts a {@link PlayerModel} + {@link RankModel} pair to the TeamsAPI {@link TeamMember} interface.
 */
public final class FactionTeamMember implements TeamMember {

    private final PlayerModel player;
    private final RankModel rank;

    public FactionTeamMember(final PlayerModel player, final RankModel rank) {
        this.player = player;
        this.rank = rank;
    }

    @Override
    public UUID getPlayerUUID() {
        return UUID.fromString(player.getId());
    }

    @Override
    public TeamRole getRole() {
        return TeamRoleMapper.rankToRole(rank);
    }

    @Override
    public Instant getJoinedAt() {
        return Instant.ofEpochMilli(player.getJoinedAt());
    }

    @Override
    public TeamRoleDefinition getRoleDefinition() {
        if (rank == null) {
            return TeamRoleDefinition.of(getRole());
        }
        return TeamsAPI.getCustomRole(TeamsCustomRoleRegistry.roleKey(
            rank.getFactionId(),
            rank.getId(),
            rank.getName()
        )).orElseGet(() -> TeamRoleDefinition.of(getRole()));
    }
}
