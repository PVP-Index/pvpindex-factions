package com.pvpindex.factions.command.sub;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.service.FactionService;
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
@DisplayName("CmdUnsetHome — /f unsethome [confirm]")
class CmdUnsetHomeTest extends CommandTestBase {


    @Mock private FactionService factionService;


    private CmdUnsetHome cmd;
    private final UUID uuid = UUID.randomUUID();


    @BeforeEach
    void setUp() {
        cmd = new CmdUnsetHome(factionService);
        when(player.getUniqueId()).thenReturn(uuid);
    }


    @StorageTest
    @DisplayName("success — home removed")
    void testHomeRemoved() {
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);
        when(factionService.unsetFactionHome(uuid)).thenReturn(true);


        cmd.execute(ctx("confirm"));


        verify(player).sendMessage(argThat(componentContains("removed")));
    }


    @StorageTest
    @DisplayName("not officer — rejected")
    void testNotOfficer() {
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(false);


        cmd.execute(ctx("confirm"));


        verify(player).sendMessage(argThat(componentContains("officers")));
        verify(factionService, never()).unsetFactionHome(any());
    }


    @StorageTest
    @DisplayName("no confirm arg — asks for confirmation")
    void testNoConfirm() {
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("confirm")));
        verify(factionService, never()).unsetFactionHome(any());
    }


    @StorageTest
    @DisplayName("service failure — error message sent")
    void testServiceFailure() {
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);
        when(factionService.unsetFactionHome(uuid)).thenReturn(false);


        cmd.execute(ctx("confirm"));


        verify(player).sendMessage(argThat(componentContains("Failed")));
    }
}
