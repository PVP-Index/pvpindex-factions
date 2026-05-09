package com.gyvex.pvpindex.factions.api;

import com.gyvex.pvpindex.factions.data.model.PlayerModel;
import com.gyvex.pvpindex.factions.data.model.RankModel;
import com.skyblockexp.teamsapi.model.TeamMember;
import com.skyblockexp.teamsapi.model.TeamRole;
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
        if (rank == null) {
            return TeamRole.MEMBER;
        }
        final int priority = rank.getPriority();
        if (priority >= RankModel.PRIORITY_OWNER) {
            return TeamRole.OWNER;
        } else if (priority >= RankModel.PRIORITY_OFFICER) {
            return TeamRole.ADMIN;
        }
        return TeamRole.MEMBER;
    }

    @Override
    public Instant getJoinedAt() {
        return Instant.ofEpochMilli(player.getJoinedAt());
    }
}
