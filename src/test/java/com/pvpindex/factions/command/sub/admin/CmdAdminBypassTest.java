package com.pvpindex.factions.command.sub.admin;


import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.repository.PlayerRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import com.pvpindex.factions.command.StorageTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdAdminBypass — /fa bypass")
class CmdAdminBypassTest extends CommandTestBase {


    @Mock private PlayerRepository playerRepository;
    @Mock private PlayerModel playerModel;


    private CmdAdminBypass cmd;
    private final UUID uuid = UUID.randomUUID();


    @BeforeEach
    void setUp() throws Exception {
        cmd = new CmdAdminBypass();
        when(player.getUniqueId()).thenReturn(uuid);
        when(repos.players()).thenReturn(playerRepository);
        when(playerRepository.findOrCreate(uuid.toString())).thenReturn(playerModel);
    }


    @StorageTest
    @DisplayName("bypass toggled on — enabled message shown")
    void testBypassEnabled() {
        when(playerModel.isOverriding()).thenReturn(false).thenReturn(true);


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("Bypass enabled")));
    }


    @StorageTest
    @DisplayName("bypass toggled off — disabled message shown")
    void testBypassDisabled() {
        when(playerModel.isOverriding()).thenReturn(true).thenReturn(false);


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("Bypass disabled")));
    }
}
