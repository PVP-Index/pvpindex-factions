package com.pvpindex.factions.api;

import com.pvpindex.factions.data.model.RankModel;
import com.skyblockexp.teamsapi.model.TeamRole;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

    /**
     * Resolve the best internal rank for a TeamsAPI built-in role.
     *
     * <p>Uses tier-based matching so custom rank priorities still map correctly:
     * OWNER -> highest rank in the faction
     * ADMIN -> highest rank below owner-threshold and above/equal officer-threshold
     * MEMBER -> lowest rank in the faction
     */
    public static Optional<RankModel> resolveRankForRole(final List<RankModel> ranks, final TeamRole role) {
        if (ranks == null || ranks.isEmpty()) {
            return Optional.empty();
        }
        return switch (role) {
            case OWNER -> ranks.stream().max(Comparator.comparingInt(RankModel::getPriority));
            case ADMIN -> ranks.stream()
                .filter(r -> r.getPriority() < RankModel.PRIORITY_OWNER)
                .filter(r -> r.getPriority() >= RankModel.PRIORITY_OFFICER)
                .max(Comparator.comparingInt(RankModel::getPriority))
                .or(() -> ranks.stream()
                    .filter(r -> r.getPriority() < RankModel.PRIORITY_OWNER)
                    .max(Comparator.comparingInt(RankModel::getPriority)));
            case MEMBER -> ranks.stream().min(Comparator.comparingInt(RankModel::getPriority));
        };
    }
}
