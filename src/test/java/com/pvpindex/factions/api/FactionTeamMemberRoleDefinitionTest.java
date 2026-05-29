package com.pvpindex.factions.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.pvpindex.factions.data.model.RankModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.skyblockexp.teamsapi.api.TeamsAPI;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FactionTeamMember role definition mapping")
class FactionTeamMemberRoleDefinitionTest {

    private String customKey;

    @AfterEach
    void cleanup() {
        if (customKey != null) {
            TeamsAPI.unregisterCustomRole(customKey);
        }
    }

    @Test
    @DisplayName("returns registered custom role definition when available")
    void returnsCustomRoleDefinition() {
        final RankModel rank = new RankModel("rank-1");
        rank.setFactionId("f-1");
        rank.setName("Scout");
        rank.setPriority(30);
        rank.setPrefix("<gray>[S]</gray>");
        customKey = TeamsCustomRoleRegistry.roleKey(rank.getFactionId(), rank.getId(), rank.getName());
        TeamsAPI.registerCustomRole(mock(Plugin.class), TeamsCustomRoleRegistry.toRoleDefinition(rank));

        final PlayerModel player = new PlayerModel("00000000-0000-0000-0000-000000000001");
        final FactionTeamMember member = new FactionTeamMember(player, rank);

        assertEquals(customKey, member.getRoleDefinition().getKey());
        assertEquals(30, member.getRoleDefinition().getPriority());
    }

    @Test
    @DisplayName("falls back to builtin role definition when custom role missing")
    void fallsBackToBuiltinDefinition() {
        final PlayerModel player = new PlayerModel("00000000-0000-0000-0000-000000000001");
        final FactionTeamMember member = new FactionTeamMember(player, null);
        assertTrue(member.getRoleDefinition().getKey() != null && !member.getRoleDefinition().getKey().isBlank());
    }
}
