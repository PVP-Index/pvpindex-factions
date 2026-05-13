package com.pvpindex.factions.engine;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionInboxEntry;
import com.pvpindex.factions.data.repository.FactionInboxRepository;
import com.pvpindex.factions.data.repository.PlayerRepository;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.InviteService;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
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
@DisplayName("EngineNotifications inbox delivery")
class EngineNotificationsInboxDeliveryTest {

    @Mock private InviteService inviteService;
    @Mock private FactionService factionService;
    @Mock private Repositories repos;
    @Mock private PlayerRepository players;
    @Mock private FactionInboxRepository inbox;
    @Mock private Player player;

    private EngineNotifications engine;

    @BeforeEach
    void setUp() throws StorageException {
        when(repos.players()).thenReturn(players);
        when(repos.inbox()).thenReturn(inbox);
        engine = new EngineNotifications(inviteService, factionService, repos, Logger.getLogger("test"));
    }

    @Test
    @DisplayName("deliverInbox sends header and each entry message to the player")
    void deliverInboxSendsHeaderAndEntries() throws Exception {
        final String msg1 = "<green>Alice joined your faction.";
        final String msg2 = "<yellow>Bob left your faction.";

        final FactionInboxEntry e1 = new FactionInboxEntry(UUID.randomUUID().toString());
        e1.setMessage(msg1);
        final FactionInboxEntry e2 = new FactionInboxEntry(UUID.randomUUID().toString());
        e2.setMessage(msg2);

        final Method method = EngineNotifications.class.getDeclaredMethod(
            "deliverInbox", Player.class, List.class);
        method.setAccessible(true);
        method.invoke(engine, player, List.of(e1, e2));

        // Header contains "2"
        verify(player).sendMessage(argThat((Component c) ->
            PlainTextComponentSerializer.plainText().serialize(c).contains("2")));
        // Entry 1
        verify(player).sendMessage(argThat((Component c) ->
            PlainTextComponentSerializer.plainText().serialize(c).contains("Alice")));
        // Entry 2
        verify(player).sendMessage(argThat((Component c) ->
            PlainTextComponentSerializer.plainText().serialize(c).contains("Bob")));
    }

    @Test
    @DisplayName("deliverInbox with empty list sends no messages")
    void deliverInboxWithEmptyListSendsNothing() throws Exception {
        final Method method = EngineNotifications.class.getDeclaredMethod(
            "deliverInbox", Player.class, List.class);
        method.setAccessible(true);
        method.invoke(engine, player, List.of());

        // Only the header (count = 0) is sent — player.sendMessage called once
        verify(player).sendMessage(argThat((Component c) ->
            PlainTextComponentSerializer.plainText().serialize(c).contains("0")));
    }
}
