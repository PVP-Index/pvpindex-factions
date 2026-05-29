package com.pvpindex.factions.api;

import com.pvpindex.factions.data.model.RankModel;
import com.skyblockexp.teamsapi.model.TeamRole;

/**
 * Maps PvPIndex ranks to TeamsAPI roles.
 */
public final class TeamRoleMapper {

    private TeamRoleMapper() {
    }

    public static int roleToPriority(final TeamRole role) {
        return switch (role) {
            case OWNER -> RankModel.PRIORITY_OWNER;
            case ADMIN -> RankModel.PRIORITY_OFFICER;
            case MEMBER -> RankModel.PRIORITY_MEMBER;
        };
    }

    public static TeamRole rankToRole(final RankModel rank) {
        if (rank == null) {
            return TeamRole.MEMBER;
        }
        if (rank.getPriority() >= RankModel.PRIORITY_OWNER) {
            return TeamRole.OWNER;
        }
        if (rank.getPriority() >= RankModel.PRIORITY_OFFICER) {
            return TeamRole.ADMIN;
        }
        return TeamRole.MEMBER;
    }
}
