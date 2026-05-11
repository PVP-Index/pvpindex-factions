package com.pvpindex.factions.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.model.RankModel;
import com.pvpindex.factions.data.repository.FactionRepository;
import com.pvpindex.factions.data.repository.InvitationRepository;
import com.pvpindex.factions.data.repository.PlayerRepository;
import com.pvpindex.factions.data.repository.RankRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FactionServiceImpl join notifications")
class FactionServiceImplJoinNotificationTest {

    @Mock private Plugin plugin;
    @Mock private Repositories repos;
    @Mock private FactionsConfig config;
    @Mock private FactionRepository factions;
    @Mock private RankRepository ranks;
    @Mock private PlayerRepository players;
    @Mock private InvitationRepository invitations;

    private FactionServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        when(repos.factions()).thenReturn(factions);
        when(repos.ranks()).thenReturn(ranks);
        when(repos.players()).thenReturn(players);
        when(repos.invitations()).thenReturn(invitations);
        service = new FactionServiceImpl(plugin, repos, config, Logger.getLogger("test"));

        final Server mockServer = mock(Server.class);
        final PluginManager mockPM = mock(PluginManager.class);
        when(mockServer.getPluginManager()).thenReturn(mockPM);
        final Field serverField = org.bukkit.Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, mockServer);
    }

    @AfterEach
    void tearDown() throws Exception {
        final Field serverField = org.bukkit.Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, null);
    }

    @Test
    @DisplayName("joinFaction notifies online faction members")
    void joinFactionNotifiesOnlineMembers() throws StorageException {
        final String factionId = UUID.randomUUID().toString();
        final UUID joinedUuid = UUID.randomUUID();
        final String joinedId = joinedUuid.toString();
        final UUID memberUuid = UUID.randomUUID();
        final String memberId = memberUuid.toString();
        final String rankId = UUID.randomUUID().toString();

        final FactionModel faction = new FactionModel(factionId);
        final RankModel defaultRank = new RankModel(rankId);
        final PlayerModel joinedPlayer = new PlayerModel(joinedId);
        final PlayerModel member = new PlayerModel(memberId);
        member.setFactionId(factionId);

        final Player onlineMember = mock(Player.class);
        when(onlineMember.isOnline()).thenReturn(true);
        final Player joinedPlayerBukkit = mock(Player.class);
        when(joinedPlayerBukkit.getName()).thenReturn("Joiner");

        when(factions.find(factionId)).thenReturn(Optional.of(faction));
        when(ranks.findDefaultRank(factionId)).thenReturn(Optional.of(defaultRank));
        when(players.findOrCreate(joinedId)).thenReturn(joinedPlayer);
        when(players.findByFactionId(factionId)).thenReturn(List.of(joinedPlayer, member));
        when(org.bukkit.Bukkit.getServer().getPlayer(memberUuid)).thenReturn(onlineMember);
        when(org.bukkit.Bukkit.getServer().getPlayer(joinedUuid)).thenReturn(joinedPlayerBukkit);
        when(org.bukkit.Bukkit.getServer().getOfflinePlayer(joinedUuid)).thenReturn(joinedPlayerBukkit);
        service.joinFaction(factionId, joinedUuid);

        verify(onlineMember).sendMessage(argThat((Component c) ->
            PlainTextComponentSerializer.plainText().serialize(c).contains("Joiner")
                && PlainTextComponentSerializer.plainText().serialize(c).contains("joined your faction")));
    }
}
