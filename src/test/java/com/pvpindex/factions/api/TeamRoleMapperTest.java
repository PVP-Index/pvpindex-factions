package com.pvpindex.factions.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pvpindex.factions.data.model.RankModel;
import com.skyblockexp.teamsapi.model.TeamRole;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TeamRoleMapper rank resolution")
class TeamRoleMapperTest {

    @Test
    @DisplayName("ADMIN resolves to best matching custom mid-tier rank")
    void adminResolvesToBestMidTier() {
        final RankModel owner = rank("r1", 100);
        final RankModel commander = rank("r2", 80);
        final RankModel officer = rank("r3", 50);
        final RankModel member = rank("r4", 10);

        final var resolved = TeamRoleMapper.resolveRankForRole(
            List.of(owner, commander, officer, member),
            TeamRole.ADMIN);

        assertTrue(resolved.isPresent());
        assertEquals("r2", resolved.get().getId());
    }

    @Test
    @DisplayName("MEMBER resolves to lowest priority rank")
    void memberResolvesToLowestPriority() {
        final RankModel scout = rank("r1", 30);
        final RankModel member = rank("r2", 10);

        final var resolved = TeamRoleMapper.resolveRankForRole(
            List.of(scout, member),
            TeamRole.MEMBER);

        assertTrue(resolved.isPresent());
        assertEquals("r2", resolved.get().getId());
    }

    @Test
    @DisplayName("OWNER resolves to highest priority rank")
    void ownerResolvesToHighestPriority() {
        final RankModel owner = rank("r1", 100);
        final RankModel chief = rank("r2", 95);

        final var resolved = TeamRoleMapper.resolveRankForRole(
            List.of(owner, chief),
            TeamRole.OWNER);

        assertTrue(resolved.isPresent());
        assertEquals("r1", resolved.get().getId());
    }

    private RankModel rank(final String id, final int priority) {
        final RankModel rank = new RankModel(id);
        rank.setPriority(priority);
        return rank;
    }
}
