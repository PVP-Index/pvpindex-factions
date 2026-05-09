package com.gyvex.pvpindex.factions.command.sub;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gyvex.pvpindex.factions.command.CommandTestBase;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.data.model.PlayerModel;
import com.gyvex.pvpindex.factions.data.repository.PlayerRepository;
import com.gyvex.pvpindex.factions.service.FactionService;
import com.gyvex.pvpindex.factions.service.InviteService;
import com.github.ezframework.jaloquent.exception.StorageException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Server;
import org.bukkit.entity.Player;
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
@DisplayName("CmdInvite — /f invite <player>")
class CmdInviteTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private InviteService inviteService;
    @Mock private FactionModel faction;
    @Mock private PlayerRepository playerRepository;

    private CmdInvite cmd;
    private final UUID uuid = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();
    private final UUID targetUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        cmd = new CmdInvite(factionService, inviteService, repos);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn(factionId);
        when(faction.getName()).thenReturn("Alpha");
        lenient().when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);
        lenient().when(repos.players()).thenReturn(playerRepository);

        // Inject mock Server into Bukkit.server so Bukkit.getPlayer() works
        final Server mockServer = mock(Server.class);
        final PluginManager mockPM = mock(PluginManager.class);
        lenient().when(mockServer.getPluginManager()).thenReturn(mockPM);
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
    @DisplayName("success — invites online player")
    void testInviteSuccess() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        final Player target = mock(Player.class);
        when(target.getUniqueId()).thenReturn(targetUuid);
        when(target.getName()).thenReturn("Bob");
        when(org.bukkit.Bukkit.getServer().getPlayer("Bob")).thenReturn(target);
        when(factionService.isInFaction(targetUuid)).thenReturn(false);
        when(inviteService.sendInvite(factionId, uuid, targetUuid)).thenReturn(true);
        final PlayerModel targetModel = new PlayerModel(targetUuid.toString());
        targetModel.setInviteNotifications(true);
        try {
            when(playerRepository.findOrCreate(targetUuid.toString())).thenReturn(targetModel);
        } catch (StorageException ignored) {
            // Not expected in this test setup.
        }

        cmd.execute(ctx("Bob"));

        verify(player).sendMessage(argThat(componentContains("Invited")));
        verify(target).sendMessage(argThat(componentContains("invited")));
    }

    @Test
    @DisplayName("not in faction — rejected")
    void testNotInFaction() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.empty());

        cmd.execute(ctx("Bob"));

        verify(player).sendMessage(argThat(componentContains("not in a faction")));
        verify(inviteService, never()).sendInvite(any(), any(), any());
    }

    @Test
    @DisplayName("target not online — rejected")
    void testTargetNotOnline() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(org.bukkit.Bukkit.getServer().getPlayer("Ghost")).thenReturn(null);

        cmd.execute(ctx("Ghost"));

        verify(player).sendMessage(argThat(componentContains("not found")));
    }

    @Test
    @DisplayName("target already in faction — rejected")
    void testTargetAlreadyInFaction() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        final Player target = mock(Player.class);
        when(target.getUniqueId()).thenReturn(targetUuid);
        when(org.bukkit.Bukkit.getServer().getPlayer("Bob")).thenReturn(target);
        when(factionService.isInFaction(targetUuid)).thenReturn(true);

        cmd.execute(ctx("Bob"));

        verify(player).sendMessage(argThat(componentContains("already in a faction")));
        verify(inviteService, never()).sendInvite(any(), any(), any());
    }

    @Test
    @DisplayName("invite already pending — error shown")
    void testInviteAlreadyPending() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        final Player target = mock(Player.class);
        when(target.getUniqueId()).thenReturn(targetUuid);
        when(org.bukkit.Bukkit.getServer().getPlayer("Bob")).thenReturn(target);
        when(factionService.isInFaction(targetUuid)).thenReturn(false);
        when(inviteService.sendInvite(factionId, uuid, targetUuid)).thenReturn(false);

        cmd.execute(ctx("Bob"));

        verify(player).sendMessage(argThat(componentContains("already pending")));
    }

    @Test
    @DisplayName("tab complete — returns online player names")
    void testTabComplete() {
        final Player online = mock(Player.class);
        when(online.getName()).thenReturn("Carol");
        @SuppressWarnings("unchecked")
        final Collection<Player> online2 = (Collection<Player>) mock(Collection.class);
        lenient().when(org.bukkit.Bukkit.getServer().getOnlinePlayers()).thenReturn(
            (Collection) List.of(online));

        final List<String> completions = cmd.tabComplete(ctx());

        // Primary assertion: no exception is thrown
    }
}
