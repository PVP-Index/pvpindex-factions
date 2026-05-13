package com.pvpindex.factions.engine;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionInboxEntry;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.repository.FactionInboxRepository;
import com.pvpindex.factions.data.repository.PlayerRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FactionMemberNotifier.notifyMembers inbox")
class FactionMemberNotifierInboxTest {

    @Mock private Repositories repos;
    @Mock private PlayerRepository players;
    @Mock private FactionInboxRepository inbox;

    private final Logger logger = Logger.getLogger("test");
    private final String factionId = UUID.randomUUID().toString();
    private final UUID onlineUuid = UUID.randomUUID();
    private final UUID offlineUuid = UUID.randomUUID();
    private final String message = "<green>Someone joined your faction.";

    @BeforeEach
    void setUp() throws Exception {
        when(repos.players()).thenReturn(players);
        when(repos.inbox()).thenReturn(inbox);

        final Server mockServer = mock(Server.class);
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
    @DisplayName("online members receive message immediately")
    void onlineMemberReceivesMessageImmediately() throws StorageException {
        final PlayerModel pm = new PlayerModel(onlineUuid.toString());
        pm.setFactionId(factionId);
        when(players.findByFactionId(factionId)).thenReturn(List.of(pm));

        final Player online = mock(Player.class);
        when(online.isOnline()).thenReturn(true);
        when(org.bukkit.Bukkit.getServer().getPlayer(onlineUuid)).thenReturn(online);

        // plugin=null so the notifier sends synchronously without scheduler
        FactionMemberNotifier.notifyMembers(null, repos, logger, factionId, m -> true, message);

        verify(online).sendMessage(argThat((Component c) ->
            PlainTextComponentSerializer.plainText().serialize(c).contains("joined your faction")));
        verify(inbox, never()).save(any());
    }

    @Test
    @DisplayName("offline members get inbox entry saved")
    void offlineMemberGetsInboxEntrySaved() throws StorageException {
        final PlayerModel pm = new PlayerModel(offlineUuid.toString());
        pm.setFactionId(factionId);
        when(players.findByFactionId(factionId)).thenReturn(List.of(pm));
        when(org.bukkit.Bukkit.getServer().getPlayer(offlineUuid)).thenReturn(null);

        FactionMemberNotifier.notifyMembers(null, repos, logger, factionId, m -> true, message);

        final ArgumentCaptor<FactionInboxEntry> captor = ArgumentCaptor.forClass(FactionInboxEntry.class);
        verify(inbox).save(captor.capture());
        final FactionInboxEntry saved = captor.getValue();
        assert saved.getPlayerId().equals(offlineUuid.toString());
        assert saved.getMessage().equals(message);
    }

    @Test
    @DisplayName("filtered-out members are not queued for offline delivery")
    void filteredMemberNotQueued() throws StorageException {
        final PlayerModel pm = new PlayerModel(offlineUuid.toString());
        pm.setFactionId(factionId);
        when(players.findByFactionId(factionId)).thenReturn(List.of(pm));
        when(org.bukkit.Bukkit.getServer().getPlayer(offlineUuid)).thenReturn(null);

        // filter rejects the member
        FactionMemberNotifier.notifyMembers(null, repos, logger, factionId, m -> false, message);

        verify(inbox, never()).save(any());
    }
}
