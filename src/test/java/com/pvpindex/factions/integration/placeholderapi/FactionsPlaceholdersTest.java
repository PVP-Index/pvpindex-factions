package com.pvpindex.factions.integration.placeholderapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.model.RankModel;
import com.pvpindex.factions.data.repository.PlayerRepository;
import com.pvpindex.factions.data.repository.RankRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FactionsPlaceholders")
class FactionsPlaceholdersTest {

    @Mock private Repositories repos;
    @Mock private PlayerRepository playerRepository;
    @Mock private RankRepository rankRepository;
    @Mock private OfflinePlayer offlinePlayer;

    @Test
    @DisplayName("resolves role name and prefix placeholders")
    void resolvesRoleAndPrefixPlaceholders() throws Exception {
        final UUID playerUuid = UUID.randomUUID();
        final String rankId = UUID.randomUUID().toString();

        final PlayerModel playerModel = new PlayerModel(playerUuid.toString());
        playerModel.setRankId(rankId);

        final RankModel rankModel = new RankModel(rankId);
        rankModel.setName("Scout");
        rankModel.setPrefix("<gray>[S]</gray>");

        when(offlinePlayer.getUniqueId()).thenReturn(playerUuid);
        when(repos.players()).thenReturn(playerRepository);
        when(repos.ranks()).thenReturn(rankRepository);
        when(playerRepository.find(playerUuid.toString())).thenReturn(Optional.of(playerModel));
        when(rankRepository.find(rankId)).thenReturn(Optional.of(rankModel));

        final FactionsPlaceholders placeholders = new FactionsPlaceholders(repos, Logger.getLogger("test"));
        assertEquals("Scout", placeholders.onRequest(offlinePlayer, "player_role"));
        assertEquals("<gray>[S]</gray>", placeholders.onRequest(offlinePlayer, "player_role_prefix"));
    }
}
