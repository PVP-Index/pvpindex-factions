package com.pvpindex.factions.command.sub;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

    @Test
    @DisplayName("status prints current settings")
    void statusPrintsSettings() {
        cmd.execute(ctx("status"));
        verify(player).sendMessage(argThat(componentContains("Notification settings")));
    }

    @Test
    @DisplayName("invites off persists and confirms")
    void invitesOffPersists() throws Exception {
        cmd.execute(ctx("invites", "off"));
        verify(playerRepository).save(org.mockito.ArgumentMatchers.any(PlayerModel.class));
        verify(player).sendMessage(argThat(componentContains("updated")));
    }
}
