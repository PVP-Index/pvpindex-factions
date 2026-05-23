package com.pvpindex.factions.command.sub;


import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import com.pvpindex.factions.command.StorageTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
@DisplayName("CmdNotify - /f notify")
class CmdNotifyTest extends CommandTestBase {


    @Mock private PlayerRepository playerRepository;
    private CmdNotify cmd;


    @BeforeEach
    void setUp() throws Exception {
        cmd = new CmdNotify();
        when(repos.players()).thenReturn(playerRepository);
        final java.util.UUID uuid = java.util.UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        final PlayerModel model = new PlayerModel(uuid.toString());
        when(playerRepository.findOrCreate(org.mockito.ArgumentMatchers.anyString())).thenReturn(model);
    }


    @StorageTest
    @DisplayName("status prints current settings")
    void statusPrintsSettings() {
        cmd.execute(ctx("status"));
        verify(player).sendMessage(argThat(componentContains("Notification settings")));
    }


    @StorageTest
    @DisplayName("invites off persists and confirms")
    void invitesOffPersists() throws Exception {
        cmd.execute(ctx("invites", "off"));
        verify(playerRepository).save(org.mockito.ArgumentMatchers.any(PlayerModel.class));
        verify(player).sendMessage(argThat(componentContains("updated")));
    }


    @StorageTest
    @DisplayName("motd on persists and shows status")
    void motdOnPersists() throws Exception {
        cmd.execute(ctx("motd", "on"));
        verify(playerRepository).save(org.mockito.ArgumentMatchers.any(PlayerModel.class));
        verify(player).sendMessage(argThat(componentContains("updated")));
        // status should now include motd line with description
        verify(player).sendMessage(argThat(componentContains("Notification settings")));
    }


    @StorageTest
    @DisplayName("all off updates all four types")
    void allOffUpdatesAll() throws Exception {
        cmd.execute(ctx("all", "off"));
        verify(playerRepository).save(org.mockito.ArgumentMatchers.any(PlayerModel.class));
        // status should show all four types
        verify(player, org.mockito.Mockito.atLeast(4)).sendMessage(
            argThat(componentContains("off")));
    }


    @StorageTest
    @DisplayName("status shows descriptions in parentheses")
    void statusShowsDescriptions() {
        cmd.execute(ctx("status"));
        verify(player).sendMessage(argThat(componentContains("faction invites on login")));
    }


    @StorageTest
    @DisplayName("unknown type shows error")
    void unknownTypeShowsError() {
        cmd.execute(ctx("bogustype", "on"));
        verify(player).sendMessage(argThat(componentContains("Unknown")));
    }
}
