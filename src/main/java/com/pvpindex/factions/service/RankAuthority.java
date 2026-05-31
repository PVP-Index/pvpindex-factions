package com.pvpindex.factions.service;

import com.pvpindex.factions.data.model.RankModel;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Shared rank hierarchy helpers used by service and command layers.
 */
public final class RankAuthority {

    public static final int MIN_CUSTOM_PRIORITY = RankModel.PRIORITY_MEMBER + 1;
    public static final int MAX_CUSTOM_PRIORITY = RankModel.PRIORITY_OWNER - 1;

    private RankAuthority() {
    }

    public static boolean canManage(final RankModel actor, final RankModel target) {
        return actor.getPriority() > target.getPriority();
    }

    public static Optional<RankModel> findByName(final List<RankModel> ranks, final String name) {
        final String normalized = normalizeName(name);
        return ranks.stream().filter(r -> normalizeName(r.getName()).equals(normalized)).findFirst();
    }

    public static boolean isProtectedBuiltin(final String rankName) {
        final String normalized = normalizeName(rankName);
        return normalizeName(RankModel.RANK_OWNER).equals(normalized)
            || normalizeName(RankModel.RANK_OFFICER).equals(normalized)
            || normalizeName(RankModel.RANK_MEMBER).equals(normalized);
    }

    public static String normalizeName(final String rankName) {
        return rankName == null ? "" : rankName.trim().toLowerCase(Locale.ROOT);
    }
}
