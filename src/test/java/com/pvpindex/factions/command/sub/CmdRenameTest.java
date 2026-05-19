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
@DisplayName("CmdRename — /f rename <name>")
class CmdRenameTest extends CommandTestBase {


    @Mock private FactionService factionService;


    private CmdRename cmd;
    private final UUID uuid = UUID.randomUUID();


    @BeforeEach
    void setUp() {
        cmd = new CmdRename(factionService);
        when(player.getUniqueId()).thenReturn(uuid);
    }


    @StorageTest
    @DisplayName("success — faction renamed")
    void testRenameSuccess() {
        when(factionService.isOwner(uuid)).thenReturn(true);
        when(factionService.renameFaction(uuid, "NewName")).thenReturn(true);


        cmd.execute(ctx("NewName"));


        verify(player).sendMessage(argThat(componentContains("NewName")));
    }


    @StorageTest
    @DisplayName("not owner — rejected")
    void testNotOwner() {
        when(factionService.isOwner(uuid)).thenReturn(false);


        cmd.execute(ctx("NewName"));


        verify(player).sendMessage(argThat(componentContains("owner")));
        verify(factionService, never()).renameFaction(any(), any());
    }


    @StorageTest
    @DisplayName("name too short — rejected")
    void testNameTooShort() {
        when(factionService.isOwner(uuid)).thenReturn(true);


        cmd.execute(ctx("AB"));


        verify(player).sendMessage(argThat(componentContains("between 3 and 32")));
        verify(factionService, never()).renameFaction(any(), any());
    }


    @StorageTest
    @DisplayName("name too long — rejected")
    void testNameTooLong() {
        when(factionService.isOwner(uuid)).thenReturn(true);


        cmd.execute(ctx("a".repeat(33)));


        verify(player).sendMessage(argThat(componentContains("between 3 and 32")));
        verify(factionService, never()).renameFaction(any(), any());
    }


    @StorageTest
    @DisplayName("name already taken — error message sent")
    void testNameAlreadyTaken() {
        when(factionService.isOwner(uuid)).thenReturn(true);
        when(factionService.renameFaction(uuid, "TakenName")).thenReturn(false);


        cmd.execute(ctx("TakenName"));


        verify(player).sendMessage(argThat(componentContains("Could not rename")));
    }
}
